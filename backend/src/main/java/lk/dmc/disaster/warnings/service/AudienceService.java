package lk.dmc.disaster.warnings.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.warnings.integration.AreaReference;
import lk.dmc.disaster.warnings.integration.CitizenDirectory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Works out who a warning reaches. A river basin counts the people registered to the basin and
 * everyone living in the basin's districts, each person once.
 */
@Service
public class AudienceService {

  private final CitizenDirectory citizens;
  private final AreaReference areas;

  public AudienceService(CitizenDirectory citizens, AreaReference areas) {
    this.citizens = citizens;
    this.areas = areas;
  }

  /** The audience size and resolved districts, recalculated as the officer changes the pick. */
  @Transactional(readOnly = true)
  public AudiencePreview preview(AudienceSelection selection) {
    Set<UUID> districts = resolveDistricts(selection);
    long count = citizens.countCitizensInAreas(districts, selection.riverBasinIds());
    return new AudiencePreview(count, districts);
  }

  /** The picked districts plus every district of the picked basins. */
  @Transactional(readOnly = true)
  public Set<UUID> resolveDistricts(AudienceSelection selection) {
    Set<UUID> districts = new HashSet<>(selection.districtIds());
    districts.addAll(areas.districtsInBasins(selection.riverBasinIds()));
    return Set.copyOf(districts);
  }

  /** Every person the warning must be sent to. */
  @Transactional(readOnly = true)
  public List<UUID> findCitizenIds(AudienceSelection selection) {
    return citizens.findCitizenIdsInAreas(resolveDistricts(selection), selection.riverBasinIds());
  }
}
