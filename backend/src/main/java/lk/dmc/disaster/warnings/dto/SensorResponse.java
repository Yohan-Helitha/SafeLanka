package lk.dmc.disaster.warnings.dto;

import java.math.BigDecimal;
import java.util.UUID;
import lk.dmc.disaster.warnings.entity.Sensor;
import lk.dmc.disaster.warnings.service.SensorSnapshot;

/** A simulated gauge with its newest reading ({@code latest} is null before the first reading). */
public record SensorResponse(
    UUID id,
    String code,
    String name,
    UUID riverBasinId,
    UUID districtId,
    BigDecimal alertLevel,
    BigDecimal majorFloodLevel,
    String unit,
    ReadingResponse latest) {

  public static SensorResponse from(SensorSnapshot snapshot) {
    Sensor sensor = snapshot.sensor();
    return new SensorResponse(
        sensor.getId(),
        sensor.getCode(),
        sensor.getName(),
        sensor.getRiverBasinId(),
        sensor.getDistrictId(),
        sensor.getAlertLevel(),
        sensor.getMajorFloodLevel(),
        sensor.getUnit(),
        ReadingResponse.from(snapshot.latestReading()));
  }
}
