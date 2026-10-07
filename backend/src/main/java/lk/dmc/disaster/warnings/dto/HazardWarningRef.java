package lk.dmc.disaster.warnings.dto;

import java.time.Instant;
import java.util.UUID;
import lk.dmc.disaster.shared.domain.WarningLevel;
import lk.dmc.disaster.warnings.entity.WarningStatus;

/** A warning issued for a hazard, as listed on the hazard detail. */
public record HazardWarningRef(
    UUID id, WarningLevel level, WarningStatus status, Instant issuedAt) {}
