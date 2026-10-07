package lk.dmc.disaster.shared.reference;

import java.util.Optional;
import java.util.UUID;

/**
 * Read-only lookups into the shared reference tables, so modules never import each other's entities
 * or repositories.
 */
public interface ReferenceData {

  Optional<HazardTypeInfo> hazardType(UUID id);

  Optional<String> districtName(UUID id);

  default boolean districtExists(UUID id) {
    return districtName(id).isPresent();
  }

  /** True when the hazard type exists and lists the category (activity is checked separately). */
  default boolean hazardTypeAcceptsCategory(UUID hazardTypeId, String category) {
    return hazardType(hazardTypeId).map(t -> t.categories().contains(category)).orElse(false);
  }
}
