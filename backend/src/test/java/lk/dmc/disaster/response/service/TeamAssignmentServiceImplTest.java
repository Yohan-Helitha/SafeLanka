package lk.dmc.disaster.response.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lk.dmc.disaster.response.dto.response.AssignmentDto;
import lk.dmc.disaster.response.entity.AssignmentStatus;
import lk.dmc.disaster.response.entity.RescueAssignment;
import lk.dmc.disaster.response.entity.RescueTeam;
import lk.dmc.disaster.response.entity.RescueTeamStatus;
import lk.dmc.disaster.response.entity.TeamType;
import lk.dmc.disaster.response.repository.RescueAssignmentRepository;
import lk.dmc.disaster.response.repository.RescueTeamRepository;
import lk.dmc.disaster.response.validation.NewAssignmentScreenValidator;
import lk.dmc.disaster.response.validation.RescueTeamsScreenValidator;
import lk.dmc.disaster.response.validation.TeamAssignmentScreenValidator;
import lk.dmc.disaster.shared.actor.ActingUser;
import lk.dmc.disaster.shared.actor.ActingUserContext;
import lk.dmc.disaster.shared.domain.Role;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class TeamAssignmentServiceImplTest {

  @Mock private RescueAssignmentRepository assignments;
  @Mock private RescueTeamRepository teams;
  @Mock private ActingUserContext actingUser;
  @Mock private NewAssignmentScreenValidator newAssignmentValidator;
  @Mock private RescueTeamsScreenValidator rescueTeamsValidator;
  @Mock private TeamAssignmentScreenValidator teamAssignmentValidator;

  private TeamAssignmentServiceImpl service;

  @BeforeEach
  void setUp() {
    service =
        new TeamAssignmentServiceImpl(
            assignments,
            teams,
            actingUser,
            newAssignmentValidator,
            rescueTeamsValidator,
            teamAssignmentValidator);
  }

  @Test
  void createAssignment_withTeam_validatesAssignsTeamAndDispatches() {
    UUID teamId = UUID.randomUUID();
    UUID districtId = UUID.randomUUID();
    UUID creatorId = UUID.randomUUID();
    ActingUser user = new ActingUser(creatorId, Role.DISTRICT_OFFICER, districtId, null, null);
    when(actingUser.require()).thenReturn(user);

    RescueTeam team =
        RescueTeam.create("Team Alpha", UUID.randomUUID(), districtId, TeamType.BOAT, 5);
    when(teams.findById(teamId)).thenReturn(Optional.of(team));
    when(assignments.save(any(RescueAssignment.class))).thenAnswer(inv -> inv.getArgument(0));

    TeamAssignmentService.CreateAssignmentCommand cmd =
        new TeamAssignmentService.CreateAssignmentCommand(
            UUID.randomUUID(),
            null,
            districtId,
            6.92,
            79.86,
            "Kelani riverbank",
            "Help evacuate flood victims",
            (short) 1,
            20,
            null,
            teamId,
            creatorId);

    AssignmentDto dto = service.createAssignment(cmd);

    assertThat(dto).isNotNull();
    assertThat(dto.teamId()).isEqualTo(teamId);
    assertThat(dto.status()).isEqualTo(AssignmentStatus.PENDING_ACK);
    assertThat(team.getStatus()).isEqualTo(RescueTeamStatus.DISPATCHED);

    verify(newAssignmentValidator).validateCreation(any());
    verify(teams).save(team);
    verify(assignments).save(any(RescueAssignment.class));
  }

  @Test
  void createAssignment_withoutTeam_createsUnassigned() {
    UUID districtId = UUID.randomUUID();
    UUID creatorId = UUID.randomUUID();
    ActingUser user = new ActingUser(creatorId, Role.DISTRICT_OFFICER, districtId, null, null);
    when(actingUser.require()).thenReturn(user);
    when(assignments.save(any(RescueAssignment.class))).thenAnswer(inv -> inv.getArgument(0));

    TeamAssignmentService.CreateAssignmentCommand cmd =
        new TeamAssignmentService.CreateAssignmentCommand(
            UUID.randomUUID(),
            null,
            districtId,
            6.92,
            79.86,
            "Kelani riverbank",
            "Help evacuate flood victims",
            (short) 1,
            20,
            null,
            null,
            creatorId);

    AssignmentDto dto = service.createAssignment(cmd);

    assertThat(dto).isNotNull();
    assertThat(dto.teamId()).isNull();
    assertThat(dto.status()).isEqualTo(AssignmentStatus.UNASSIGNED);
    verify(teams, never()).save(any());
  }

  @Test
  void assignTeam_assignmentNotFound_throwsNotFound() {
    UUID assignmentId = UUID.randomUUID();
    UUID teamId = UUID.randomUUID();
    ActingUser user =
        new ActingUser(UUID.randomUUID(), Role.DISTRICT_OFFICER, UUID.randomUUID(), null, null);
    when(actingUser.require()).thenReturn(user);
    when(assignments.findById(assignmentId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.assignTeam(assignmentId, teamId))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.NOT_FOUND);
  }

  @Test
  void assignTeam_valid_assignsTeamMarksDispatchedAndSaves() {
    UUID assignmentId = UUID.randomUUID();
    UUID teamId = UUID.randomUUID();
    UUID districtId = UUID.randomUUID();
    ActingUser user =
        new ActingUser(UUID.randomUUID(), Role.DISTRICT_OFFICER, districtId, null, null);
    when(actingUser.require()).thenReturn(user);

    RescueAssignment assignment =
        RescueAssignment.create(
            UUID.randomUUID(),
            null,
            UUID.randomUUID(),
            6.9,
            79.8,
            "Loc",
            "Task",
            (short) 1,
            5,
            null);
    when(assignments.findById(assignmentId)).thenReturn(Optional.of(assignment));

    RescueTeam team =
        RescueTeam.create("Team Beta", UUID.randomUUID(), districtId, TeamType.MEDICAL, 4);
    when(rescueTeamsValidator.validateAssignTeam(eq(assignment), any(), eq(districtId)))
        .thenReturn(team);
    when(assignments.save(any(RescueAssignment.class))).thenAnswer(inv -> inv.getArgument(0));

    AssignmentDto result = service.assignTeam(assignmentId, teamId);

    assertThat(result.status()).isEqualTo(AssignmentStatus.PENDING_ACK);
    assertThat(result.teamId()).isEqualTo(teamId);
    assertThat(team.getStatus()).isEqualTo(RescueTeamStatus.DISPATCHED);

    verify(teams).save(team);
    verify(assignments).save(assignment);
  }

  @Test
  void cancelAssignment_valid_cancelsAndSaves() {
    UUID assignmentId = UUID.randomUUID();
    RescueAssignment assignment =
        RescueAssignment.create(
            UUID.randomUUID(),
            null,
            UUID.randomUUID(),
            6.9,
            79.8,
            "Loc",
            "Task",
            (short) 1,
            5,
            null);
    when(assignments.findById(assignmentId)).thenReturn(Optional.of(assignment));
    when(assignments.save(any(RescueAssignment.class))).thenAnswer(inv -> inv.getArgument(0));

    AssignmentDto result = service.cancelAssignment(assignmentId, "Hazard cleared");

    assertThat(result.status()).isEqualTo(AssignmentStatus.CANCELLED);
    assertThat(result.declineReason()).isEqualTo("Hazard cleared");
    verify(rescueTeamsValidator).validateCancelAssignment(eq(assignment), any());
    verify(assignments).save(assignment);
  }

  @Test
  void respond_accept_acknowledgesAssignment() {
    UUID assignmentId = UUID.randomUUID();
    UUID teamId = UUID.randomUUID();
    ActingUser user = new ActingUser(UUID.randomUUID(), Role.RESCUE_MEMBER, null, null, teamId);
    when(actingUser.require()).thenReturn(user);

    RescueAssignment assignment =
        RescueAssignment.create(
            UUID.randomUUID(),
            null,
            UUID.randomUUID(),
            6.9,
            79.8,
            "Loc",
            "Task",
            (short) 1,
            5,
            null);
    assignment.assignTeam(teamId);

    when(assignments.findById(assignmentId)).thenReturn(Optional.of(assignment));
    when(assignments.save(any(RescueAssignment.class))).thenAnswer(inv -> inv.getArgument(0));

    AssignmentDto result = service.respond(assignmentId, true, null);

    assertThat(result.status()).isEqualTo(AssignmentStatus.ACCEPTED);
    verify(teamAssignmentValidator).validateRespond(eq(assignment), any(), eq(user));
    verify(assignments).save(assignment);
  }

  @Test
  void respond_decline_declinesAndMarksTeamCompleted() {
    UUID assignmentId = UUID.randomUUID();
    UUID teamId = UUID.randomUUID();
    ActingUser user = new ActingUser(UUID.randomUUID(), Role.RESCUE_MEMBER, null, null, teamId);
    when(actingUser.require()).thenReturn(user);

    RescueAssignment assignment =
        RescueAssignment.create(
            UUID.randomUUID(),
            null,
            UUID.randomUUID(),
            6.9,
            79.8,
            "Loc",
            "Task",
            (short) 1,
            5,
            null);
    assignment.assignTeam(teamId);

    RescueTeam team =
        RescueTeam.create("Team A", UUID.randomUUID(), UUID.randomUUID(), TeamType.BOAT, 4);
    team.markDispatched();

    when(assignments.findById(assignmentId)).thenReturn(Optional.of(assignment));
    when(teams.findById(teamId)).thenReturn(Optional.of(team));
    when(assignments.save(any(RescueAssignment.class))).thenAnswer(inv -> inv.getArgument(0));

    AssignmentDto result = service.respond(assignmentId, false, "Vehicle breakdown");

    assertThat(result.status()).isEqualTo(AssignmentStatus.UNASSIGNED);
    assertThat(result.teamId()).isNull();
    assertThat(result.declineReason()).isEqualTo("Vehicle breakdown");
    assertThat(team.getStatus()).isEqualTo(RescueTeamStatus.AVAILABLE);

    verify(teams).save(team);
    verify(assignments).save(assignment);

    // Also test decline when team is not found in repository (optional branch)
    when(teams.findById(teamId)).thenReturn(Optional.empty());
    service.respond(assignmentId, false, "No team found");
  }

  @Test
  void getAssignment_foundAndNotFound() {
    UUID id = UUID.randomUUID();
    RescueAssignment a =
        RescueAssignment.create(
            UUID.randomUUID(),
            null,
            UUID.randomUUID(),
            6.9,
            79.8,
            "Loc",
            "Task",
            (short) 1,
            5,
            null);
    when(assignments.findById(id)).thenReturn(Optional.of(a));

    AssignmentDto found = service.getAssignment(id);
    assertThat(found).isNotNull();

    UUID missingId = UUID.randomUUID();
    when(assignments.findById(missingId)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.getAssignment(missingId))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.NOT_FOUND);
  }

  @Test
  void getMyAssignment_returnsLiveAssignmentOrNull() {
    UUID teamId = UUID.randomUUID();
    ActingUser user = new ActingUser(UUID.randomUUID(), Role.RESCUE_MEMBER, null, null, teamId);
    when(actingUser.require()).thenReturn(user);

    RescueAssignment a =
        RescueAssignment.create(
            UUID.randomUUID(),
            null,
            UUID.randomUUID(),
            6.9,
            79.8,
            "Loc",
            "Task",
            (short) 1,
            5,
            null);
    a.assignTeam(teamId);

    when(assignments.findByTeamIdAndStatusIn(eq(teamId), any())).thenReturn(List.of(a));
    AssignmentDto dto = service.getMyAssignment();
    assertThat(dto).isNotNull();

    when(assignments.findByTeamIdAndStatusIn(eq(teamId), any())).thenReturn(List.of());
    assertThat(service.getMyAssignment()).isNull();
  }

  @Test
  void listAssignments_and_getAssignmentsPageable() {
    UUID districtId = UUID.randomUUID();
    RescueAssignment a1 =
        RescueAssignment.create(
            UUID.randomUUID(),
            null,
            UUID.randomUUID(),
            6.9,
            79.8,
            "Loc 1",
            "Task 1",
            (short) 1,
            5,
            null);
    RescueAssignment a2 =
        RescueAssignment.create(
            UUID.randomUUID(),
            null,
            UUID.randomUUID(),
            6.9,
            79.8,
            "Loc 2",
            "Task 2",
            (short) 2,
            8,
            null);

    when(assignments.findByDistrictIdAndStatus(districtId, AssignmentStatus.PENDING_ACK))
        .thenReturn(List.of(a1));
    when(assignments.findByDistrictId(districtId)).thenReturn(List.of(a1, a2));

    List<AssignmentDto> filtered = service.listAssignments(districtId, "PENDING_ACK");
    assertThat(filtered).hasSize(1);

    List<AssignmentDto> all = service.listAssignments(districtId, null);
    assertThat(all).hasSize(2);

    Page<RescueAssignment> page1 = new PageImpl<>(List.of(a1));
    when(assignments.findByDistrictIdAndStatus(
            eq(districtId), eq(AssignmentStatus.PENDING_ACK), any(Pageable.class)))
        .thenReturn(page1);
    Page<AssignmentDto> pageResultFiltered =
        service.getAssignments(districtId, "PENDING_ACK", 0, 10);
    assertThat(pageResultFiltered.getContent()).hasSize(1);

    Page<RescueAssignment> page2 = new PageImpl<>(List.of(a1, a2));
    when(assignments.findByDistrictId(eq(districtId), any(Pageable.class))).thenReturn(page2);
    Page<AssignmentDto> pageResultAll = service.getAssignments(districtId, null, 0, 10);
    assertThat(pageResultAll.getContent()).hasSize(2);

    Page<AssignmentDto> pageResultBlank = service.getAssignments(districtId, "   ", 0, 10);
    assertThat(pageResultBlank.getContent()).hasSize(2);
  }
}
