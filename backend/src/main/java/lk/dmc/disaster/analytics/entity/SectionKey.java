package lk.dmc.disaster.analytics.entity;

import java.util.Arrays;
import java.util.Optional;

/**
 * The four report sections, in the order they appear in a report. The enum name is the key used in
 * {@code unavailableSections}; {@link #apiName()} is the key of the section's data in a response.
 */
public enum SectionKey {
  ALERT_TIMELINE("alertTimeline"),
  CITIZENS_REACHED("citizensReached"),
  SHELTER_OCCUPANCY("shelterOccupancy"),
  RESOURCE_DISTRIBUTION("resourceDistribution");

  private final String apiName;

  SectionKey(String apiName) {
    this.apiName = apiName;
  }

  /** The camelCase key of this section in a report response, for example {@code alertTimeline}. */
  public String apiName() {
    return apiName;
  }

  /** Finds a section by either spelling, so reports saved with the older key still open. */
  public static Optional<SectionKey> fromAnyName(String name) {
    return Arrays.stream(values())
        .filter(k -> k.name().equals(name) || k.apiName.equals(name))
        .findFirst();
  }
}
