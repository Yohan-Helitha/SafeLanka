package lk.dmc.disaster.warnings.dto;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.shared.domain.WarningLevel;
import lk.dmc.disaster.warnings.entity.WarningStatus;

/**
 * A warning issued for a hazard, as listed on the hazard detail.
 *
 * @param levelChangedAt when the level last changed
 * @param levelHistory every level the warning has had, oldest first
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
    Instant levelChangedAt,
    List<LevelChangeResponse> levelHistory,
    long reached) {}
