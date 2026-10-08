package lk.dmc.disaster.response.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record DistributionRequest(
    @NotNull @Min(1) Integer quantityDistributed,
    Instant distributedAt,
    UUID clientRef,
    boolean recordedOffline) {}
