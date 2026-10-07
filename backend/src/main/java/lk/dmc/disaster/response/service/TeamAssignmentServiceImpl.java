package lk.dmc.disaster.response.service;

import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.shared.actor.ActingUserContext;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import lk.dmc.disaster.response.dto.response.AssignmentDto;
import lk.dmc.disaster.response.entity.AssignmentStatus;
import lk.dmc.disaster.response.entity.RescueAssignment;
import lk.dmc.disaster.response.entity.RescueTeam;
import lk.dmc.disaster.response.entity.RescueTeamStatus;
import lk.dmc.disaster.response.repository.RescueAssignmentRepository;
import lk.dmc.disaster.response.repository.RescueTeamRepository;
import lk.dmc.disaster.response.validation.NewAssignmentScreenValidator;
import lk.dmc.disaster.response.validation.RescueTeamsScreenValidator;
import lk.dmc.disaster.response.validation.TeamAssignmentScreenValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class TeamAssignmentServiceImpl implements TeamAssignmentService {

  private final RescueAssignmentRepository assignments;
  private final RescueTeamRepository teams;
  private final ActingUserContext actingUser;
  private final NewAssignmentScreenValidator newAssignmentValidator;
  private final RescueTeamsScreenValidator rescueTeamsValidator;
  private final TeamAssignmentScreenValidator teamAssignmentValidator;

  TeamAssignmentServiceImpl(
      RescueAssignmentRepository assignments,
      RescueTeamRepository teams,
      ActingUserContext actingUser,
      NewAssignmentScreenValidator newAssignmentValidator,
      RescueTeamsScreenValidator rescueTeamsValidator,
      TeamAssignmentScreenValidator teamAssignmentValidator) {
    this.assignments = assignments;
    this.teams = teams;
    this.actingUser = actingUser;
    this.newAssignmentValidator = newAssignmentValidator;
    this.rescueTeamsValidator = rescueTeamsValidator;
    this.teamAssignmentValidator = teamAssignmentValidator;
  }

  @Override
  public AssignmentDto createAssignment(CreateAssignmentCommand command) {
    var user = actingUser.require();
    lk.dmc.disaster.response.dto.request.AssignmentRequest req =
        new lk.dmc.disaster.response.dto.request.AssignmentRequest(
            command.eventId(),
            command.warningId(),
            user.districtId(),
            command.latitude(),
            command.longitude(),
            command.locationText(),
            command.task(),
            command.priority(),
            command.peopleEstimated(),
            command.destinationShelterId(),
            command.teamId());
    newAssignmentValidator.validateCreation(req);

    RescueAssignment a = RescueAssignment.create(
        command.eventId(), command.warningId(), command.createdBy(),
        command.latitude(), command.longitude(),
        command.locationText(), command.task(), command.priority(), command.peopleEstimated(),
        command.destinationShelterId());

    if (command.teamId() != null) {
      a.assignTeam(command.teamId());
      teams.findById(command.teamId()).ifPresent(t -> {
        t.markDispatched();
        teams.save(t);
      });
    }

    RescueAssignment saved = assignments.save(a);
    return AssignmentDto.from(saved);
  }

  @Override
  public AssignmentDto assignTeam(UUID assignmentId, UUID teamId) {
    var user = actingUser.require();
    RescueAssignment assignment = assignments.findById(assignmentId)
        .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Assignment not found"));

    RescueTeam team = rescueTeamsValidator.validateAssignTeam(
        assignment, new lk.dmc.disaster.response.dto.request.AssignTeamRequest(teamId), user.districtId());

    assignment.assignTeam(teamId);
    team.markDispatched();
    teams.save(team);
    RescueAssignment saved = assignments.save(assignment);
    return AssignmentDto.from(saved);
  }

  @Override
  public AssignmentDto cancelAssignment(UUID assignmentId, String reason) {
    RescueAssignment assignment = assignments.findById(assignmentId)
        .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Assignment not found"));

    rescueTeamsValidator.validateCancelAssignment(
        assignment, new lk.dmc.disaster.response.dto.request.CancelAssignmentRequest(reason));

    assignment.cancel(reason);
    RescueAssignment saved = assignments.save(assignment);
    return AssignmentDto.from(saved);
  }

  @Override
  public AssignmentDto respond(UUID assignmentId, boolean accept, String declineReason) {
    var user = actingUser.require();
    RescueAssignment assignment = assignments.findById(assignmentId)
        .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Assignment not found"));

    teamAssignmentValidator.validateRespond(
        assignment, new lk.dmc.disaster.response.dto.request.RespondRequest(accept, declineReason), user);

    if (accept) {
      assignment.acknowledge();
    } else {
      assignment.decline(declineReason);
      teams.findById(user.rescueTeamId()).ifPresent(t -> {
        t.markCompleted();
        teams.save(t);
      });
    }

    RescueAssignment saved = assignments.save(assignment);
    return AssignmentDto.from(saved);
  }

  @Override
  public AssignmentDto getAssignment(UUID id) {
    return AssignmentDto.from(assignments.findById(id)
        .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Assignment not found")));
  }

  @Override
  public AssignmentDto getMyAssignment() {
    var user = actingUser.require();
    List<AssignmentStatus> liveStatuses = List.of(
        AssignmentStatus.PENDING_ACK, AssignmentStatus.ACCEPTED,
        AssignmentStatus.EN_ROUTE, AssignmentStatus.ACTIVE);
    List<RescueAssignment> list = assignments.findByTeamIdAndStatusIn(user.rescueTeamId(), liveStatuses);
    if (list.isEmpty()) {
      return null;
    }
    return AssignmentDto.from(list.get(0));
  }

  @Override
  public List<AssignmentDto> listAssignments(UUID districtId, String status) {
    if (status != null && !status.isBlank()) {
      return assignments.findByDistrictIdAndStatus(districtId, AssignmentStatus.valueOf(status)).stream()
          .map(AssignmentDto::from)
          .toList();
    }
    return assignments.findByDistrictId(districtId).stream()
        .map(AssignmentDto::from)
        .toList();
  }

  @Override
  public Page<AssignmentDto> getAssignments(UUID districtId, String status, int page, int size) {
    Pageable pageable = Pageable.ofSize(size).withPage(page);
    if (status != null && !status.isBlank()) {
      return assignments.findByDistrictIdAndStatus(districtId, AssignmentStatus.valueOf(status), pageable)
          .map(AssignmentDto::from);
    }
    return assignments.findByDistrictId(districtId, pageable).map(AssignmentDto::from);
  }
}
