package lk.dmc.disaster.warnings.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.shared.domain.WarningLevel;
import lk.dmc.disaster.warnings.entity.TargetType;
import lk.dmc.disaster.warnings.entity.WarningContent;
import lk.dmc.disaster.warnings.entity.WarningDraft;
import lk.dmc.disaster.warnings.entity.WarningRules;
import lk.dmc.disaster.warnings.entity.WarningTarget;
import lk.dmc.disaster.warnings.service.PublishCommand;

/**
 * Body of a request to issue a warning. {@code confirm} must be true, which the officer sets by
 * passing the review step.
 */
public record PublishWarningRequest(
    @NotNull UUID hazardId,
    UUID eventId,
    @NotNull WarningLevel level,
    @NotNull TargetType targetType,
    Set<UUID> districtIds,
    Set<UUID> riverBasinIds,
    @NotBlank
        @Size(min = WarningRules.TITLE_MIN, max = WarningRules.TITLE_MAX)
        @Schema(example = "Kelani river flood warning")
        String title,
    @NotBlank
        @Size(min = WarningRules.MESSAGE_MIN, max = WarningRules.MESSAGE_MAX)
        @Schema(example = "The Kelani river is above its major flood level at Hanwella.")
        String message,
    @NotBlank
        @Size(min = WarningRules.SMS_MIN, max = WarningRules.SMS_MAX)
        @Schema(example = "DMC: Kelani flood. Colombo, Gampaha, Kegalle move to higher ground now.")
        String smsText,
    @NotBlank
        @Size(min = WarningRules.INSTRUCTIONS_MIN, max = WarningRules.INSTRUCTIONS_MAX)
        @Schema(example = "Leave low-lying homes and go to the nearest safe centre.")
        String instructions,
    Set<UUID> evidenceReportIds,
    boolean confirm) {

  public PublishCommand toCommand(UUID issuedBy) {
    WarningDraft draft =
        new WarningDraft(
            hazardId,
            eventId,
            level,
            new WarningTarget(targetType, districtIds, riverBasinIds),
            new WarningContent(title, message, smsText, instructions),
            evidenceReportIds);
    return new PublishCommand(draft, confirm, issuedBy);
  }
}
