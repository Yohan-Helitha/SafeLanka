package lk.dmc.disaster.warnings.service;

import lk.dmc.disaster.warnings.entity.Channel;

/** Deliveries of one warning over one channel that worked and that failed. */
public record ChannelOutcome(Channel channel, long delivered, long failed) {}
