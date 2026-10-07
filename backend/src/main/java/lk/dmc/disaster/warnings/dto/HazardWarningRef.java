package lk.dmc.disaster.warnings.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.shared.domain.WarningLevel;
import lk.dmc.disaster.warnings.entity.WarningStatus;

/**
 * A warning issued for a hazard, as listed on the hazard detail.
 *
 * @param reached how many people the warning was sent to
 */
public record HazardWarningRef(
    UUID id,
    UUID hazardId,
    WarningLevel level,
    WarningStatus status,
    String title,
    Set<UUID> districtIds,
    Set<UUID> riverBasinIds,
    Instant issuedAt,
    long reached) {}
