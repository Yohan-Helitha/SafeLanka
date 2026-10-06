package lk.dmc.disaster.warnings.integration;

import lk.dmc.disaster.warnings.entity.Channel;
import org.springframework.stereotype.Component;

/** Mock audible-alarm gateway. A real one would trigger a siren or an in-app alarm sound. */
@Component
public class AudibleAlertSimulator extends SimulatedChannel {

  public AudibleAlertSimulator(GatewayFailureSwitch failureSwitch) {
    super(Channel.AUDIBLE, failureSwitch);
  }

  @Override
  DeliveryResult deliver(DeliveryRequest request) {
    return DeliveryResult.delivered();
  }
}
