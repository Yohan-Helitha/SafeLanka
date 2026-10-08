package lk.dmc.disaster.response.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AllocationRequest(
    @NotNull UUID stockId,
    @NotNull UUID shelterId,
    @NotNull UUID eventId,
    @NotNull @Min(1) Integer quantity) {}
