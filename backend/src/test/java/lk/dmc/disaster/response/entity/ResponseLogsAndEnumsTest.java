package lk.dmc.disaster.response.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ResponseLogsAndEnumsTest {

  @Test
  void activityLog_creation() {
    UUID districtId = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();
    Instant now = Instant.now();

    ActivityLog log =
        ActivityLog.create(districtId, eventId, ActivityType.RELIEF, "Allocated 100 blankets", now);

    assertThat(log.getId()).isNotNull();
    assertThat(log.getDistrictId()).isEqualTo(districtId);
    assertThat(log.getEventId()).isEqualTo(eventId);
    assertThat(log.getType()).isEqualTo(ActivityType.RELIEF);
    assertThat(log.getMessage()).isEqualTo("Allocated 100 blankets");
    assertThat(log.getOccurredAt()).isEqualTo(now);
  }

  @Test
  void occupancyLog_creation() {
    UUID shelterId = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();
    UUID recordedBy = UUID.randomUUID();
    Instant now = Instant.now();

    OccupancyLog log = OccupancyLog.create(shelterId, eventId, 75, 15, recordedBy, now);

    assertThat(log.getId()).isNotNull();
    assertThat(log.getShelterId()).isEqualTo(shelterId);
    assertThat(log.getEventId()).isEqualTo(eventId);
    assertThat(log.getOccupancy()).isEqualTo(75);
    assertThat(log.getDelta()).isEqualTo(15);
    assertThat(log.getRecordedBy()).isEqualTo(recordedBy);
    assertThat(log.getRecordedAt()).isEqualTo(now);
  }

  @Test
  void teamStatusLog_creation() {
    UUID teamId = UUID.randomUUID();
    UUID assignmentId = UUID.randomUUID();
    UUID changedBy = UUID.randomUUID();
    UUID clientRef = UUID.randomUUID();
    Instant now = Instant.now();

    TeamStatusLog log =
        TeamStatusLog.create(
            teamId, assignmentId, "AVAILABLE", "DISPATCHED", changedBy, now, true, clientRef);

    assertThat(log.getId()).isNotNull();
    assertThat(log.getTeamId()).isEqualTo(teamId);
    assertThat(log.getAssignmentId()).isEqualTo(assignmentId);
    assertThat(log.getFromStatus()).isEqualTo("AVAILABLE");
    assertThat(log.getToStatus()).isEqualTo("DISPATCHED");
    assertThat(log.getChangedBy()).isEqualTo(changedBy);
    assertThat(log.getChangedAt()).isEqualTo(now);
    assertThat(log.isRecordedOffline()).isTrue();
    assertThat(log.getClientRef()).isEqualTo(clientRef);
    assertThat(log.getSyncedAt()).isNotNull();
  }

  @Test
  void enums_valuesAndValueOf() {
    assertThat(ActivityType.values())
        .contains(
            ActivityType.WARNING,
            ActivityType.DISPATCH,
            ActivityType.TEAM_STATUS,
            ActivityType.SHELTER,
            ActivityType.RELIEF);
    assertThat(AllocationStatus.values())
        .contains(
            AllocationStatus.ALLOCATED,
            AllocationStatus.PARTIALLY_DISTRIBUTED,
            AllocationStatus.DISTRIBUTED,
            AllocationStatus.CANCELLED);
    assertThat(AssignmentStatus.values())
        .contains(
            AssignmentStatus.UNASSIGNED,
            AssignmentStatus.PENDING_ACK,
            AssignmentStatus.ACCEPTED,
            AssignmentStatus.EN_ROUTE,
            AssignmentStatus.ACTIVE,
            AssignmentStatus.COMPLETED,
            AssignmentStatus.CANCELLED);
    assertThat(RescueTeamStatus.values())
        .contains(
            RescueTeamStatus.AVAILABLE,
            RescueTeamStatus.DISPATCHED,
            RescueTeamStatus.EN_ROUTE,
            RescueTeamStatus.ACTIVE,
            RescueTeamStatus.OFFLINE_UNKNOWN);
    assertThat(ShelterStatus.values())
        .contains(ShelterStatus.OPEN, ShelterStatus.FULL, ShelterStatus.CLOSED);
    assertThat(TeamType.values()).contains(TeamType.BOAT, TeamType.MEDICAL, TeamType.SEARCH);
  }
}
