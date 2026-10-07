package lk.dmc.disaster.shared.reference;

import java.util.List;
import java.util.UUID;

/** What other modules may know about a hazard type. */
public record HazardTypeInfo(
    UUID id, String code, String name, boolean active, List<String> categories) {

  public HazardTypeInfo {
    categories = List.copyOf(categories);
  }
}
