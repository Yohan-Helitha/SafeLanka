package lk.dmc.disaster.reports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import lk.dmc.disaster.TestcontainersConfiguration;
import lk.dmc.disaster.auth.application.port.AccessTokenIssuer;
import lk.dmc.disaster.auth.persistence.UserAccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;

/**
 * The whole UC02 flow through the real stack: JWT login rules, role gates, services, database and
 * the published event. It writes two reports and deletes them again afterwards.
 */
@SpringBootTest
@AutoConfigureMockMvc
@RecordApplicationEvents
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ReportLifecycleTest {

  private static final String OFFICER = "00000000-0000-0000-0006-000000000001";
  private static final String DISTRICT_OFFICER = "00000000-0000-0000-0006-000000000002";
  private static final String CITIZEN = "00000000-0000-0000-0006-000000000004";
  private static final String VOLUNTEER = "00000000-0000-0000-0006-000000000005";
  private static final String FLOOD = "00000000-0000-0000-0003-000000000001";
  private static final String COLOMBO = "00000000-0000-0000-0001-000000000001";
  private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2, 3, 4};

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired UserAccountRepository accounts;
  @Autowired AccessTokenIssuer tokens;
  @Autowired VerifiedReportQuery verifiedReports;
  @Autowired ApplicationEvents events;

  private final List<UUID> createdClientRefs = new ArrayList<>();

  @AfterEach
  void deleteWhatTheTestCreated() {
    createdClientRefs.forEach(
        ref -> jdbc.update("delete from hazard_reports where client_ref = ?", ref));
  }

  private String bearer(String userId) {
    var account = accounts.findById(UUID.fromString(userId)).orElseThrow();
    return "Bearer " + tokens.issue(account).value();
  }

  private <B extends AbstractMockHttpServletRequestBuilder<B>> B as(String userId, B request) {
    return request.header("Authorization", bearer(userId));
  }

  private String reportJson(UUID clientRef, boolean withGps) {
    createdClientRefs.add(clientRef);
    String location =
        withGps
            ? "\"latitude\":6.9391,\"longitude\":79.8921,"
            : "\"manualLocationText\":\"Next to the old railway bridge\",";
    return """
        {"clientRef":"%s","hazardTypeId":"%s","category":"RISING_WATER",
         "description":"Lifecycle test: water over the road near the canal bridge",
         %s"districtId":"%s","capturedAt":"%s"}
        """
        .formatted(clientRef, FLOOD, location, COLOMBO, Instant.now().minusSeconds(60));
  }

  private MvcResult submit(String userId, String json, boolean withPhoto, int expectedStatus)
      throws Exception {
    var request =
        multipart("/api/reports")
            .file(new MockMultipartFile("report", "", MediaType.APPLICATION_JSON_VALUE, json.getBytes()));
    if (withPhoto) {
      request.file(new MockMultipartFile("photo", "p.jpg", "image/jpeg", JPEG));
    }
    return mvc.perform(as(userId, request))
        .andExpect(status().is(expectedStatus))
        .andReturn();
  }

  private static String idOf(MvcResult result) throws Exception {
    return JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
  }

  @Test
  void citizenSubmitsOfficerVerifiesAndTheEventIsPublished() throws Exception {
    UUID clientRef = UUID.randomUUID();
    String json = reportJson(clientRef, true);

    // submit, then the phone retries after a lost response
    MvcResult created = submit(CITIZEN, json, true, 201);
    String id = idOf(created);
    assertThat(JsonPath.<String>read(created.getResponse().getContentAsString(), "$.data.referenceNo"))
        .matches("RPT-\\d{4}-\\d{4,}");
    assertThat(idOf(submit(CITIZEN, json, true, 200))).isEqualTo(id);
    mvc.perform(as(VOLUNTEER, multipart("/api/reports")
            .file(new MockMultipartFile("report", "", MediaType.APPLICATION_JSON_VALUE, json.getBytes()))))
        .andExpect(status().isConflict());

    // the reporter sees it; others do not
    mvc.perform(as(CITIZEN, get("/api/reports/mine")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[?(@.id=='" + id + "')].status").value("PENDING"))
        .andExpect(jsonPath("$.data[?(@.id=='" + id + "')].hasPhoto").value(true));
    mvc.perform(as(CITIZEN, get("/api/reports/" + id)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.reporter.fullName").value("Ruwan Fernando"));
    mvc.perform(as(VOLUNTEER, get("/api/reports/" + id))).andExpect(status().isForbidden());
    mvc.perform(as(CITIZEN, get("/api/reports/" + id + "/photo")))
        .andExpect(status().isOk())
        .andExpect(content().contentType("image/jpeg"))
        .andExpect(content().bytes(JPEG));
    mvc.perform(as(VOLUNTEER, get("/api/reports/" + id + "/photo"))).andExpect(status().isForbidden());

    // the officer queue
    mvc.perform(as(OFFICER, get("/api/reports?status=PENDING&size=100")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[?(@.id=='" + id + "')]").isNotEmpty())
        .andExpect(jsonPath("$.meta.totalElements").isNumber());
    mvc.perform(as(CITIZEN, get("/api/reports"))).andExpect(status().isForbidden());
    mvc.perform(as(DISTRICT_OFFICER, get("/api/reports"))).andExpect(status().isForbidden());

    // ask for more information, then verify
    mvc.perform(
            as(OFFICER, patch("/api/reports/" + id + "/request-info"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"comment\":\"Which side of the bridge?\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("NEEDS_MORE_INFO"));
    assertThat(events.stream(ReportVerifiedEvent.class)).isEmpty();

    mvc.perform(as(OFFICER, patch("/api/reports/" + id + "/verify")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("VERIFIED"))
        .andExpect(jsonPath("$.data.reviewedBy").value("Nimal Perera"));

    List<ReportVerifiedEvent> published = events.stream(ReportVerifiedEvent.class).toList();
    assertThat(published).hasSize(1);
    assertThat(published.get(0).reportId()).isEqualTo(UUID.fromString(id));
    assertThat(published.get(0).latitude()).isEqualTo(6.9391);

    // decisions are final
    mvc.perform(as(OFFICER, patch("/api/reports/" + id + "/verify")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("INVALID_STATE_TRANSITION"));
    mvc.perform(
            as(OFFICER, patch("/api/reports/" + id + "/reject"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"DUPLICATE\"}"))
        .andExpect(status().isConflict());

    // the warnings module's view of it
    var summary = verifiedReports.findVerifiedById(UUID.fromString(id)).orElseThrow();
    assertThat(summary.photoUrl()).isEqualTo("/api/reports/" + id + "/photo");
    assertThat(verifiedReports.findVerified(VerifiedReportFilter.any()))
        .extracting(VerifiedReportSummary::reportId)
        .contains(UUID.fromString(id));
    mvc.perform(as(CITIZEN, get("/api/reports/mine")))
        .andExpect(jsonPath("$.data[?(@.id=='" + id + "')].status").value("VERIFIED"));
  }

  @Test
  void reportWithoutGpsIsRejectedWithAReasonAndNeverReachesWarnings() throws Exception {
    String id = idOf(submit(CITIZEN, reportJson(UUID.randomUUID(), false), false, 201));

    mvc.perform(
            as(OFFICER, patch("/api/reports/" + id + "/reject"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"OTHER\"}"))
        .andExpect(status().isUnprocessableContent());
    mvc.perform(
            as(OFFICER, patch("/api/reports/" + id + "/reject"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"OTHER\",\"comment\":\"Test photo from the internet\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("REJECTED"))
        .andExpect(jsonPath("$.data.rejectionReason").value("OTHER"))
        .andExpect(jsonPath("$.data.isManualLocation").value(true))
        .andExpect(jsonPath("$.data.latitude").doesNotExist());

    assertThat(events.stream(ReportRejectedEvent.class)).hasSize(1);
    assertThat(events.stream(ReportVerifiedEvent.class)).isEmpty();
    assertThat(verifiedReports.findVerifiedById(UUID.fromString(id))).isEmpty();
  }

  @Test
  void requestsWithoutALoginOrWithTheWrongRoleAreTurnedAway() throws Exception {
    mvc.perform(get("/api/reports/mine")).andExpect(status().isUnauthorized());
    mvc.perform(as(OFFICER, get("/api/reports/mine"))).andExpect(status().isForbidden());
    mvc.perform(as(DISTRICT_OFFICER, get("/api/reports/mine"))).andExpect(status().isForbidden());
  }

  @Test
  void twoSimultaneousSubmissionsOfTheSameClientRefCreateExactlyOneReport() throws Exception {
    UUID clientRef = UUID.randomUUID();
    String json = reportJson(clientRef, true);
    String auth = bearer(CITIZEN);
    CountDownLatch start = new CountDownLatch(1);
    Callable<Integer> request =
        () -> {
          start.await();
          return mvc.perform(
                  multipart("/api/reports")
                      .file(new MockMultipartFile("report", "", MediaType.APPLICATION_JSON_VALUE, json.getBytes()))
                      .file(new MockMultipartFile("photo", "p.jpg", "image/jpeg", JPEG))
                      .header("Authorization", auth))
              .andReturn()
              .getResponse()
              .getStatus();
        };
    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      Future<Integer> first = pool.submit(request);
      Future<Integer> second = pool.submit(request);
      start.countDown();

      List<Integer> statuses =
          List.of(first.get(60, TimeUnit.SECONDS), second.get(60, TimeUnit.SECONDS)).stream()
              .sorted()
              .toList();

      assertThat(statuses).containsExactly(200, 201);
    } finally {
      pool.shutdownNow();
    }
    assertThat(
            jdbc.queryForObject(
                "select count(*) from hazard_reports where client_ref = ?", Integer.class, clientRef))
        .isEqualTo(1);
  }
}
