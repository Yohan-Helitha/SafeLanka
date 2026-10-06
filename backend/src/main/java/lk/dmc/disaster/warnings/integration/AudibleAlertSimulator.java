package lk.dmc.disaster.warnings.integration;

import lk.dmc.disaster.shared.domain.WarningLevel;
import lk.dmc.disaster.warnings.entity.Channel;
import lk.dmc.disaster.warnings.entity.WarningRules;
import org.springframework.stereotype.Component;

/**
 * Mock audible-alarm gateway. A real one would trigger a siren or an in-app alarm sound. Only used
 * for the two most serious levels, so a minor advisory never sounds an alarm.
 */
@Component
public class AudibleAlertSimulator extends SimulatedChannel {

  public AudibleAlertSimulator(GatewayFailureSwitch failureSwitch) {
    super(Channel.AUDIBLE, failureSwitch);
  }

  @Override
  public boolean supports(WarningLevel level) {
    return WarningRules.audibleAllowedAt(level);
  }

  @Override
  DeliveryResult deliver(DeliveryRequest request) {
    return DeliveryResult.delivered();
  }
}
