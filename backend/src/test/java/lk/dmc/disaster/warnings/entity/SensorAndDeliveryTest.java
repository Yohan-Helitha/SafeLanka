package lk.dmc.disaster.warnings.entity;

import static lk.dmc.disaster.warnings.entity.EntityFixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import org.junit.jupiter.api.Test;

class SensorAndDeliveryTest {

  @Test
  void simulationStep_isAnEighthOfTheGap() {
    Sensor sensor = EntityFixtures.sensor("1.20", "2.80");

    assertThat(sensor.simulationStep()).isEqualByComparingTo("0.20");
  }

  @Test
  void simulationStep_tinyGap_isNeverBelowMinimum() {
    Sensor sensor = EntityFixtures.sensor("1.00", "1.10");

    assertThat(sensor.simulationStep()).isEqualByComparingTo("0.05");
  }

  @Test
  void thresholds_exactlyAtLevel_countAsReached() {
    Sensor sensor = EntityFixtures.sensor("1.20", "2.50");

    assertThat(sensor.isAtOrAboveAlert(new BigDecimal("1.19"))).isFalse();
    assertThat(sensor.isAtOrAboveAlert(new BigDecimal("1.20"))).isTrue();
    assertThat(sensor.hasReachedMajorFlood(new BigDecimal("2.49"))).isFalse();
    assertThat(sensor.hasReachedMajorFlood(new BigDecimal("2.50"))).isTrue();
  }

  @Test
  void sensorReading_recordKeepsValues() {
    UUID sensorId = UUID.randomUUID();

    SensorReading reading = SensorReading.record(sensorId, new BigDecimal("1.74"), NOW);

    assertThat(reading.getSensorId()).isEqualTo(sensorId);
    assertThat(reading.getValue()).isEqualByComparingTo("1.74");
    assertThat(reading.getRecordedAt()).isEqualTo(NOW);
  }

  @Test
  void delivery_queued_thenDelivered() {
    NotificationDelivery delivery =
        NotificationDelivery.queue(UUID.randomUUID(), UUID.randomUUID(), Channel.PUSH, NOW);
    assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.QUEUED);

    delivery.delivered(NOW.plusSeconds(1));

    assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.DELIVERED);
    assertThat(delivery.getDeliveredAt()).isEqualTo(NOW.plusSeconds(1));
    assertThat(delivery.getFailureReason()).isNull();
  }

  @Test
  void delivery_failed_keepsReason() {
    NotificationDelivery delivery =
        NotificationDelivery.queue(UUID.randomUUID(), UUID.randomUUID(), Channel.SMS, NOW);

    delivery.failed("Simulated gateway timeout");

    assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.FAILED);
    assertThat(delivery.getFailureReason()).isEqualTo("Simulated gateway timeout");
    assertThat(delivery.getDeliveredAt()).isNull();
  }

  @Test
  void delivery_failedWithBlankOrHugeReason_isStillStoredWithinLimit() {
    NotificationDelivery blank =
        NotificationDelivery.queue(UUID.randomUUID(), UUID.randomUUID(), Channel.SMS, NOW);
    NotificationDelivery huge =
        NotificationDelivery.queue(UUID.randomUUID(), UUID.randomUUID(), Channel.SMS, NOW);

    blank.failed("  ");
    huge.failed("x".repeat(500));

    assertThat(blank.getFailureReason()).isEqualTo("Unknown failure");
    assertThat(huge.getFailureReason()).hasSize(WarningRules.FAILURE_REASON_MAX);
  }

  @Test
  void delivery_settledTwice_isInvalidTransition() {
    NotificationDelivery delivery =
        NotificationDelivery.queue(UUID.randomUUID(), UUID.randomUUID(), Channel.AUDIBLE, NOW);
    delivery.delivered(NOW);

    assertThatThrownBy(() -> delivery.failed("late"))
        .isInstanceOfSatisfying(
            AppException.class,
            e -> assertThat(e.code()).isEqualTo(ErrorCode.INVALID_STATE_TRANSITION));
    assertThatThrownBy(() -> delivery.delivered(NOW)).isInstanceOf(AppException.class);
  }

  @Test
  void channelSetting_switchesChangeOnlyTheirOwnFlag() {
    ChannelSetting setting = new ChannelSetting();

    setting.switchEnabled(true);
    setting.switchSimulatedFailure(true);
    setting.switchEnabled(false);

    assertThat(setting.isEnabled()).isFalse();
    assertThat(setting.isSimulateFailure()).isTrue();
  }

  @Test
  void targetArea_factoriesSetExactlyOneId() {
    UUID id = UUID.randomUUID();

    WarningTargetArea district = WarningTargetArea.ofDistrict(id);
    WarningTargetArea basin = WarningTargetArea.ofRiverBasin(id);

    assertThat(district.getDistrictId()).isEqualTo(id);
    assertThat(district.getRiverBasinId()).isNull();
    assertThat(basin.getRiverBasinId()).isEqualTo(id);
    assertThat(basin.getDistrictId()).isNull();
  }
}
