package lk.dmc.disaster.warnings.entity;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;

/**
 * Where a warning goes: districts or river basins, never both, and at least one area.
 *
 * @param type which kind of area is targeted
 * @param districtIds districts, used when the type is DISTRICT
 * @param riverBasinIds basins, used when the type is RIVER_BASIN
 */
public record WarningTarget(TargetType type, Set<UUID> districtIds, Set<UUID> riverBasinIds) {

  public WarningTarget {
    districtIds = districtIds == null ? Set.of() : Set.copyOf(districtIds);
    riverBasinIds = riverBasinIds == null ? Set.of() : Set.copyOf(riverBasinIds);
    Set<UUID> chosen = type == TargetType.DISTRICT ? districtIds : riverBasinIds;
    Set<UUID> other = type == TargetType.DISTRICT ? riverBasinIds : districtIds;
    if (chosen.isEmpty() || !other.isEmpty()) {
      throw new AppException(
          ErrorCode.VALIDATION_ERROR,
          "Choose at least one " + type + " area and no areas of the other kind.",
          Map.of("field", type == TargetType.DISTRICT ? "districtIds" : "riverBasinIds"));
    }
  }
}
