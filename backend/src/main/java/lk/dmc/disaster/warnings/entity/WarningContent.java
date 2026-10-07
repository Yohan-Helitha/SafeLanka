package lk.dmc.disaster.warnings.entity;

/** The four texts of a warning, trimmed and checked against {@link WarningRules}. */
public record WarningContent(String title, String message, String smsText, String instructions) {

  public WarningContent {
    title =
        WarningRules.requireText("title", title, WarningRules.TITLE_MIN, WarningRules.TITLE_MAX);
    message =
        WarningRules.requireText(
            "message", message, WarningRules.MESSAGE_MIN, WarningRules.MESSAGE_MAX);
    smsText =
        WarningRules.requireText("smsText", smsText, WarningRules.SMS_MIN, WarningRules.SMS_MAX);
    instructions =
        WarningRules.requireText(
            "instructions",
            instructions,
            WarningRules.INSTRUCTIONS_MIN,
            WarningRules.INSTRUCTIONS_MAX);
  }
}
