package lk.dmc.disaster.response.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RespondRequest(boolean accept, String declineReason) {
}
