package lk.dmc.disaster.warnings.dto;

import java.util.List;

/** Delivery totals of a warning, overall and per channel. */
public record DeliverySummaryResponse(
    long targeted, long delivered, long failed, List<ChannelOutcomeResponse> byChannel) {}
