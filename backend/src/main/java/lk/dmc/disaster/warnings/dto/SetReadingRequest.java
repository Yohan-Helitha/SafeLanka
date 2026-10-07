package lk.dmc.disaster.warnings.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Body of a request to set a gauge reading. */
public record SetReadingRequest(@NotNull BigDecimal value) {}
