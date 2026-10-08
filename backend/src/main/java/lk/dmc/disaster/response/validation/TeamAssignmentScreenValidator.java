package lk.dmc.disaster.response.validation;

import java.util.Map;
import lk.dmc.disaster.response.dto.request.RespondRequest;
import lk.dmc.disaster.response.dto.request.TeamStatusUpdateRequest;
import lk.dmc.disaster.response.entity.AssignmentStatus;
import lk.dmc.disaster.response.entity.RescueAssignment;
import lk.dmc.disaster.response.entity.RescueTeam;
import lk.dmc.disaster.response.entity.RescueTeamStatus;
import lk.dmc.disaster.shared.actor.ActingUser;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import org.springframework.stereotype.Component;

/**
 * Validation logic corresponding to frontend screen: TeamAssignmentScreen
 * (frontend/src/screens/response/TeamAssignmentScreen.tsx).
 *
 * <p>Handles: - Rescue team responding to an assignment (accept / decline). - Rescue team status
 * progression (EN_ROUTE, ACTIVE, AVAILABLE/COMPLETED).
 */
@Component
public class TeamAssignmentScreenValidator {

  public void validateRespond(
      RescueAssignment assignment, RespondRequest request, ActingUser user) {
    if (request == null) {
      throw new AppException(ErrorCode.VALIDATION_ERROR, "Respond request body is required");
    }

    if (user == null
        || user.rescueTeamId() == null
        || !user.rescueTeamId().equals(assignment.getTeamId())) {
      throw new AppException(
          ErrorCode.FORBIDDEN_ROLE, "Not authorized to respond to this assignment");
    }

    if (assignment.getStatus() != AssignmentStatus.PENDING_ACK) {
      throw new AppException(
          ErrorCode.CONFLICT,
          "Assignment is in status "
              + assignment.getStatus().name()
              + " and cannot be acknowledged/declined");
    }

    if (!request.accept()
        && (request.declineReason() == null || request.declineReason().trim().isEmpty())) {
      throw new AppException(
          ErrorCode.VALIDATION_ERROR,
          "Decline reason is required when declining an assignment",
          Map.of("declineReason", "Must provide a reason when declining"));
    }
  }

  public void validateStatusUpdate(
      RescueTeam team, TeamStatusUpdateRequest request, ActingUser user) {
    if (request == null || request.toStatus() == null || request.toStatus().trim().isEmpty()) {
      throw new AppException(
          ErrorCode.VALIDATION_ERROR,
          "Target status is required",
          Map.of("toStatus", "Must not be blank"));
    }

    try {
      RescueTeamStatus.valueOf(request.toStatus());
    } catch (IllegalArgumentException e) {
      throw new AppException(
          ErrorCode.VALIDATION_ERROR,
          "Invalid rescue team status: " + request.toStatus(),
          Map.of(
              "toStatus",
              "Must be one of AVAILABLE, DISPATCHED, EN_ROUTE, ACTIVE, OFFLINE_UNKNOWN"));
    }
  }
}
