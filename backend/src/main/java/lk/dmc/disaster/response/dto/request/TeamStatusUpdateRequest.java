package lk.dmc.disaster.response.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import lk.dmc.disaster.response.validation.ValidTeamStatus;

public record TeamStatusUpdateRequest(
    @NotBlank(message = "Status cannot be blank") @ValidTeamStatus String toStatus,
    UUID clientRef,
    java.time.Instant changedAt,
    boolean recordedOffline) {}
