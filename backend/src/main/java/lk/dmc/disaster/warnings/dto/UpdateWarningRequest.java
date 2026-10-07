package lk.dmc.disaster.warnings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lk.dmc.disaster.warnings.entity.WarningContent;
import lk.dmc.disaster.warnings.entity.WarningRules;

/** Body of a request to change the texts of an ACTIVE warning. Nothing is sent again. */
public record UpdateWarningRequest(
    @NotBlank @Size(min = WarningRules.TITLE_MIN, max = WarningRules.TITLE_MAX) String title,
    @NotBlank @Size(min = WarningRules.MESSAGE_MIN, max = WarningRules.MESSAGE_MAX) String message,
    @NotBlank @Size(min = WarningRules.SMS_MIN, max = WarningRules.SMS_MAX) String smsText,
    @NotBlank @Size(min = WarningRules.INSTRUCTIONS_MIN, max = WarningRules.INSTRUCTIONS_MAX)
        String instructions) {

  /** The new texts, checked against the warning text limits. */
  public WarningContent toContent() {
    return new WarningContent(title, message, smsText, instructions);
  }
}
