package lk.dmc.disaster.reports.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.reports.entity.HazardReport;
import lk.dmc.disaster.reports.entity.ReportPhoto;
import lk.dmc.disaster.reports.service.DuplicateMatch;
import lk.dmc.disaster.reports.service.PhotoContent;
import lk.dmc.disaster.reports.service.ReportDetailView;
import lk.dmc.disaster.reports.service.ReportWithPhoto;
import lk.dmc.disaster.reports.service.SubmissionResult;
import lk.dmc.disaster.reports.service.SubmitReportCommand;
import lk.dmc.disaster.shared.actor.ActingUser;
import lk.dmc.disaster.shared.reference.UserView;
import lk.dmc.disaster.shared.domain.Role;
import lk.dmc.disaster.shared.error.BusinessRuleException;
import lk.dmc.disaster.shared.error.ConflictException;
import lk.dmc.disaster.shared.error.ForbiddenRoleException;
import lk.dmc.disaster.shared.error.NotFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

class ReportControllerTest extends ReportWebTestSupport {

  private static final String JSON_BODY =
      """
      {"clientRef":"%s","hazardTypeId":"%s","category":"RISING_WATER",
       "description":"Water over the road near Kolonnawa canal bridge",
       "latitude":6.9391,"longitude":79.8921,"isManualLocation":false,
       "districtId":"%s","capturedAt":"2026-10-04T08:10:00Z"}
      """;

  private static String body() {
    return JSON_BODY.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
  }

  private static MockMultipartFile reportPart(String json) {
    return new MockMultipartFile("report", "", MediaType.APPLICATION_JSON_VALUE, json.getBytes());
  }

  private static MockMultipartFile photoPart() {
    return new MockMultipartFile(
        "photo", "p.jpg", "image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1});
  }

  // ---- submit -------------------------------------------------------------------------------

  @Test
  void submit_withPhotoIs201AndPassesTheCommandToTheService() throws Exception {
    HazardReport report = gpsReport();
    when(submission.submit(any(), any())).thenReturn(new SubmissionResult(report, null, true));

    mvc.perform(
            multipart("/api/reports")
                .file(reportPart(body()))
                .file(photoPart())
                .header(HEADER, CITIZEN))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.id").value(report.getId().toString()))
        .andExpect(jsonPath("$.data.referenceNo").value(report.getReferenceNo()))
        .andExpect(jsonPath("$.data.status").value("PENDING"))
        .andExpect(jsonPath("$.data.hasPhoto").value(false))
        .andExpect(jsonPath("$.meta").doesNotExist());

    ArgumentCaptor<SubmitReportCommand> command =
        ArgumentCaptor.forClass(SubmitReportCommand.class);
    verify(submission).submit(eq(UUID.fromString(CITIZEN)), command.capture());
    assertThat(command.getValue().category()).isEqualTo("RISING_WATER");
    assertThat(command.getValue().latitude()).isEqualTo(6.9391);
    assertThat(command.getValue().photo().contentType()).isEqualTo("image/jpeg");
    assertThat(command.getValue().photo().content()).hasSize(4);
  }

  @Test
  void submit_withoutPhotoIs201AndNoUploadIsPassed() throws Exception {
    when(submission.submit(any(), any())).thenReturn(new SubmissionResult(gpsReport(), null, true));

    mvc.perform(multipart("/api/reports").file(reportPart(body())).header(HEADER, VOLUNTEER))
        .andExpect(status().isCreated());

    ArgumentCaptor<SubmitReportCommand> command =
        ArgumentCaptor.forClass(SubmitReportCommand.class);
    verify(submission).submit(any(), command.capture());
    assertThat(command.getValue().photo()).isNull();
  }

  @Test
  void submit_replayOfASyncedReportIs200() throws Exception {
    HazardReport report = gpsReport();
    ReportPhoto photo = ReportPhoto.attach(report.getId(), "reports/a.jpg", "image/jpeg", 9);
    when(submission.submit(any(), any())).thenReturn(new SubmissionResult(report, photo, false));

    mvc.perform(multipart("/api/reports").file(reportPart(body())).header(HEADER, CITIZEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.hasPhoto").value(true));
  }

  @Test
  void submit_missingDescriptionIs400WithTheFieldNamed() throws Exception {
    String json = body().replaceAll("\"description\":\"[^\"]*\",", "");

    mvc.perform(multipart("/api/reports").file(reportPart(json)).header(HEADER, CITIZEN))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.error.details.fields.description").exists());
    verifyNoInteractions(submission);
  }

  @Test
  void submit_shortDescriptionIs400() throws Exception {
    String json = body().replace("Water over the road near Kolonnawa canal bridge", "too short");

    mvc.perform(multipart("/api/reports").file(reportPart(json)).header(HEADER, CITIZEN))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.details.fields.description").exists());
  }

  @Test
  void submit_missingReportPartIs400() throws Exception {
    mvc.perform(multipart("/api/reports").file(photoPart()).header(HEADER, CITIZEN))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
  }

  @Test
  void submit_businessRuleFailureIs422WithTheErrorEnvelope() throws Exception {
    when(submission.submit(any(), any())).thenThrow(new BusinessRuleException("Unknown district."));

    mvc.perform(multipart("/api/reports").file(reportPart(body())).header(HEADER, CITIZEN))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.error.code").value("BUSINESS_RULE"))
        .andExpect(jsonPath("$.error.message").value("Unknown district."));
  }

  @Test
  void submit_clientRefOfAnotherReporterIs409() throws Exception {
    when(submission.submit(any(), any())).thenThrow(new ConflictException("Key already used."));

    mvc.perform(multipart("/api/reports").file(reportPart(body())).header(HEADER, CITIZEN))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("CONFLICT"));
  }

  @Test
  void submit_officerRoleIs403() throws Exception {
    mvc.perform(multipart("/api/reports").file(reportPart(body())).header(HEADER, OFFICER))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("FORBIDDEN_ROLE"));
    verifyNoInteractions(submission);
  }

  @Test
  void submit_withoutAnActingUserIs401() throws Exception {
    mvc.perform(multipart("/api/reports").file(reportPart(body())))
        .andExpect(status().isUnauthorized());
  }

  // ---- mine ---------------------------------------------------------------------------------

  @Test
  void mine_returnsTheListWithPagingMeta() throws Exception {
    HazardReport report = gpsReport();
    when(queries.mine(UUID.fromString(CITIZEN), 0, 20))
        .thenReturn(new PageImpl<>(List.of(new ReportWithPhoto(report, null))));

    mvc.perform(get("/api/reports/mine").header(HEADER, CITIZEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].referenceNo").value(report.getReferenceNo()))
        .andExpect(jsonPath("$.data[0].hasDuplicates").value(false))
        .andExpect(jsonPath("$.meta.totalElements").value(1))
        .andExpect(jsonPath("$.meta.page").value(0));
  }

  @Test
  void mine_passesPagingParameters() throws Exception {
    when(queries.mine(any(), eq(2), eq(5))).thenReturn(new PageImpl<>(List.of()));

    mvc.perform(get("/api/reports/mine?page=2&size=5").header(HEADER, VOLUNTEER))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isEmpty());
  }

  @Test
  void mine_officerRoleIs403() throws Exception {
    mvc.perform(get("/api/reports/mine").header(HEADER, OFFICER)).andExpect(status().isForbidden());
  }

  // ---- detail and photo ---------------------------------------------------------------------

  @Test
  void detail_returnsTheFrontendShapeForAnOfficer() throws Exception {
    HazardReport report = gpsReport();
    HazardReport other = gpsReport();
    ReportDetailView view =
        new ReportDetailView(
            report,
            ReportPhoto.attach(report.getId(), "reports/a.jpg", "image/jpeg", 9),
            new UserView(report.getReporterId(), "Ruwan Fernando", Role.CITIZEN, null, null, null, null),
            List.of(new DuplicateMatch(other, 84.6)),
            null);
    when(queries.detail(eq(report.getId()), any(ActingUser.class))).thenReturn(view);

    mvc.perform(get("/api/reports/" + report.getId()).header(HEADER, OFFICER))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(report.getId().toString()))
        .andExpect(jsonPath("$.data.latitude").value(6.9391))
        .andExpect(jsonPath("$.data.isManualLocation").value(false))
        .andExpect(jsonPath("$.data.photoUrl").value("/api/reports/" + report.getId() + "/photo"))
        .andExpect(jsonPath("$.data.reporter.fullName").value("Ruwan Fernando"))
        .andExpect(jsonPath("$.data.reporter.role").value("CITIZEN"))
        .andExpect(jsonPath("$.data.reviewedBy").isEmpty())
        .andExpect(jsonPath("$.data.hasDuplicates").value(true))
        .andExpect(
            jsonPath("$.data.possibleDuplicates[0].referenceNo").value(other.getReferenceNo()))
        .andExpect(jsonPath("$.data.possibleDuplicates[0].distanceMetres").value(85));
  }

  @Test
  void detail_reportWithoutGpsHasNoCoordinateFields() throws Exception {
    HazardReport manual =
        lk.dmc.disaster.reports.service.ReportFixtures.manual(
            UUID.fromString(CITIZEN), lk.dmc.disaster.reports.service.ReportFixtures.NOW);
    when(queries.detail(eq(manual.getId()), any(ActingUser.class))).thenReturn(detailOf(manual));

    mvc.perform(get("/api/reports/" + manual.getId()).header(HEADER, CITIZEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.latitude").doesNotExist())
        .andExpect(jsonPath("$.data.isManualLocation").value(true))
        .andExpect(jsonPath("$.data.manualLocationText").value("Next to the old railway bridge"))
        .andExpect(jsonPath("$.data.photoUrl").isEmpty());
  }

  @Test
  void detail_someoneElsesReportIs403FromTheService() throws Exception {
    UUID id = UUID.randomUUID();
    when(queries.detail(eq(id), any(ActingUser.class)))
        .thenThrow(new ForbiddenRoleException("You cannot open this report."));

    mvc.perform(get("/api/reports/" + id).header(HEADER, CITIZEN))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("FORBIDDEN_ROLE"));
  }

  @Test
  void detail_unknownReportIs404() throws Exception {
    UUID id = UUID.randomUUID();
    when(queries.detail(eq(id), any(ActingUser.class)))
        .thenThrow(new NotFoundException("Report not found."));

    mvc.perform(get("/api/reports/" + id).header(HEADER, OFFICER))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
  }

  @Test
  void detail_notAUuidIs400() throws Exception {
    mvc.perform(get("/api/reports/not-a-uuid").header(HEADER, OFFICER))
        .andExpect(status().isBadRequest());
  }

  @Test
  void photo_isServedWithItsContentTypeAndPrivateCaching() throws Exception {
    UUID id = UUID.randomUUID();
    when(queries.photo(eq(id), any(ActingUser.class)))
        .thenReturn(new PhotoContent(new byte[] {1, 2, 3}, "image/png"));

    mvc.perform(get("/api/reports/" + id + "/photo").header(HEADER, CITIZEN))
        .andExpect(status().isOk())
        .andExpect(content().contentType("image/png"))
        .andExpect(content().bytes(new byte[] {1, 2, 3}))
        .andExpect(header().string("Cache-Control", "private, max-age=3600"));
  }

  @Test
  void photo_missingPhotoIs404() throws Exception {
    UUID id = UUID.randomUUID();
    when(queries.photo(eq(id), any(ActingUser.class)))
        .thenThrow(new NotFoundException("This report has no photo."));

    mvc.perform(get("/api/reports/" + id + "/photo").header(HEADER, OFFICER))
        .andExpect(status().isNotFound());
  }

  @Test
  void photo_districtOfficerRoleIs403() throws Exception {
    mvc.perform(
            get("/api/reports/" + UUID.randomUUID() + "/photo").header(HEADER, DISTRICT_OFFICER))
        .andExpect(status().isForbidden());
  }
}
