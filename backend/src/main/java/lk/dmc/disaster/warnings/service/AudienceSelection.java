package lk.dmc.disaster.warnings.service;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import lk.dmc.disaster.warnings.entity.WarningTarget;

/** The districts and river basins an officer has picked. At least one area is required. */
public record AudienceSelection(Set<UUID> districtIds, Set<UUID> riverBasinIds) {

  public AudienceSelection {
    districtIds = districtIds == null ? Set.of() : Set.copyOf(districtIds);
    riverBasinIds = riverBasinIds == null ? Set.of() : Set.copyOf(riverBasinIds);
    if (districtIds.isEmpty() && riverBasinIds.isEmpty()) {
      throw new AppException(
          ErrorCode.VALIDATION_ERROR,
          "Choose at least one district or river basin.",
          Map.of("field", "districtIds"));
    }
  }

  /** The selection a warning was issued for. */
  public static AudienceSelection of(WarningTarget target) {
    return new AudienceSelection(target.districtIds(), target.riverBasinIds());
  }
}
