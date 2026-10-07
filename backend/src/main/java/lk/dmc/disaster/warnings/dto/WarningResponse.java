package lk.dmc.disaster.warnings.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.shared.domain.WarningLevel;
import lk.dmc.disaster.warnings.entity.TargetType;
import lk.dmc.disaster.warnings.entity.WarningStatus;

/**
 * A public warning.
 *
 * @param resolvedDistrictIds the districts reached, including those of the targeted basins
 * @param supersedesId the warning this one replaced when it was escalated, or null
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
    UUID supersedesId,
    Instant cancelledAt,
    String cancelReason,
    Set<UUID> evidenceReportIds,
    DeliverySummaryResponse deliverySummary) {}
