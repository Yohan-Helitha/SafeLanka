package lk.dmc.disaster.shared.sms;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * SMS settings, bound from {@code app.integrations.sms.*}. Real sending is opt-in: the default
 * provider is the mock, so a forgotten key can never spend message credits.
 */
@ConfigurationProperties(prefix = "app.integrations.sms")
public record SmsProperties(
    @DefaultValue("mock") Provider provider,
    @DefaultValue("TextLKDemo") String senderId,
    @DefaultValue TextLk textlk) {

  public enum Provider {
    MOCK,
    TEXTLK
  }

  /** Text.lk (https://text.lk) HTTP API v3. */
  public record TextLk(
      @DefaultValue("https://app.text.lk/api/v3/sms/send") String url,
      String apiKey,
      @DefaultValue("5s") Duration connectTimeout,
      @DefaultValue("10s") Duration readTimeout) {}

  public SmsProperties {
    if (provider == Provider.TEXTLK) {
      if (textlk.apiKey() == null || textlk.apiKey().isBlank()) {
        throw new IllegalStateException(
            "app.integrations.sms.provider is 'textlk' but no API key is set. "
                + "Set SMS_TEXTLK_API_KEY in backend/.env, or use SMS_PROVIDER=mock.");
      }
      if (senderId == null || senderId.isBlank()) {
        throw new IllegalStateException("SMS_SENDER_ID must not be empty for the textlk provider.");
      }
    }
  }
}
