package lk.dmc.disaster.warnings.dto;

import lk.dmc.disaster.warnings.entity.Channel;

/** Deliveries over one channel that worked and that failed. */
public record ChannelOutcomeResponse(Channel channel, long delivered, long failed) {}
