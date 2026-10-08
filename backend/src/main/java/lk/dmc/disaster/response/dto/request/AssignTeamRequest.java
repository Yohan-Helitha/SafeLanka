package lk.dmc.disaster.response.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssignTeamRequest(@NotNull(message = "Team ID is required") UUID teamId) {}
