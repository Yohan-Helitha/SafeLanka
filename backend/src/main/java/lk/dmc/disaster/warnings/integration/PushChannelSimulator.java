package lk.dmc.disaster.warnings.integration;

import lk.dmc.disaster.warnings.entity.Channel;
import org.springframework.stereotype.Component;

/** Mock app-push gateway. A real one would call Firebase Cloud Messaging. */
@Component
public class PushChannelSimulator extends SimulatedChannel {

  public PushChannelSimulator(GatewayFailureSwitch failureSwitch) {
    super(Channel.PUSH, failureSwitch);
  }

  @Override
  DeliveryResult deliver(DeliveryRequest request) {
    return DeliveryResult.delivered();
  }
}
