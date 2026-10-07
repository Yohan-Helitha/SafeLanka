package lk.dmc.disaster.warnings.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lk.dmc.disaster.warnings.entity.HazardArea;
import lk.dmc.disaster.warnings.entity.WarningRules;
import lk.dmc.disaster.warnings.service.CreateHazardCommand;

/** Body of a request to record a hazard by hand. A district or a river basin is required. */
public record CreateHazardRequest(
    @NotNull UUID hazardTypeId,
    @Min(WarningRules.SEVERITY_MIN) @Max(WarningRules.SEVERITY_MAX) int severity,
    UUID districtId,
    UUID riverBasinId,
    @NotBlank
        @Size(min = WarningRules.HAZARD_DESCRIPTION_MIN, max = WarningRules.HAZARD_DESCRIPTION_MAX)
        @Schema(example = "Kelani river is rising fast near Hanwella; low-lying homes at risk.")
        String description,
    UUID eventId) {

  /** Turns the request into the command for the assessment service. */
  public CreateHazardCommand toCommand() {
    return new CreateHazardCommand(
        hazardTypeId, severity, new HazardArea(districtId, riverBasinId), description, eventId);
  }
}
