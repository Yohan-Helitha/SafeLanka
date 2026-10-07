package lk.dmc.disaster.warnings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lk.dmc.disaster.warnings.entity.Sensor;
import lk.dmc.disaster.warnings.entity.SensorReading;
import lk.dmc.disaster.warnings.repository.SensorReadingRepository;
import lk.dmc.disaster.warnings.repository.SensorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GaugeReadingsTest {

  private static final Instant NOW = ServiceFixtures.NOW;

  @Mock private SensorRepository sensors;
  @Mock private SensorReadingRepository readings;

  private GaugeReadings gauges;
  private Sensor sensor;

  @BeforeEach
  void setUp() {
    gauges = new GaugeReadings(sensors, readings);
    sensor = ServiceFixtures.sensor();
  }

  @Test
  void latest_sensorWithReadings_returnsTheNewest() {
    SensorReading newest = ServiceFixtures.reading(sensor, "1.30", NOW);
    when(sensors.findById(sensor.getId())).thenReturn(Optional.of(sensor));
    when(readings.findFirstBySensorIdOrderByRecordedAtDesc(sensor.getId()))
        .thenReturn(Optional.of(newest));

    assertThat(gauges.latest(sensor.getId()))
        .hasValueSatisfying(g -> assertThat(g.reading()).isSameAs(newest));
  }

  @Test
  void latest_sensorWithoutReadingsOrUnknownSensor_isEmpty() {
    when(sensors.findById(sensor.getId())).thenReturn(Optional.of(sensor));
    when(readings.findFirstBySensorIdOrderByRecordedAtDesc(sensor.getId()))
        .thenReturn(Optional.empty());
    UUID unknown = UUID.randomUUID();
    when(sensors.findById(unknown)).thenReturn(Optional.empty());

    assertThat(gauges.latest(sensor.getId())).isEmpty();
    assertThat(gauges.latest(unknown)).isEmpty();
  }

  @Test
  void history_returnsTheGaugeWithItsReadings() {
    SensorReading reading = ServiceFixtures.reading(sensor, "1.10", NOW);
    when(sensors.findById(sensor.getId())).thenReturn(Optional.of(sensor));
    when(readings.findBySensorIdAndRecordedAtAfterOrderByRecordedAtAsc(sensor.getId(), NOW))
        .thenReturn(List.of(reading));

    assertThat(gauges.history(sensor.getId(), NOW))
        .hasValueSatisfying(h -> assertThat(h.readings()).containsExactly(reading));
  }

  @Test
  void history_unknownSensor_isEmpty() {
    UUID unknown = UUID.randomUUID();
    when(sensors.findById(unknown)).thenReturn(Optional.empty());

    assertThat(gauges.history(unknown, NOW)).isEmpty();
  }
}
