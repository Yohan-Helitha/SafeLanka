package lk.dmc.disaster.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
import java.util.concurrent.ThreadLocalRandom;
import lk.dmc.disaster.TestcontainersConfiguration;
import lk.dmc.disaster.shared.sms.SmsDeliveryException;
import lk.dmc.disaster.shared.sms.SmsGateway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * The auth flow depends on the SmsGateway interface, so a gateway that fails (or a real one) can
 * stand in without touching auth code.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, SmsFailureTest.FailingGateway.class})
class SmsFailureTest {

  @TestConfiguration
  static class FailingGateway {
    @Bean
    @Primary
    SmsGateway failingSmsGateway() {
      return new SmsGateway() {
        @Override
        public void send(String recipientE164, String message) {
          throw new SmsDeliveryException("gateway down");
        }

        @Override
        public boolean simulated() {
          return false;
        }
      };
    }
  }

  @Autowired MockMvc mvc;

  @Test
  void whenTheSmsCannotBeSentSignupFailsCleanlyAndNoAccountIsLeftBehind() throws Exception {
    String phone = "07" + ThreadLocalRandom.current().nextInt(10_000_000, 99_999_999);
    String nic =
        String.valueOf(ThreadLocalRandom.current().nextLong(100_000_000_000L, 999_999_999_999L));
    String body =
        "{\"fullName\":\"Test Citizen\",\"phone\":\""
            + phone
            + "\",\"nic\":\""
            + nic
            + "\",\"homeAddress\":\"12 Temple Road, Kolonnawa\","
            + "\"districtId\":\"00000000-0000-0000-0001-000000000001\","
            + "\"preferredLanguage\":\"en\",\"password\":\"Sunrise2026\"}";

    MvcResult first =
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(body))
            .andReturn();
    assertThat(first.getResponse().getStatus()).isEqualTo(503);
    assertThat((String) JsonPath.read(first.getResponse().getContentAsString(), "$.error.code"))
        .isEqualTo("SMS_UNAVAILABLE");

    // the failed attempt rolled back, so the same number can try again instead of being "already
    // registered"
    MvcResult second =
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(body))
            .andReturn();
    assertThat(second.getResponse().getStatus()).isEqualTo(503);
  }
}
