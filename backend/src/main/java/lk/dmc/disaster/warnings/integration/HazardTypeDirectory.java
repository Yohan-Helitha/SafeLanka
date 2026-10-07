package lk.dmc.disaster.warnings.integration;

import java.util.Map;
import java.util.UUID;

/** The shared hazard types (FLOOD, LANDSLIDE and so on). */
public interface HazardTypeDirectory {

  /** Every hazard type code, keyed by id. */
  Map<UUID, String> codesById();
}
