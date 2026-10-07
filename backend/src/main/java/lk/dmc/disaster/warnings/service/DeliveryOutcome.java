package lk.dmc.disaster.warnings.service;

import java.util.List;

/**
 * Delivery results of one warning.
 *
 * @param targeted how many different people it was sent to
 * @param delivered successful deliveries over all channels
 * @param failed failed deliveries over all channels
 */
public record DeliveryOutcome(
    long targeted, long delivered, long failed, List<ChannelOutcome> byChannel) {

  public DeliveryOutcome {
    byChannel = List.copyOf(byChannel);
  }
}
