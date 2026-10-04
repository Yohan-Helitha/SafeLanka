package lk.dmc.disaster.shared.sms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import lk.dmc.disaster.shared.sms.SmsProperties.Provider;
import lk.dmc.disaster.shared.sms.SmsProperties.TextLk;
import org.junit.jupiter.api.Test;

class SmsPropertiesTest {

  private static TextLk textLk(String apiKey) {
    return new TextLk(
        "https://app.text.lk/api/v3/sms/send",
        apiKey,
        Duration.ofSeconds(5),
        Duration.ofSeconds(10));
  }

  @Test
  void realSendingWithoutAKeyFailsAtStartupWithAClearMessage() {
    assertThatThrownBy(() -> new SmsProperties(Provider.TEXTLK, "TextLKDemo", textLk("")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("SMS_TEXTLK_API_KEY");
    assertThatThrownBy(() -> new SmsProperties(Provider.TEXTLK, "TextLKDemo", textLk(null)))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void realSendingNeedsASenderId() {
    assertThatThrownBy(() -> new SmsProperties(Provider.TEXTLK, " ", textLk("key")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("SMS_SENDER_ID");
  }

  @Test
  void theMockNeedsNoKeyAtAll() {
    assertThatCode(() -> new SmsProperties(Provider.MOCK, "TextLKDemo", textLk(null)))
        .doesNotThrowAnyException();
  }

  @Test
  void aValidTextLkSetupIsAccepted() {
    SmsProperties props = new SmsProperties(Provider.TEXTLK, "TextLKDemo", textLk("key"));
    assertThat(props.provider()).isEqualTo(Provider.TEXTLK);
  }
}
