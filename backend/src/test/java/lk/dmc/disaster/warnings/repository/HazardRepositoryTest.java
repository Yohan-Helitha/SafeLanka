package lk.dmc.disaster.warnings.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.warnings.entity.Hazard;
import lk.dmc.disaster.warnings.entity.HazardArea;
import lk.dmc.disaster.warnings.entity.HazardEvidence;
import lk.dmc.disaster.warnings.entity.HazardStatus;
import lk.dmc.disaster.warnings.entity.Sensor;
import lk.dmc.disaster.warnings.entity.SensorReading;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;

class HazardRepositoryTest extends RepositoryTestSupport {

  @Autowired private HazardEvidenceRepository evidence;
  @Autowired private SensorRepository sensors;
  @Autowired private SensorReadingRepository readings;

  private UUID type;
  private UUID otherType;
  private UUID district;
  private UUID otherDistrict;
  private UUID basin;

  @BeforeEach
  void setUp() {
    List<UUID> typeIds = ids("hazard_types", 2);
    type = typeIds.get(0);
    otherType = typeIds.get(1);
    List<UUID> districtIds = ids("districts", 2);
    district = districtIds.get(0);
    otherDistrict = districtIds.get(1);
    basin = firstId("river_basins");
  }

  @Test
  void findNewestOpenMatching_sameDistrict_returnsTheNewestHazard() {
    savedHazard(type, new HazardArea(district, null), NOW);
    Hazard newest = savedHazard(type, new HazardArea(district, null), NOW.plusSeconds(60));

    assertThat(hazards.findNewestOpenMatching(type, district, Set.of())).contains(newest);
  }

  @Test
  void findNewestOpenMatching_hazardInAContainingBasin_matches() {
    Hazard basinHazard = savedHazard(type, new HazardArea(null, basin), NOW);

    assertThat(hazards.findNewestOpenMatching(type, otherDistrict, Set.of(basin)))
        .contains(basinHazard);
  }

  @Test
  void findNewestOpenMatching_resolvedOtherTypeOrOtherPlace_isIgnored() {
    Hazard resolved = savedHazard(type, new HazardArea(district, null), NOW);
    resolved.assessAs(HazardStatus.RESOLVED);
    hazards.saveAndFlush(resolved);
    savedHazard(otherType, new HazardArea(district, null), NOW);
    savedHazard(type, new HazardArea(otherDistrict, null), NOW);

    assertThat(hazards.findNewestOpenMatching(type, district, Set.of())).isEmpty();
  }

  @Test
  void specifications_filterByStatusTypeAndDistrict() {
    Hazard open = savedHazard(type, new HazardArea(district, null), NOW);
    Hazard monitoring = savedHazard(type, new HazardArea(district, null), NOW);
    monitoring.assessAs(HazardStatus.MONITORING);
    hazards.saveAndFlush(monitoring);
    Hazard elsewhere = savedHazard(otherType, new HazardArea(otherDistrict, null), NOW);

    List<Hazard> result =
        hazards.findAll(
            HazardSpecifications.statusIn(Set.of(HazardStatus.UNDER_ASSESSMENT))
                .and(HazardSpecifications.ofType(type))
                .and(HazardSpecifications.inDistrict(district)),
            Sort.by(Sort.Direction.DESC, "severity", "detectedAt"));

    assertThat(result).containsExactly(open);
    assertThat(
            hazards.findAll(
                HazardSpecifications.statusIn(null).and(HazardSpecifications.ofType(null))))
        .contains(open, monitoring, elsewhere);
  }

  @Test
  void existsBySensorIdAndStatusNot_trueOnlyWhileTheSensorHazardIsOpen() {
    Sensor sensor = sensors.findAll().get(0);
    Hazard hazard =
        hazards.saveAndFlush(Hazard.fromSensor(sensor, type, "Gauge crossed alert level.", NOW));

    assertThat(hazards.existsBySensorIdAndStatusNot(sensor.getId(), HazardStatus.RESOLVED))
        .isTrue();

    hazard.assessAs(HazardStatus.RESOLVED);
    hazards.saveAndFlush(hazard);

    assertThat(hazards.existsBySensorIdAndStatusNot(sensor.getId(), HazardStatus.RESOLVED))
        .isFalse();
  }

  @Test
  void evidence_isLinkedOnce_andCountedPerHazard() {
    Hazard withTwo = savedHazard(type, new HazardArea(district, null), NOW);
    Hazard withNone = savedHazard(type, new HazardArea(district, null), NOW);
    UUID report1 = insertReport();
    UUID report2 = insertReport();
    evidence.save(HazardEvidence.link(withTwo.getId(), report1, NOW));
    evidence.saveAndFlush(HazardEvidence.link(withTwo.getId(), report2, NOW));

    assertThat(evidence.existsByIdReportId(report1)).isTrue();
    assertThat(evidence.existsByIdReportId(UUID.randomUUID())).isFalse();
    assertThat(evidence.findByIdHazardId(withTwo.getId())).hasSize(2);
    assertThat(evidence.countByHazardIds(List.of(withTwo.getId(), withNone.getId())))
        .containsExactly(new HazardEvidenceCount(withTwo.getId(), 2));
  }

  @Test
  void sensorReadings_latestAndSinceAreOrdered() {
    Sensor sensor = sensors.findAll().get(0);
    Instant t0 = NOW;
    readings.save(SensorReading.record(sensor.getId(), new BigDecimal("1.00"), t0));
    readings.save(
        SensorReading.record(sensor.getId(), new BigDecimal("1.30"), t0.plusSeconds(600)));
    readings.saveAndFlush(
        SensorReading.record(sensor.getId(), new BigDecimal("1.60"), t0.plusSeconds(1200)));

    assertThat(readings.findFirstBySensorIdOrderByRecordedAtDesc(sensor.getId()))
        .get()
        .satisfies(r -> assertThat(r.getValue()).isEqualByComparingTo("1.60"));
    assertThat(
            readings.findBySensorIdAndRecordedAtAfterOrderByRecordedAtAsc(
                sensor.getId(), t0.plusSeconds(300)))
        .extracting(r -> r.getValue().toPlainString())
        .containsExactly("1.30", "1.60");
  }
}
