package lk.dmc.disaster.warnings.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lk.dmc.disaster.warnings.entity.HazardStatus;

/**
 * Body of a request to keep a hazard under monitoring or resolve it.
 *
 * @param note an optional comment from the officer. It is accepted but not stored.
 */
public record HazardStatusRequest(@NotNull HazardStatus status, @Size(max = 300) String note) {}
