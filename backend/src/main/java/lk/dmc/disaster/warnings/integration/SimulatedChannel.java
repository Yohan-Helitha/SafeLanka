package lk.dmc.disaster.warnings.integration;

import lk.dmc.disaster.warnings.entity.Channel;
import lombok.extern.slf4j.Slf4j;

/**
 * Common behaviour of the mock gateways (Adapter stand-ins for FCM, an SMS provider and sirens):
 * fail on demand, otherwise let the subclass apply its own gateway rule.
 */
@Slf4j
abstract class SimulatedChannel implements NotificationChannel {

  static final String TIMEOUT_REASON = "Simulated gateway timeout";

  private final Channel channel;
  private final GatewayFailureSwitch failureSwitch;

  SimulatedChannel(Channel channel, GatewayFailureSwitch failureSwitch) {
    this.channel = channel;
    this.failureSwitch = failureSwitch;
  }

  @Override
  public final Channel channel() {
    return channel;
  }

  @Override
  public final DeliveryResult send(DeliveryRequest request) {
    DeliveryResult result =
        failureSwitch.isFailureSimulated(channel)
            ? DeliveryResult.failed(TIMEOUT_REASON)
            : deliver(request);
    if (result.success()) {
      log.debug("{} delivered warning {} to {}", channel, request.warningId(), request.citizenId());
    } else {
      log.warn(
          "{} failed for warning {}, citizen {}: {}",
          channel,
          request.warningId(),
          request.citizenId(),
          result.failureReason());
    }
    return result;
  }

  /** The gateway's own rule once the failure switch is off. */
  abstract DeliveryResult deliver(DeliveryRequest request);
}
