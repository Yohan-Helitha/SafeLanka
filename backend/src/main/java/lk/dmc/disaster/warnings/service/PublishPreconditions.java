package lk.dmc.disaster.warnings.service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import lk.dmc.disaster.warnings.entity.Hazard;
import lk.dmc.disaster.warnings.entity.Warning;
import lk.dmc.disaster.warnings.entity.WarningDraft;
import lk.dmc.disaster.warnings.entity.WarningStatus;
import lk.dmc.disaster.warnings.integration.VerifiedReports;
import lk.dmc.disaster.warnings.repository.HazardRepository;
import lk.dmc.disaster.warnings.repository.WarningRepository;
import org.springframework.stereotype.Component;

/**
 * Everything that must be true before a new warning is issued: the hazard exists and is open, the
 * evidence is verified, and no ACTIVE warning of the same hazard already covers the area.
 */
@Component
class PublishPreconditions {

  private final HazardRepository hazards;
  private final WarningRepository warnings;
  private final VerifiedReports verifiedReports;
  private final AudienceService audience;

  PublishPreconditions(
      HazardRepository hazards,
      WarningRepository warnings,
      VerifiedReports verifiedReports,
      AudienceService audience) {
    this.hazards = hazards;
    this.warnings = warnings;
    this.verifiedReports = verifiedReports;
    this.audience = audience;
  }

  /**
   * Checks the draft and returns its hazard.
   *
   * @throws AppException NOT_FOUND for an unknown hazard; BUSINESS_RULE for a resolved hazard or
   *     unverified evidence; CONFLICT, with {@code details.warningId}, for an overlapping warning
   */
  Hazard check(WarningDraft draft, Set<UUID> resolvedDistrictIds) {
    Hazard hazard =
        hazards
            .findById(draft.hazardId())
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Hazard not found."));
    if (!hazard.isOpen()) {
      throw new AppException(ErrorCode.BUSINESS_RULE, "A resolved hazard cannot be warned about.");
    }
    requireVerified(draft.evidenceReportIds());
    requireNoOverlap(hazard, resolvedDistrictIds);
    return hazard;
  }

  private void requireVerified(Set<UUID> reportIds) {
    Set<UUID> unverified = new HashSet<>(reportIds);
    verifiedReports.findVerified(reportIds).forEach(report -> unverified.remove(report.id()));
    if (!unverified.isEmpty()) {
      throw new AppException(
          ErrorCode.BUSINESS_RULE,
          "Only verified reports can be used as evidence.",
          Map.of("unverifiedReportIds", unverified));
    }
  }

  private void requireNoOverlap(Hazard hazard, Set<UUID> resolvedDistrictIds) {
    for (Warning active : warnings.findByHazardIdAndStatus(hazard.getId(), WarningStatus.ACTIVE)) {
      Set<UUID> covered = audience.resolveDistricts(AudienceSelection.of(active.target()));
      if (covered.stream().anyMatch(resolvedDistrictIds::contains)) {
        throw new AppException(
            ErrorCode.CONFLICT,
            "An ACTIVE warning already covers this area. Escalate or edit it instead.",
            Map.of("warningId", active.getId()));
      }
    }
  }
}
