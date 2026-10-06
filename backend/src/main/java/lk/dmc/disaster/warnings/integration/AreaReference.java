package lk.dmc.disaster.warnings.integration;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

/** Geography the warnings module needs: which districts lie in which river basins. */
public interface AreaReference {

  /** Every district that lies in at least one of the basins. */
  Set<UUID> districtsInBasins(Collection<UUID> riverBasinIds);
}
