package lk.dmc.disaster.shared.sms;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.net.http.HttpClient;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Real SMS through Text.lk's HTTP API v3: {@code POST /api/v3/sms/send} with a bearer token and
 * {@code {recipient, sender_id, type, message}}. Recipients are written without the plus sign
 * ({@code 94771234567}). The API key is never logged.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.integrations.sms", name = "provider", havingValue = "textlk")
class TextLkSmsGateway implements SmsGateway {

  @JsonIgnoreProperties(ignoreUnknown = true)
  record Response(String status, String message) {}

  private final RestClient client;
  private final String senderId;

  @Autowired
  TextLkSmsGateway(SmsProperties properties) {
    this(buildClient(properties.textlk()), properties.senderId());
  }

  /** Visible to tests, which supply a client wired to a fake server. */
  TextLkSmsGateway(RestClient client, String senderId) {
    this.client = client;
    this.senderId = senderId;
  }

  @Override
  public void send(String recipientE164, String message) {
    Map<String, String> body =
        Map.of(
            "recipient",
            recipientE164.replace("+", ""),
            "sender_id",
            senderId,
            "type",
            "plain",
            "message",
            message);
    try {
      Response response =
          client
              .post()
              .contentType(MediaType.APPLICATION_JSON)
              .body(body)
              .retrieve()
              .body(Response.class);
      if (response == null || !"success".equalsIgnoreCase(response.status())) {
        String reason = response == null ? "empty response" : response.message();
        throw new SmsDeliveryException("Text.lk did not accept the message: " + reason);
      }
      log.info("SMS sent through Text.lk to ***{}", last4(recipientE164));
    } catch (RestClientResponseException e) {
      throw new SmsDeliveryException(
          "Text.lk answered HTTP "
              + e.getStatusCode().value()
              + ": "
              + abbreviate(e.getResponseBodyAsString()),
          e);
    } catch (RestClientException e) {
      throw new SmsDeliveryException("Text.lk could not be reached", e);
    }
  }

  @Override
  public boolean simulated() {
    return false;
  }

  private static RestClient buildClient(SmsProperties.TextLk config) {
    HttpClient http = HttpClient.newBuilder().connectTimeout(config.connectTimeout()).build();
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
    factory.setReadTimeout(config.readTimeout());
    return RestClient.builder()
        .baseUrl(config.url())
        .requestFactory(factory)
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.apiKey())
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .build();
  }

  private static String last4(String number) {
    return number.length() <= 4 ? "****" : number.substring(number.length() - 4);
  }

  private static String abbreviate(String text) {
    if (text == null) {
      return "";
    }
    return text.length() <= 200 ? text : text.substring(0, 200) + "...";
  }
}
