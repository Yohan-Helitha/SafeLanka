package lk.dmc.disaster.warnings.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lk.dmc.disaster.warnings.entity.WarningRules;

/**
 * Body of a request to set how dangerous a hazard is.
 *
 * @param severity 1 (low) to 5 (very dangerous)
 */
public record HazardSeverityRequest(
    @Min(WarningRules.SEVERITY_MIN) @Max(WarningRules.SEVERITY_MAX) int severity) {}
