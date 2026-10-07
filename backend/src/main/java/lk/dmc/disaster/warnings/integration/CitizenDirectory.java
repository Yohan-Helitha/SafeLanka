package lk.dmc.disaster.warnings.integration;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The people a warning can reach: citizens and volunteers, never disabled accounts. Owned by the
 * warnings module so it does not wait for, or depend on, another module's internals.
 */
public interface CitizenDirectory {

  /** Ids of everyone living in any of the districts or registered to any of the river basins. */
  List<UUID> findCitizenIdsInAreas(Collection<UUID> districtIds, Collection<UUID> riverBasinIds);

  /** How many people {@link #findCitizenIdsInAreas} would return. */
  long countCitizensInAreas(Collection<UUID> districtIds, Collection<UUID> riverBasinIds);

  /** The home district of each person, keyed by person id. Unknown ids are left out. */
  Map<UUID, UUID> districtsOf(Collection<UUID> citizenIds);
}
