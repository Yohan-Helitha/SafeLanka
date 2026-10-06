package lk.dmc.disaster.warnings.service;

import java.util.UUID;
import lk.dmc.disaster.warnings.entity.WarningDraft;

/**
 * A duty officer's request to issue a warning.
 *
 * @param confirmed true only when the officer passed the review step
 * @param issuedBy the acting officer
 */
public record PublishCommand(WarningDraft draft, boolean confirmed, UUID issuedBy) {}
