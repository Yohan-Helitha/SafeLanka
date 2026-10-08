package lk.dmc.disaster.shared.reference;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The default methods, against an in-memory stub (no database). */
class ReferenceDataTest {

  private static final UUID FLOOD = UUID.randomUUID();
  private static final UUID COLOMBO = UUID.randomUUID();

  private final ReferenceData data =
      new ReferenceData() {
        @Override
        public Optional<HazardTypeInfo> hazardType(UUID id) {
          return FLOOD.equals(id)
              ? Optional.of(
                  new HazardTypeInfo(FLOOD, "FLOOD", "Flood", true, List.of("RISING_WATER")))
              : Optional.empty();
        }

        @Override
        public Optional<String> districtName(UUID id) {
          return COLOMBO.equals(id) ? Optional.of("Colombo") : Optional.empty();
        }
      };

  @Test
  void districtExists_followsDistrictName() {
    assertThat(data.districtExists(COLOMBO)).isTrue();
    assertThat(data.districtExists(UUID.randomUUID())).isFalse();
  }

  @Test
  void hazardTypeAcceptsCategory_listedCategoryOnly() {
    assertThat(data.hazardTypeAcceptsCategory(FLOOD, "RISING_WATER")).isTrue();
    assertThat(data.hazardTypeAcceptsCategory(FLOOD, "LANDSLIDE_CRACK")).isFalse();
  }

  @Test
  void hazardTypeAcceptsCategory_unknownTypeIsFalse() {
    assertThat(data.hazardTypeAcceptsCategory(UUID.randomUUID(), "RISING_WATER")).isFalse();
  }
}
