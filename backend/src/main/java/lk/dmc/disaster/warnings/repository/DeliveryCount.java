package lk.dmc.disaster.warnings.repository;

import lk.dmc.disaster.warnings.entity.Channel;
import lk.dmc.disaster.warnings.entity.DeliveryStatus;

/** Number of deliveries of one warning for one channel and status. */
public record DeliveryCount(Channel channel, DeliveryStatus status, long count) {}
