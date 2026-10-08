package lk.dmc.disaster.response.dto.response;

import java.time.Instant;
import java.util.UUID;
import lk.dmc.disaster.response.entity.AssignmentStatus;
import lk.dmc.disaster.response.entity.RescueAssignment;

public record AssignmentDto(
    UUID id,
    UUID eventId,
    UUID warningId,
    UUID teamId,
    UUID createdBy,
    double latitude,
    double longitude,
    String locationText,
    String task,
    short priority,
    int peopleEstimated,
    UUID destinationShelterId,
    AssignmentStatus status,
    String declineReason,
    Instant assignedAt,
    Instant acknowledgedAt,
    Instant completedAt,
    long version) {

  public static AssignmentDto from(RescueAssignment assignment) {
    return new AssignmentDto(
        assignment.getId(),
        assignment.getEventId(),
        assignment.getWarningId(),
        assignment.getTeamId(),
        assignment.getCreatedBy(),
        assignment.getLatitude(),
        assignment.getLongitude(),
        assignment.getLocationText(),
        assignment.getTask(),
        assignment.getPriority(),
        assignment.getPeopleEstimated(),
        assignment.getDestinationShelterId(),
        assignment.getStatus(),
        assignment.getDeclineReason(),
        assignment.getAssignedAt(),
        assignment.getAcknowledgedAt(),
        assignment.getCompletedAt(),
        assignment.getVersion());
  }
}
