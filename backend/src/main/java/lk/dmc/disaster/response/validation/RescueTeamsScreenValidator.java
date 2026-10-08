package lk.dmc.disaster.response.validation;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import lk.dmc.disaster.response.dto.request.AssignTeamRequest;
import lk.dmc.disaster.response.dto.request.CancelAssignmentRequest;
import lk.dmc.disaster.response.entity.AssignmentStatus;
import lk.dmc.disaster.response.entity.RescueAssignment;
import lk.dmc.disaster.response.entity.RescueTeam;
import lk.dmc.disaster.response.entity.RescueTeamStatus;
import lk.dmc.disaster.response.repository.RescueTeamRepository;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import org.springframework.stereotype.Component;

/**
 * Validation logic corresponding to frontend screen: RescueTeamsScreen
 * (frontend/src/screens/response/RescueTeamsScreen.tsx) and modal AssignDialog
 * (frontend/src/components/response/AssignDialog.tsx).
 *
 * <p>Handles: - Team assignment validation: team availability, assignment status readiness. -
 * Assignment cancellation validation: cancellation reason presence and assignment status.
 */
@Component
public class RescueTeamsScreenValidator {

  private final RescueTeamRepository teams;

  public RescueTeamsScreenValidator(RescueTeamRepository teams) {
    this.teams = teams;
  }

  public RescueTeam validateAssignTeam(
      RescueAssignment assignment, AssignTeamRequest request, UUID districtId) {
    if (request == null || request.teamId() == null) {
      throw new AppException(
          ErrorCode.VALIDATION_ERROR,
          "Team ID is required to assign a team",
          Map.of("teamId", "Must not be null"));
    }

    if (assignment.getStatus() == AssignmentStatus.COMPLETED
        || assignment.getStatus() == AssignmentStatus.CANCELLED) {
      throw new AppException(
          ErrorCode.CONFLICT,
          "Cannot assign a team to an assignment with status " + assignment.getStatus().name());
    }

    RescueTeam team =
        teams
            .findById(request.teamId())
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
              team.getId(),
              "currentStatus",
              team.getStatus().name()));
    }

    return team;
  }

  public void validateCancelAssignment(
      RescueAssignment assignment, CancelAssignmentRequest request) {
    if (request == null || request.reason() == null || request.reason().trim().isEmpty()) {
      throw new AppException(
          ErrorCode.VALIDATION_ERROR,
          "Cancellation reason is required",
          Map.of("reason", "Must not be blank"));
    }

    if (assignment.getStatus() == AssignmentStatus.COMPLETED) {
      throw new AppException(
          ErrorCode.CONFLICT, "Cannot cancel an assignment that has already been completed");
    }

    if (assignment.getStatus() == AssignmentStatus.CANCELLED) {
      throw new AppException(ErrorCode.CONFLICT, "Assignment is already cancelled");
    }
  }
}
