package lk.dmc.disaster.response.validation;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import lk.dmc.disaster.response.dto.request.AssignmentRequest;
import lk.dmc.disaster.response.entity.RescueTeam;
import lk.dmc.disaster.response.entity.RescueTeamStatus;
import lk.dmc.disaster.response.repository.RescueTeamRepository;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class AssignmentValidator {

  private final RescueTeamRepository teams;

  public AssignmentValidator(RescueTeamRepository teams) {
    this.teams = teams;
  }

  public void validateCreation(AssignmentRequest request) {
    if (request == null) {
      throw new AppException(ErrorCode.VALIDATION_ERROR, "Assignment request body is required");
    }

    if (request.eventId() == null) {
      throw new AppException(
          ErrorCode.VALIDATION_ERROR,
          "Disaster event ID is required",
          Map.of("eventId", "Event ID must not be null"));
    }

    if (request.locationText() == null || request.locationText().trim().length() < 3) {
      throw new AppException(
          ErrorCode.VALIDATION_ERROR,
          "Add a valid place name (minimum 3 characters)",
          Map.of("locationText", "Minimum 3 characters"));
    }

    if (request.task() == null
        || request.task().trim().length() < 5
        || request.task().trim().length() > 500) {
      throw new AppException(
          ErrorCode.VALIDATION_ERROR,
          "Task description must be between 5 and 500 characters",
          Map.of("task", "Must be 5-500 characters"));
    }

    if (request.priority() < 1 || request.priority() > 3) {
      throw new AppException(
          ErrorCode.VALIDATION_ERROR,
          "Priority must be between 1 and 3",
          Map.of("priority", "Must be 1, 2, or 3"));
    }

    if (request.peopleEstimated() < 0) {
      throw new AppException(
          ErrorCode.VALIDATION_ERROR,
          "Estimated people count cannot be negative",
          Map.of("peopleEstimated", "Must be 0 or more"));
    }

    if (request.teamId() != null) {
      validateTeamAvailability(request.teamId(), request.districtId());
    }
  }

  public RescueTeam validateTeamAvailability(UUID teamId, UUID districtId) {
    RescueTeam team =
        teams
            .findById(teamId)
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Rescue team not found"));

    if (team.getStatus() != RescueTeamStatus.AVAILABLE) {
      List<Map<String, Object>> alternatives = List.of();
      if (districtId != null) {
        alternatives =
            teams.findByDistrictIdAndStatus(districtId, RescueTeamStatus.AVAILABLE).stream()
                .map(t -> Map.<String, Object>of("id", t.getId().toString(), "name", t.getName()))
                .toList();
      }
      throw new AppException(
          ErrorCode.TEAM_NOT_AVAILABLE,
          "Rescue team '"
              + team.getName()
              + "' is currently "
              + team.getStatus().name()
              + " and not available for assignment",
          Map.of(
              "alternatives",
              alternatives,
              "teamId",
              teamId,
              "currentStatus",
              team.getStatus().name()));
    }

    return team;
  }
}
