package lk.dmc.disaster.shared.sms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** The Text.lk adapter against a fake server: request shape, success, rejection and outages. */
class TextLkSmsGatewayTest {

  private static final String URL = "https://app.text.lk/api/v3/sms/send";

  private MockRestServiceServer server;
  private TextLkSmsGateway gateway;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder =
        RestClient.builder()
            .baseUrl(URL)
            .defaultHeader("Authorization", "Bearer test-token")
            .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE);
    server = MockRestServiceServer.bindTo(builder).build();
    gateway = new TextLkSmsGateway(builder.build(), "TextLKDemo");
  }

  @Test
  void postsTheDocumentedRequestWithBearerTokenAndNoPlusInTheNumber() {
    server
        .expect(requestTo(URL))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer test-token"))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.recipient").value("94771234567"))
        .andExpect(jsonPath("$.sender_id").value("TextLKDemo"))
        .andExpect(jsonPath("$.type").value("plain"))
        .andExpect(jsonPath("$.message").value("Your code is 123456"))
        .andRespond(
            withSuccess(
                "{\"status\":\"success\",\"data\":{\"uid\":\"abc\"}}", MediaType.APPLICATION_JSON));

    assertThatCode(() -> gateway.send("+94771234567", "Your code is 123456"))
        .doesNotThrowAnyException();
    server.verify();
  }

  @Test
  void anErrorStatusInTheBodyIsADeliveryFailureWithTheReason() {
    server
        .expect(requestTo(URL))
        .andRespond(
            withSuccess(
                "{\"status\":\"error\",\"message\":\"Sender ID not approved\"}",
                MediaType.APPLICATION_JSON));

    assertThatThrownBy(() -> gateway.send("+94771234567", "x"))
        .isInstanceOf(SmsDeliveryException.class)
        .hasMessageContaining("Sender ID not approved");
  }

  @Test
  void anHttpErrorIsADeliveryFailureWithTheStatus() {
    server
        .expect(requestTo(URL))
        .andRespond(
            withStatus(HttpStatus.UNAUTHORIZED)
                .body("{\"message\":\"Unauthenticated\"}")
                .contentType(MediaType.APPLICATION_JSON));

    assertThatThrownBy(() -> gateway.send("+94771234567", "x"))
        .isInstanceOf(SmsDeliveryException.class)
        .hasMessageContaining("401");
  }

  @Test
  void aServerOutageIsADeliveryFailure() {
    server.expect(requestTo(URL)).andRespond(withServerError());

    assertThatThrownBy(() -> gateway.send("+94771234567", "x"))
        .isInstanceOf(SmsDeliveryException.class);
  }

  @Test
  void anUnreachableServerIsADeliveryFailure() {
    server
        .expect(requestTo(URL))
        .andRespond(
            request -> {
              throw new IOException("connection refused");
            });

    assertThatThrownBy(() -> gateway.send("+94771234567", "x"))
        .isInstanceOf(SmsDeliveryException.class)
        .hasMessageContaining("could not be reached");
  }

  @Test
  void aRealGatewayIsNotSimulated() {
    assertThat(gateway.simulated()).isFalse();
  }
}
