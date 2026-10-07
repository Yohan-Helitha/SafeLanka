package lk.dmc.disaster.response.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OccupancyUpdateRequest(@NotNull @Min(0) Integer occupancy) {
}
