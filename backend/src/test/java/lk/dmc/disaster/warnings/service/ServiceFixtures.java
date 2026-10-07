package lk.dmc.disaster.warnings.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.shared.domain.WarningLevel;
import lk.dmc.disaster.warnings.entity.Hazard;
import lk.dmc.disaster.warnings.entity.HazardArea;
import lk.dmc.disaster.warnings.entity.Sensor;
import lk.dmc.disaster.warnings.entity.SensorReading;
import lk.dmc.disaster.warnings.entity.TargetType;
import lk.dmc.disaster.warnings.entity.Warning;
import lk.dmc.disaster.warnings.entity.WarningContent;
import lk.dmc.disaster.warnings.entity.WarningDraft;
import lk.dmc.disaster.warnings.entity.WarningTarget;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

/** Shared test data for the service tests. */
final class ServiceFixtures {

  static final Instant NOW = Instant.parse("2026-10-06T10:00:00Z");
  static final UUID OFFICER = UUID.randomUUID();

  private ServiceFixtures() {}

  static WarningContent content() {
    return new WarningContent(
        "Kelani flood", "Water is rising fast.", "Kelani flood: move now.", "Leave homes.");
  }

  static WarningTarget districtTarget(UUID... districts) {
    return new WarningTarget(TargetType.DISTRICT, Set.of(districts), Set.of());
  }

  static WarningTarget basinTarget(UUID... basins) {
    return new WarningTarget(TargetType.RIVER_BASIN, Set.of(), Set.of(basins));
  }

  static Warning warning(WarningLevel level, WarningTarget target, Instant issuedAt) {
    WarningDraft draft =
        new WarningDraft(UUID.randomUUID(), null, level, target, content(), Set.of());
    return Warning.publish(draft, OFFICER, issuedAt);
  }

  static Warning warningForEvent(WarningLevel level, WarningTarget target, UUID eventId) {
    WarningDraft draft =
        new WarningDraft(UUID.randomUUID(), eventId, level, target, content(), Set.of());
    return Warning.publish(draft, OFFICER, NOW);
  }

  static Hazard manualHazard(UUID districtId) {
    return Hazard.manual(
        UUID.randomUUID(),
        3,
        new HazardArea(districtId, null),
        "Kelani river is rising near Hanwella.",
        null,
        NOW);
  }

  static Sensor sensor() {
    Sensor sensor = BeanUtils.instantiateClass(Sensor.class);
    ReflectionTestUtils.setField(sensor, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(sensor, "districtId", UUID.randomUUID());
    ReflectionTestUtils.setField(sensor, "riverBasinId", UUID.randomUUID());
    ReflectionTestUtils.setField(sensor, "alertLevel", new BigDecimal("1.20"));
    ReflectionTestUtils.setField(sensor, "majorFloodLevel", new BigDecimal("2.50"));
    return sensor;
  }

  static SensorReading reading(Sensor sensor, String value, Instant at) {
    return SensorReading.record(sensor.getId(), new BigDecimal(value), at);
  }
}
