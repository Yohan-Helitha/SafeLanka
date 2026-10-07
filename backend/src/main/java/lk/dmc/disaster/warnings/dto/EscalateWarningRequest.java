package lk.dmc.disaster.warnings.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lk.dmc.disaster.shared.domain.WarningLevel;
import lk.dmc.disaster.warnings.entity.WarningContent;
import lk.dmc.disaster.warnings.service.EscalateCommand;

/**
 * Body of a request to replace a warning with a higher-level one. Give all four texts to change
 * them, or none to keep the current ones. {@code confirm} must be true.
 */
public record EscalateWarningRequest(
    @NotNull WarningLevel level,
    String title,
    String message,
    String smsText,
    String instructions,
    boolean confirm) {

  public EscalateCommand toCommand(UUID warningId, UUID issuedBy) {
    return new EscalateCommand(warningId, level, newContent(), confirm, issuedBy);
  }

  private WarningContent newContent() {
    boolean noText = title == null && message == null && smsText == null && instructions == null;
    return noText ? null : new WarningContent(title, message, smsText, instructions);
  }
}
