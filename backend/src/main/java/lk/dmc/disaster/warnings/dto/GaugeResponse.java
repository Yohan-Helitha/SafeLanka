package lk.dmc.disaster.warnings.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** A gauge with its recent readings, for the hazard chart. */
public record GaugeResponse(
    UUID id,
    String code,
    String name,
    UUID riverBasinId,
    UUID districtId,
    BigDecimal alertLevel,
    BigDecimal majorFloodLevel,
    String unit,
    List<ReadingResponse> readings) {}
