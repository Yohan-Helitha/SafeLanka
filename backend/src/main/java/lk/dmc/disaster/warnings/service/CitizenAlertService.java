package lk.dmc.disaster.warnings.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.warnings.integration.AreaReference;
import lk.dmc.disaster.warnings.repository.WarningRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The warnings a citizen should see: the ACTIVE ones for their district and river basin. */
@Service
public class CitizenAlertService {

  private final WarningRepository warnings;
  private final AreaReference areas;

  public CitizenAlertService(WarningRepository warnings, AreaReference areas) {
    this.warnings = warnings;
    this.areas = areas;
  }

  /**
   * Active alerts, most serious first. A basin warning reaches a citizen who is registered to the
   * basin or lives in one of its districts.
   *
   * @param riverBasinId the citizen's own basin, or null
   */
  @Transactional(readOnly = true)
  public List<CitizenAlert> alertsFor(UUID districtId, UUID riverBasinId) {
    Set<UUID> basins = new HashSet<>(areas.basinsOfDistrict(districtId));
    if (riverBasinId != null) {
      basins.add(riverBasinId);
    }
    return warnings.findActiveCovering(Set.of(districtId), basins).stream()
        .sorted(WarningOrdering.HIGHEST_LEVEL_FIRST)
        .map(warning -> CitizenAlert.of(warning, districtId, riverBasinId))
        .toList();
  }
}
