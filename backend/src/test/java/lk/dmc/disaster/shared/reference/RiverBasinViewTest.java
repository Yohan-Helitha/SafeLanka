package lk.dmc.disaster.shared.reference;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RiverBasinViewTest {

  private static District district(UUID id) {
    District d = new District();
    ReflectionTestUtils.setField(d, "id", id);
    return d;
  }

  private static RiverBasin basin(UUID id, Set<District> districts) {
    RiverBasin b = new RiverBasin();
    ReflectionTestUtils.setField(b, "id", id);
    ReflectionTestUtils.setField(b, "code", "KELANI");
    ReflectionTestUtils.setField(b, "name", "Kelani Ganga");
    ReflectionTestUtils.setField(b, "districts", districts);
    return b;
  }

  @Test
  void districtIds_listsEveryDistrictTheBasinCovers() {
    UUID colombo = UUID.fromString("00000000-0000-0000-0001-000000000001");
    UUID gampaha = UUID.fromString("00000000-0000-0000-0001-000000000002");
    UUID basinId = UUID.randomUUID();

    RiverBasinView view = new RiverBasinView(basin(basinId, Set.of(district(gampaha), district(colombo))));

    assertThat(view.id()).isEqualTo(basinId);
    assertThat(view.code()).isEqualTo("KELANI");
    assertThat(view.name()).isEqualTo("Kelani Ganga");
    assertThat(view.districtIds()).containsExactly(colombo, gampaha);
  }

  @Test
  void districtIds_isEmptyNotNullWhenNoDistrictsAreLoaded() {
    assertThat(new RiverBasinView(basin(UUID.randomUUID(), null)).districtIds()).isEmpty();
    assertThat(new RiverBasinView(basin(UUID.randomUUID(), Set.of())).districtIds()).isEmpty();
  }
}
