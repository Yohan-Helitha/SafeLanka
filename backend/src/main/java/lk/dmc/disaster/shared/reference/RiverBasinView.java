package lk.dmc.disaster.shared.reference;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * View for RiverBasin. {@code districtIds} lists the districts the basin covers; the frontend uses
 * it to work out which districts a river-basin warning reaches.
 */
public record RiverBasinView(UUID id, String code, String name, List<UUID> districtIds) {

  public RiverBasinView(RiverBasin b) {
    this(b.getId(), b.getCode(), b.getName(), districtIdsOf(b));
  }

  private static List<UUID> districtIdsOf(RiverBasin basin) {
    if (basin.getDistricts() == null) {
      return List.of();
    }
    return basin.getDistricts().stream().map(District::getId).sorted(Comparator.naturalOrder()).toList();
  }
}
