package lk.dmc.disaster.warnings.dto;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.shared.domain.WarningLevel;
import lk.dmc.disaster.warnings.entity.TargetType;
import lk.dmc.disaster.warnings.entity.WarningStatus;

/**
 * A public warning.
 *
 * @param resolvedDistrictIds the districts reached, including those of the targeted basins
 * @param issuedAt when the warning was first issued
 * @param levelChangedAt when its level last changed (the issue time until it is escalated)
 * @param levelHistory every level it has had, oldest first
 */
public record WarningResponse(
    UUID id,
    UUID hazardId,
    UUID eventId,
    WarningLevel level,
    WarningStatus status,
    TargetType targetType,
    Set<UUID> districtIds,
    Set<UUID> riverBasinIds,
    Set<UUID> resolvedDistrictIds,
    String title,
    String message,
    String smsText,
    String instructions,
    Instant issuedAt,
    UUID issuedBy,
    Instant levelChangedAt,
    List<LevelChangeResponse> levelHistory,
    Instant cancelledAt,
    String cancelReason,
    Set<UUID> evidenceReportIds,
    DeliverySummaryResponse deliverySummary) {}
