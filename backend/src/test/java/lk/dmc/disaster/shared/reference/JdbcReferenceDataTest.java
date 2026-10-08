package lk.dmc.disaster.shared.reference;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import lk.dmc.disaster.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/** Against the seeded reference data. */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class JdbcReferenceDataTest {

  private static final UUID FLOOD = UUID.fromString("00000000-0000-0000-0003-000000000001");
  private static final UUID DROUGHT = UUID.fromString("00000000-0000-0000-0003-000000000003");
  private static final UUID COLOMBO = UUID.fromString("00000000-0000-0000-0001-000000000001");

  @Autowired ReferenceData referenceData;

  @Test
  void hazardType_returnsCodeActiveFlagAndCategories() {
    HazardTypeInfo flood = referenceData.hazardType(FLOOD).orElseThrow();

    assertThat(flood.code()).isEqualTo("FLOOD");
    assertThat(flood.active()).isTrue();
    assertThat(flood.categories()).containsExactly("RISING_WATER", "BLOCKED_ROAD", "OTHER");
  }

  @Test
  void hazardType_inactiveTypeIsStillReturnedAsInactive() {
    assertThat(referenceData.hazardType(DROUGHT))
        .get()
        .extracting(HazardTypeInfo::active)
        .isEqualTo(false);
  }

  @Test
  void hazardType_unknownIdIsEmpty() {
    assertThat(referenceData.hazardType(UUID.randomUUID())).isEmpty();
  }

  @Test
  void districtName_knownAndUnknown() {
    assertThat(referenceData.districtName(COLOMBO)).contains("Colombo");
    assertThat(referenceData.districtExists(UUID.randomUUID())).isFalse();
  }

  @Test
  void hazardTypeAcceptsCategory_usesTheStoredCategories() {
    assertThat(referenceData.hazardTypeAcceptsCategory(FLOOD, "BLOCKED_ROAD")).isTrue();
    assertThat(referenceData.hazardTypeAcceptsCategory(FLOOD, "LANDSLIDE_CRACK")).isFalse();
  }
}
