package lk.dmc.disaster.warnings.integration;

import lk.dmc.disaster.warnings.entity.Channel;

/**
 * One way of reaching a citizen (Strategy). Dispatch holds a list of these and never needs to know
 * which they are, so a new channel is a new class and nothing else.
 */
public interface NotificationChannel {

  /** The channel this strategy serves. */
  Channel channel();

  /**
   * Sends one message to one citizen. A failure is returned, not thrown, so one bad channel never
   * stops the others.
   */
  DeliveryResult send(DeliveryRequest request);
}
