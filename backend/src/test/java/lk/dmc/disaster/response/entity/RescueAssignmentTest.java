package lk.dmc.disaster.response.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class RescueAssignmentTest {

  @Test
  void create_initializesWithUnassignedStatusAndDefaults() {
    UUID eventId = UUID.randomUUID();
    UUID warningId = UUID.randomUUID();
    UUID creatorId = UUID.randomUUID();
    UUID shelterId = UUID.randomUUID();

    RescueAssignment assignment =
        RescueAssignment.create(
            eventId,
            warningId,
            creatorId,
            6.9271,
            79.8612,
            "Kelani River bank near Wellampitiya",
            "Evacuate trapped families",
            (short) 1,
            25,
            shelterId);

    assertThat(assignment.getId()).isNotNull();
    assertThat(assignment.getEventId()).isEqualTo(eventId);
    assertThat(assignment.getWarningId()).isEqualTo(warningId);
    assertThat(assignment.getCreatedBy()).isEqualTo(creatorId);
    assertThat(assignment.getLatitude()).isEqualTo(6.9271);
    assertThat(assignment.getLongitude()).isEqualTo(79.8612);
    assertThat(assignment.getLocationText()).isEqualTo("Kelani River bank near Wellampitiya");
    assertThat(assignment.getTask()).isEqualTo("Evacuate trapped families");
    assertThat(assignment.getPriority()).isEqualTo((short) 1);
    assertThat(assignment.getPeopleEstimated()).isEqualTo(25);
    assertThat(assignment.getDestinationShelterId()).isEqualTo(shelterId);
    assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.UNASSIGNED);
    assertThat(assignment.getTeamId()).isNull();
    assertThat(assignment.getVersion()).isEqualTo(0L);
  }

  @Test
  void assignmentLifecycle_stateTransitions() {
    RescueAssignment assignment =
        RescueAssignment.create(
            UUID.randomUUID(),
            null,
            UUID.randomUUID(),
            6.9,
            79.8,
            "Kolonnawa Junction",
            "Provide first aid",
            (short) 2,
            10,
            null);

    UUID teamId = UUID.randomUUID();
    assignment.assignTeam(teamId);
    assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.PENDING_ACK);
    assertThat(assignment.getTeamId()).isEqualTo(teamId);
    assertThat(assignment.getAssignedAt()).isNotNull();

    assignment.acknowledge();
    assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.ACCEPTED);
    assertThat(assignment.getAcknowledgedAt()).isNotNull();

    assignment.startEnRoute();
    assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.EN_ROUTE);

    assignment.startActive();
    assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.ACTIVE);

    assignment.complete();
    assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.COMPLETED);
    assertThat(assignment.getCompletedAt()).isNotNull();
  }

  @Test
  void cancelAndDecline_updateFieldsCorrectly() {
    RescueAssignment assignment =
        RescueAssignment.create(
            UUID.randomUUID(),
            null,
            UUID.randomUUID(),
            6.9,
            79.8,
            "Location A",
            "Rescue operation",
            (short) 1,
            5,
            null);

    UUID teamId = UUID.randomUUID();
    assignment.assignTeam(teamId);

    assignment.cancel("Flood receded, no longer needed");
    assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.CANCELLED);
    assertThat(assignment.getDeclineReason()).isEqualTo("Flood receded, no longer needed");

    RescueAssignment assignment2 =
        RescueAssignment.create(
            UUID.randomUUID(),
            null,
            UUID.randomUUID(),
            6.9,
            79.8,
            "Location B",
            "Rescue operation",
            (short) 1,
            5,
            null);
    assignment2.assignTeam(teamId);

    assignment2.decline("Team boat engine issue");
    assertThat(assignment2.getStatus()).isEqualTo(AssignmentStatus.UNASSIGNED);
    assertThat(assignment2.getTeamId()).isNull();
    assertThat(assignment2.getDeclineReason()).isEqualTo("Team boat engine issue");
  }

  @Test
  void respond_callsAcknowledgeOrDecline() {
    RescueAssignment acceptAssignment =
        RescueAssignment.create(
            UUID.randomUUID(),
            null,
            UUID.randomUUID(),
            6.9,
            79.8,
            "Location C",
            "Task",
            (short) 2,
            8,
            null);
    acceptAssignment.assignTeam(UUID.randomUUID());
    acceptAssignment.respond(true, null);
    assertThat(acceptAssignment.getStatus()).isEqualTo(AssignmentStatus.ACCEPTED);

    RescueAssignment declineAssignment =
        RescueAssignment.create(
            UUID.randomUUID(),
            null,
            UUID.randomUUID(),
            6.9,
            79.8,
            "Location D",
            "Task",
            (short) 2,
            8,
            null);
    declineAssignment.assignTeam(UUID.randomUUID());
    declineAssignment.respond(false, "Cannot reach access road");
    assertThat(declineAssignment.getStatus()).isEqualTo(AssignmentStatus.UNASSIGNED);
    assertThat(declineAssignment.getDeclineReason()).isEqualTo("Cannot reach access road");
  }
}
