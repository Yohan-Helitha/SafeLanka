package lk.dmc.disaster.warnings.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lk.dmc.disaster.warnings.entity.WarningRules;

/** Body of a request to cancel an ACTIVE warning. */
public record CancelWarningRequest(
    @NotBlank
        @Size(min = WarningRules.CANCEL_REASON_MIN, max = WarningRules.CANCEL_REASON_MAX)
        @Schema(example = "Kelani water level has fallen below the alert level.")
        String reason) {}
