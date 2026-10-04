package lk.dmc.disaster.shared.sms;

import static org.assertj.core.api.Assertions.assertThat;

import lk.dmc.disaster.shared.config.SmsConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** Which gateway the application uses is decided by configuration alone. */
class SmsProviderSelectionTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withUserConfiguration(SmsConfig.class, MockSmsGateway.class, TextLkSmsGateway.class);

  @Test
  void mockIsTheDefaultSoNothingIsSentUnlessAskedFor() {
    runner.run(
        context -> {
          assertThat(context).hasSingleBean(SmsGateway.class);
          assertThat(context.getBean(SmsGateway.class)).isInstanceOf(MockSmsGateway.class);
          assertThat(context.getBean(SmsGateway.class).simulated()).isTrue();
        });
  }

  @Test
  void textLkIsUsedWhenSelected() {
    runner
        .withPropertyValues(
            "app.integrations.sms.provider=textlk",
            "app.integrations.sms.textlk.api-key=test-key",
            "app.integrations.sms.sender-id=TextLKDemo")
        .run(
            context -> {
              assertThat(context).hasSingleBean(SmsGateway.class);
              assertThat(context.getBean(SmsGateway.class)).isInstanceOf(TextLkSmsGateway.class);
              assertThat(context.getBean(SmsGateway.class).simulated()).isFalse();
            });
  }

  @Test
  void selectingTextLkWithoutAKeyStopsTheApplicationFromStarting() {
    runner
        .withPropertyValues("app.integrations.sms.provider=textlk")
        .run(
            context ->
                assertThat(context.getStartupFailure())
                    .rootCause()
                    .hasMessageContaining("SMS_TEXTLK_API_KEY"));
  }
}
