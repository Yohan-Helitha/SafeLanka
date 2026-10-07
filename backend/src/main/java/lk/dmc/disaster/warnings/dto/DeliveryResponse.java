package lk.dmc.disaster.warnings.dto;

import java.time.Instant;
import java.util.UUID;
import lk.dmc.disaster.warnings.entity.Channel;
import lk.dmc.disaster.warnings.entity.DeliveryStatus;

/** One attempt to reach one person over one channel. */
public record DeliveryResponse(
    UUID citizenId,
    UUID districtId,
    Channel channel,
    DeliveryStatus status,
    Instant attemptedAt,
    Instant deliveredAt,
    String failureReason) {}
