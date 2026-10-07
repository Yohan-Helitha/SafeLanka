package lk.dmc.disaster.warnings.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.warnings.ActiveWarningQuery;
import lk.dmc.disaster.warnings.ActiveWarningSummary;
import lk.dmc.disaster.warnings.entity.Warning;
import lk.dmc.disaster.warnings.entity.WarningTarget;
import lk.dmc.disaster.warnings.integration.AreaReference;
import lk.dmc.disaster.warnings.repository.WarningRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** What other modules see of live warnings (the {@link ActiveWarningQuery} contract). */
@Service
class ActiveWarningQueryImpl implements ActiveWarningQuery {

  private final WarningRepository warnings;
  private final AreaReference areas;

  ActiveWarningQueryImpl(WarningRepository warnings, AreaReference areas) {
    this.warnings = warnings;
    this.areas = areas;
  }

  @Override
  @Transactional(readOnly = true)
  public List<ActiveWarningSummary> findActiveForDistrict(UUID districtId) {
    return warnings
        .findActiveCovering(Set.of(districtId), areas.basinsOfDistrict(districtId))
        .stream()
        .sorted(WarningOrdering.HIGHEST_LEVEL_FIRST)
        .map(ActiveWarningQueryImpl::toSummary)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public boolean hasActiveWarning(UUID eventId, UUID districtId) {
    return findActiveForDistrict(districtId).stream()
        .anyMatch(summary -> eventId.equals(summary.eventId()));
  }

  private static ActiveWarningSummary toSummary(Warning warning) {
    WarningTarget target = warning.target();
    return new ActiveWarningSummary(
        warning.getId(),
        warning.getLevel(),
        warning.getTitle(),
        warning.getInstructions(),
        warning.getEventId(),
        warning.getIssuedAt(),
        target.districtIds(),
        target.riverBasinIds());
  }
}
