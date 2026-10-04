package lk.dmc.disaster.shared.actor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
import lk.dmc.disaster.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The per-method rule on its own. The probe lives under {@code /api/auth/**}, which the URL rules
 * leave open, so any rejection here comes from {@link RoleInterceptor}, not from the URL gate.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class RequiresRoleTest {

  @Autowired MockMvc mvc;

  @Test
  void roleMatchPassesAndMismatchIsForbidden() throws Exception {
    String dmc = tokenFor("EMAIL", "nimal.perera@dmc.lk");
    String citizen = tokenFor("PHONE", "0771000004");

    assertThat(status("/api/auth/probe-dmc", dmc)).isEqualTo(200);
    assertThat(status("/api/auth/probe-dmc", citizen)).isEqualTo(403);
  }

  @Test
  void annotatedMethodWithoutAnyoneSignedInIsUnauthenticated() throws Exception {
    assertThat(status("/api/auth/probe-dmc", null)).isEqualTo(401);
  }

  @Test
  void methodsWithoutTheAnnotationAreNotChecked() throws Exception {
    assertThat(status("/api/auth/probe-open", null)).isEqualTo(200);
  }

  private int status(String url, String token) throws Exception {
    var request = get(url);
    if (token != null) {
      request.header("Authorization", "Bearer " + token);
    }
    return mvc.perform(request).andReturn().getResponse().getStatus();
  }

  private String tokenFor(String type, String identifier) throws Exception {
    String body =
        "{\"identifierType\":\""
            + type
            + "\",\"identifier\":\""
            + identifier
            + "\",\"password\":\"Demo@1234\"}";
    String response =
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(response, "$.data.accessToken");
  }
}
