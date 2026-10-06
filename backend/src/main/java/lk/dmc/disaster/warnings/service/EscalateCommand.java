package lk.dmc.disaster.warnings.service;

import java.util.UUID;
import lk.dmc.disaster.shared.domain.WarningLevel;
import lk.dmc.disaster.warnings.entity.WarningContent;

/**
 * A duty officer's request to replace an ACTIVE warning with a higher-level one.
 *
 * @param newContent new texts, or null to keep the current ones
 * @param confirmed true only when the officer passed the review step
 */
public record EscalateCommand(
    UUID warningId,
    WarningLevel level,
    WarningContent newContent,
    boolean confirmed,
    UUID issuedBy) {}
