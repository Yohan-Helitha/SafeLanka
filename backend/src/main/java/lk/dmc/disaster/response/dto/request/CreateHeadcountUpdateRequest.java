package lk.dmc.disaster.response.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateHeadcountUpdateRequest(
    @NotNull UUID shelterId,
    @NotNull @Min(0) Integer reportedOccupancy,
    String reportedByName,
    String reportedByRole,
    @NotBlank String message) {}

