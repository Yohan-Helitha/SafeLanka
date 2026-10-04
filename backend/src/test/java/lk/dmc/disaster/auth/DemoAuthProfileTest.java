package lk.dmc.disaster.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import lk.dmc.disaster.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** The demo fallback: no login, the role switcher's X-Acting-User header identifies the person. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "demo-auth"})
@Import(TestcontainersConfiguration.class)
class DemoAuthProfileTest {

  private static final String NIMAL_PERERA = "00000000-0000-0000-0006-000000000001";

  @Autowired MockMvc mvc;

  @Test
  void headerActsAsThatUser() throws Exception {
    var result = mvc.perform(get("/api/auth/me").header("X-Acting-User", NIMAL_PERERA)).andReturn();
    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    assertThat(result.getResponse().getContentAsString()).contains("Nimal Perera", "DMC_OFFICER");
  }

  @Test
  void noHeaderMeansNobodyIsSignedIn() throws Exception {
    assertThat(mvc.perform(get("/api/auth/me")).andReturn().getResponse().getStatus())
        .isEqualTo(401);
  }

  @Test
  void aMalformedHeaderIsIgnored() throws Exception {
    var status =
        mvc.perform(get("/api/auth/me").header("X-Acting-User", "not-a-uuid"))
            .andReturn()
            .getResponse()
            .getStatus();
    assertThat(status).isEqualTo(401);
  }
}
