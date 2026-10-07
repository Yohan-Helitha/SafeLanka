package lk.dmc.disaster.warnings.integration;

import lk.dmc.disaster.warnings.entity.Channel;
import lk.dmc.disaster.warnings.entity.WarningRules;
import org.springframework.stereotype.Component;

/** Mock SMS gateway. Like a real one it rejects text longer than a single SMS. */
@Component
public class SmsGatewaySimulator extends SimulatedChannel {

  static final String TOO_LONG_REASON =
      "SMS text longer than " + WarningRules.SMS_MAX + " characters";

  public SmsGatewaySimulator(GatewayFailureSwitch failureSwitch) {
    super(Channel.SMS, failureSwitch);
  }

  @Override
  DeliveryResult deliver(DeliveryRequest request) {
    if (request.smsText().length() > WarningRules.SMS_MAX) {
      return DeliveryResult.failed(TOO_LONG_REASON);
    }
    return DeliveryResult.delivered();
  }
}
