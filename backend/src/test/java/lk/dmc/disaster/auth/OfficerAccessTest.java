package lk.dmc.disaster.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import lk.dmc.disaster.TestcontainersConfiguration;
import lk.dmc.disaster.auth.application.port.AccessTokenIssuer;
import lk.dmc.disaster.auth.persistence.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * What district officers may do, through the real security chain (URL rules and per-method roles).
 * Tokens are minted directly, so nothing is written to the database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class OfficerAccessTest {

  private static final String DMC_OFFICER = "00000000-0000-0000-0006-000000000001";
  private static final String DISTRICT_OFFICER = "00000000-0000-0000-0006-000000000002";
  private static final String CITIZEN = "00000000-0000-0000-0006-000000000004";

  @Autowired MockMvc mvc;
  @Autowired UserAccountRepository accounts;
  @Autowired AccessTokenIssuer tokens;

  private String bearer(String userId) {
    var account = accounts.findById(UUID.fromString(userId)).orElseThrow();
    return "Bearer " + tokens.issue(account).value();
  }

  private int statusOf(String userId, String method, String url) throws Exception {
    var request =
        "POST".equals(method)
            ? post(url).contentType(MediaType.APPLICATION_JSON).content("{}")
            : get(url);
    return mvc.perform(request.header("Authorization", bearer(userId)))
        .andReturn()
        .getResponse()
        .getStatus();
  }

  @Test
  void districtOfficerCanReadAnalytics() throws Exception {
    assertThat(statusOf(DISTRICT_OFFICER, "GET", "/api/analytics/events")).isEqualTo(200);
    // the same list under the path the frontend uses (ad blockers block /analytics/events)
    assertThat(statusOf(DISTRICT_OFFICER, "GET", "/api/analytics/disaster-events"))
        .isEqualTo(200);
    assertThat(statusOf(DISTRICT_OFFICER, "GET", "/api/analytics/reports")).isEqualTo(200);
  }

  @Test
  void onlyTheDmcCanGenerateAnAnalyticsReport() throws Exception {
    assertThat(statusOf(DISTRICT_OFFICER, "POST", "/api/analytics/reports")).isEqualTo(403);
    // the DMC passes the role check; the empty body is then rejected by validation
    assertThat(statusOf(DMC_OFFICER, "POST", "/api/analytics/reports")).isEqualTo(400);
  }

  @Test
  void citizensCannotOpenAnalytics() throws Exception {
    assertThat(statusOf(CITIZEN, "GET", "/api/analytics/events")).isEqualTo(403);
  }

  @Test
  void officersSeeTheActiveAlertBannerLikeEveryoneElse() throws Exception {
    for (String officer : new String[] {DISTRICT_OFFICER, DMC_OFFICER, CITIZEN}) {
      mvc.perform(get("/api/warnings/active/mine").header("Authorization", bearer(officer)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data").isArray());
    }
  }
}
