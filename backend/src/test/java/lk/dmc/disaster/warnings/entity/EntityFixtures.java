package lk.dmc.disaster.warnings.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.shared.domain.WarningLevel;
import org.springframework.test.util.ReflectionTestUtils;

/** Shared test data for the entity tests. */
final class EntityFixtures {

  static final Instant NOW = Instant.parse("2026-10-06T10:00:00Z");
  static final UUID OFFICER = UUID.randomUUID();
  static final UUID HAZARD_ID = UUID.randomUUID();
  static final UUID DISTRICT = UUID.randomUUID();

  private EntityFixtures() {}

  static WarningContent content() {
    return new WarningContent(
        "Kelani river flood",
        "Water is rising fast along the Kelani river.",
        "Kelani flood: move to higher ground now.",
        "Leave low-lying homes.");
  }

  static WarningTarget districts() {
    return new WarningTarget(TargetType.DISTRICT, Set.of(DISTRICT), Set.of());
  }

  static Warning activeWarning(WarningLevel level) {
    WarningDraft draft =
        new WarningDraft(HAZARD_ID, null, level, districts(), content(), Set.of(UUID.randomUUID()));
    return Warning.publish(draft, OFFICER, NOW);
  }

  /** A Sensor as the database would load it; sensors are seeded and have no public factory. */
  static Sensor sensor(String alertLevel, String majorFloodLevel) {
    Sensor sensor = new Sensor();
    ReflectionTestUtils.setField(sensor, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(sensor, "districtId", UUID.randomUUID());
    ReflectionTestUtils.setField(sensor, "riverBasinId", UUID.randomUUID());
    ReflectionTestUtils.setField(sensor, "alertLevel", new BigDecimal(alertLevel));
    ReflectionTestUtils.setField(sensor, "majorFloodLevel", new BigDecimal(majorFloodLevel));
    return sensor;
  }
}
