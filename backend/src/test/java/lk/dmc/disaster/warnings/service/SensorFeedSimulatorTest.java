package lk.dmc.disaster.warnings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import lk.dmc.disaster.warnings.entity.Hazard;
import lk.dmc.disaster.warnings.entity.HazardSource;
import lk.dmc.disaster.warnings.entity.HazardStatus;
import lk.dmc.disaster.warnings.entity.Sensor;
import lk.dmc.disaster.warnings.entity.SensorReading;
import lk.dmc.disaster.warnings.integration.HazardTypeDirectory;
import lk.dmc.disaster.warnings.repository.HazardRepository;
import lk.dmc.disaster.warnings.repository.SensorReadingRepository;
import lk.dmc.disaster.warnings.repository.SensorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Sensor alert level 1.20 m, major flood level 2.50 m, so one tick adds 0.16 m (the gap / 8). */
@ExtendWith(MockitoExtension.class)
class SensorFeedSimulatorTest {

  private static final Instant NOW = ServiceFixtures.NOW;
  private static final UUID FLOOD_TYPE = UUID.randomUUID();

  @Mock private SensorRepository sensors;
  @Mock private SensorReadingRepository readings;
  @Mock private HazardRepository hazards;
  @Mock private HazardTypeDirectory hazardTypes;

  private SensorFeedSimulator simulator;
  private Sensor sensor;

  @BeforeEach
  void setUp() {
    simulator =
        new SensorFeedSimulator(
            sensors, readings, hazards, hazardTypes, Clock.fixed(NOW, ZoneOffset.UTC));
    sensor = ServiceFixtures.sensor();
    lenient().when(sensors.findById(sensor.getId())).thenReturn(Optional.of(sensor));
  }

  private void latestReadingIs(String value) {
    when(readings.findFirstBySensorIdOrderByRecordedAtDesc(sensor.getId()))
        .thenReturn(Optional.of(ServiceFixtures.reading(sensor, value, NOW.minusSeconds(60))));
  }

  private void noReadingsYet() {
    when(readings.findFirstBySensorIdOrderByRecordedAtDesc(sensor.getId()))
        .thenReturn(Optional.empty());
  }

  private void savesAreEchoed() {
    when(readings.save(any(SensorReading.class))).thenAnswer(call -> call.getArgument(0));
  }

  private void hazardsAreEchoedAndFloodTypeExists() {
    when(hazards.save(any(Hazard.class))).thenAnswer(call -> call.getArgument(0));
    when(hazardTypes.codesById())
        .thenReturn(Map.of(FLOOD_TYPE, "FLOOD", UUID.randomUUID(), "LANDSLIDE"));
  }

  private static void assertCode(Throwable thrown, ErrorCode expected) {
    assertThat(thrown)
        .isInstanceOfSatisfying(AppException.class, e -> assertThat(e.code()).isEqualTo(expected));
  }

  // ---- tick --------------------------------------------------------------------------------

  @Test
  void tick_addsOneStepToTheLatestReading() {
    latestReadingIs("0.50");
    savesAreEchoed();

    TickResult result = simulator.tick(sensor.getId());

    assertThat(result.reading().getValue()).isEqualByComparingTo("0.66");
    assertThat(result.reading().getRecordedAt()).isEqualTo(NOW);
    assertThat(result.thresholdCrossed()).isFalse();
    assertThat(result.hazardId()).isNull();
  }

  @Test
  void tick_belowAlertLevel_opensNoHazard() {
    latestReadingIs("0.50");
    savesAreEchoed();

    simulator.tick(sensor.getId());

    verify(hazards, never()).save(any());
  }

  @Test
  void tick_gaugeWithNoReadings_startsFourStepsBelowTheAlertLevel() {
    noReadingsYet();
    savesAreEchoed();

    TickResult result = simulator.tick(sensor.getId());

    assertThat(result.reading().getValue()).isEqualByComparingTo("0.56");
    assertThat(result.thresholdCrossed()).isFalse();
  }

  @Test
  void tick_firstCrossingOfTheAlertLevel_opensOneSensorHazard() {
    latestReadingIs("1.10");
    savesAreEchoed();
    hazardsAreEchoedAndFloodTypeExists();

    TickResult result = simulator.tick(sensor.getId());

    assertThat(result.reading().getValue()).isEqualByComparingTo("1.26");
    assertThat(result.thresholdCrossed()).isTrue();
    assertThat(result.hazardId()).isNotNull();
    verify(hazards).save(any(Hazard.class));
  }

  @Test
  void tick_createdHazard_isASensorHazardUnderAssessmentForTheFloodType() {
    latestReadingIs("1.10");
    savesAreEchoed();
    hazardsAreEchoedAndFloodTypeExists();

    TickResult result = simulator.tick(sensor.getId());

    org.mockito.ArgumentCaptor<Hazard> saved = org.mockito.ArgumentCaptor.forClass(Hazard.class);
    verify(hazards).save(saved.capture());
    Hazard hazard = saved.getValue();
    assertThat(hazard.getId()).isEqualTo(result.hazardId());
    assertThat(hazard.getSource()).isEqualTo(HazardSource.SENSOR);
    assertThat(hazard.getStatus()).isEqualTo(HazardStatus.UNDER_ASSESSMENT);
    assertThat(hazard.getSensorId()).isEqualTo(sensor.getId());
    assertThat(hazard.getHazardTypeId()).isEqualTo(FLOOD_TYPE);
    assertThat(hazard.getDescription()).contains("1.26").contains("1.20");
  }

  @Test
  void tick_secondReadingAboveAlert_doesNotOpenAnotherHazard() {
    latestReadingIs("1.30");
    savesAreEchoed();
    Hazard open = Hazard.fromSensor(sensor, FLOOD_TYPE, "Gauge crossed alert level.", NOW);
    when(hazards.findOpenForSensor(sensor.getId())).thenReturn(Optional.of(open));

    TickResult result = simulator.tick(sensor.getId());

    assertThat(result.thresholdCrossed()).isFalse();
    assertThat(result.hazardId()).isNull();
    verify(hazards, never()).save(any());
  }

  @Test
  void tick_reachingMajorFloodLevel_raisesTheOpenHazardToSeverityFour() {
    latestReadingIs("2.40");
    savesAreEchoed();
    Hazard open = Hazard.fromSensor(sensor, FLOOD_TYPE, "Gauge crossed alert level.", NOW);
    when(hazards.findOpenForSensor(sensor.getId())).thenReturn(Optional.of(open));

    simulator.tick(sensor.getId());

    assertThat(open.getSeverity()).isEqualTo(4);
  }

  // ---- setReading --------------------------------------------------------------------------

  @Test
  void setReading_valueAboveMajorFloodWithNoHazard_opensItAtSeverityFour() {
    noReadingsYet();
    savesAreEchoed();
    hazardsAreEchoedAndFloodTypeExists();

    TickResult result = simulator.setReading(sensor.getId(), new BigDecimal("2.60"));

    assertThat(result.thresholdCrossed()).isTrue();
    org.mockito.ArgumentCaptor<Hazard> saved = org.mockito.ArgumentCaptor.forClass(Hazard.class);
    verify(hazards).save(saved.capture());
    assertThat(saved.getValue().getSeverity()).isEqualTo(4);
  }

  @Test
  void setReading_alreadyAtSeverityFive_isNeverLowered() {
    latestReadingIs("2.00");
    savesAreEchoed();
    Hazard open = Hazard.fromSensor(sensor, FLOOD_TYPE, "Gauge crossed alert level.", NOW);
    open.raiseSeverity(5);
    when(hazards.findOpenForSensor(sensor.getId())).thenReturn(Optional.of(open));

    simulator.setReading(sensor.getId(), new BigDecimal("2.60"));

    assertThat(open.getSeverity()).isEqualTo(5);
  }

  @Test
  void setReading_roundsToTwoDecimals() {
    noReadingsYet();
    savesAreEchoed();

    TickResult result = simulator.setReading(sensor.getId(), new BigDecimal("0.456"));

    assertThat(result.reading().getValue()).isEqualByComparingTo("0.46");
  }

  @Test
  void setReading_exactlyAtAlertLevel_countsAsCrossing() {
    latestReadingIs("1.00");
    savesAreEchoed();
    hazardsAreEchoedAndFloodTypeExists();

    TickResult result = simulator.setReading(sensor.getId(), new BigDecimal("1.20"));

    assertThat(result.thresholdCrossed()).isTrue();
  }

  @Test
  void setReading_nullNegativeOrHugeValue_isValidationError() {
    assertThatThrownBy(() -> simulator.setReading(sensor.getId(), null))
        .satisfies(e -> assertCode(e, ErrorCode.VALIDATION_ERROR));
    assertThatThrownBy(() -> simulator.setReading(sensor.getId(), new BigDecimal("-0.1")))
        .satisfies(e -> assertCode(e, ErrorCode.VALIDATION_ERROR));
    assertThatThrownBy(() -> simulator.setReading(sensor.getId(), new BigDecimal("10000")))
        .satisfies(e -> assertCode(e, ErrorCode.VALIDATION_ERROR));
    verify(readings, never()).save(any());
  }

  @Test
  void floodTypeMissing_isInternalErrorAndNoHazardIsSaved() {
    latestReadingIs("1.10");
    savesAreEchoed();
    when(hazardTypes.codesById()).thenReturn(Map.of(UUID.randomUUID(), "LANDSLIDE"));

    assertThatThrownBy(() -> simulator.tick(sensor.getId()))
        .satisfies(e -> assertCode(e, ErrorCode.INTERNAL_ERROR));
    verify(hazards, never()).save(any());
  }

  // ---- unknown sensor and listing ----------------------------------------------------------

  @Test
  void tickAndSetReading_unknownSensor_areNotFound() {
    UUID unknown = UUID.randomUUID();
    when(sensors.findById(unknown)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> simulator.tick(unknown))
        .satisfies(e -> assertCode(e, ErrorCode.NOT_FOUND));
    assertThatThrownBy(() -> simulator.setReading(unknown, BigDecimal.ONE))
        .satisfies(e -> assertCode(e, ErrorCode.NOT_FOUND));
  }

  @Test
  void listSensors_returnsEachGaugeWithItsLatestReadingOrNull() {
    Sensor second = ServiceFixtures.sensor();
    SensorReading latest = ServiceFixtures.reading(sensor, "1.00", NOW);
    when(sensors.findAll()).thenReturn(List.of(sensor, second));
    when(readings.findFirstBySensorIdOrderByRecordedAtDesc(sensor.getId()))
        .thenReturn(Optional.of(latest));
    when(readings.findFirstBySensorIdOrderByRecordedAtDesc(second.getId()))
        .thenReturn(Optional.empty());

    List<SensorSnapshot> snapshots = simulator.listSensors();

    assertThat(snapshots).extracting(SensorSnapshot::latestReading).containsExactly(latest, null);
  }
}
