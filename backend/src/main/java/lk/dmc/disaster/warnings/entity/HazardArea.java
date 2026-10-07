package lk.dmc.disaster.warnings.entity;

import java.util.Map;
import java.util.UUID;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;

/** Where a hazard is: a district, a river basin, or both. At least one is required. */
public record HazardArea(UUID districtId, UUID riverBasinId) {

  public HazardArea {
    if (districtId == null && riverBasinId == null) {
      throw new AppException(
          ErrorCode.VALIDATION_ERROR,
          "A hazard needs a district or a river basin.",
          Map.of("field", "districtId"));
    }
  }
}
