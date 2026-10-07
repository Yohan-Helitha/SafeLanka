package lk.dmc.disaster.response.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CancelAssignmentRequest(@NotBlank String reason) {
}
