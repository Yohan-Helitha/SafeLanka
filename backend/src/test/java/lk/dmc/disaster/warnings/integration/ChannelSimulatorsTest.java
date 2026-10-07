package lk.dmc.disaster.warnings.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.shared.domain.WarningLevel;
import lk.dmc.disaster.warnings.entity.Channel;
import org.junit.jupiter.api.Test;

class ChannelSimulatorsTest {

  private static DeliveryRequest requestWithSms(String smsText) {
    return new DeliveryRequest(
        UUID.randomUUID(),
        UUID.randomUUID(),
        WarningLevel.WARNING,
        "Flood alert",
        "Water is rising fast.",
        smsText);
  }

  private static GatewayFailureSwitch failing(Channel... channels) {
    Set<Channel> failing = Set.of(channels);
    return failing::contains;
  }

  @Test
  void sms_exactly160Characters_isDelivered() {
    SmsGatewaySimulator sms = new SmsGatewaySimulator(failing());

    DeliveryResult result = sms.send(requestWithSms("x".repeat(160)));

    assertThat(result.success()).isTrue();
    assertThat(result.failureReason()).isNull();
  }

  @Test
  void sms_161Characters_isRejectedByTheGateway() {
    SmsGatewaySimulator sms = new SmsGatewaySimulator(failing());

    DeliveryResult result = sms.send(requestWithSms("x".repeat(161)));

    assertThat(result.success()).isFalse();
    assertThat(result.failureReason()).isEqualTo(SmsGatewaySimulator.TOO_LONG_REASON);
  }

  @Test
  void sms_failureSwitchOn_failsWithTimeout() {
    SmsGatewaySimulator sms = new SmsGatewaySimulator(failing(Channel.SMS));

    DeliveryResult result = sms.send(requestWithSms("short sms text"));

    assertThat(result.success()).isFalse();
    assertThat(result.failureReason()).isEqualTo("Simulated gateway timeout");
  }

  @Test
  void push_andAudible_deliverWhenSwitchIsOff() {
    DeliveryRequest request = requestWithSms("short sms text");

    assertThat(new PushChannelSimulator(failing()).send(request).success()).isTrue();
    assertThat(new AudibleAlertSimulator(failing()).send(request).success()).isTrue();
  }

  @Test
  void push_andAudible_failWhenTheirSwitchIsOn() {
    DeliveryRequest request = requestWithSms("short sms text");

    DeliveryResult push = new PushChannelSimulator(failing(Channel.PUSH)).send(request);
    DeliveryResult audible = new AudibleAlertSimulator(failing(Channel.AUDIBLE)).send(request);

    assertThat(push.success()).isFalse();
    assertThat(audible.failureReason()).isEqualTo("Simulated gateway timeout");
  }

  @Test
  void failureSwitch_affectsOnlyItsOwnChannel() {
    GatewayFailureSwitch onlySmsFails = failing(Channel.SMS);
    DeliveryRequest request = requestWithSms("short sms text");

    assertThat(new PushChannelSimulator(onlySmsFails).send(request).success()).isTrue();
    assertThat(new AudibleAlertSimulator(onlySmsFails).send(request).success()).isTrue();
    assertThat(new SmsGatewaySimulator(onlySmsFails).send(request).success()).isFalse();
  }

  @Test
  void eachSimulator_reportsItsOwnChannel() {
    assertThat(new PushChannelSimulator(failing()).channel()).isEqualTo(Channel.PUSH);
    assertThat(new SmsGatewaySimulator(failing()).channel()).isEqualTo(Channel.SMS);
    assertThat(new AudibleAlertSimulator(failing()).channel()).isEqualTo(Channel.AUDIBLE);
  }

  @Test
  void supports_audibleOnlyFromWarningLevel_otherChannelsAlways() {
    AudibleAlertSimulator audible = new AudibleAlertSimulator(failing());
    PushChannelSimulator push = new PushChannelSimulator(failing());

    assertThat(audible.supports(WarningLevel.WATCH)).isFalse();
    assertThat(audible.supports(WarningLevel.WARNING)).isTrue();
    assertThat(audible.supports(WarningLevel.EVACUATE)).isTrue();
    assertThat(push.supports(WarningLevel.ADVISORY)).isTrue();
  }
}
