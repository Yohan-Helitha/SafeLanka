package lk.dmc.disaster.response.controller;

import jakarta.validation.Valid;
import java.util.UUID;
import lk.dmc.disaster.response.dto.request.AssignTeamRequest;
import lk.dmc.disaster.response.dto.request.AssignmentRequest;
import lk.dmc.disaster.response.dto.request.CancelAssignmentRequest;
import lk.dmc.disaster.response.dto.request.RespondRequest;
import lk.dmc.disaster.response.dto.response.AssignmentDto;
import lk.dmc.disaster.response.service.TeamAssignmentService;
import lk.dmc.disaster.shared.actor.ActingUserContext;
import lk.dmc.disaster.shared.actor.RequiresRole;
import lk.dmc.disaster.shared.api.ApiResponse;
import lk.dmc.disaster.shared.domain.Role;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assignments")
public class TeamAssignmentController {

  private final TeamAssignmentService assignmentService;
  private final ActingUserContext actingUser;

  TeamAssignmentController(TeamAssignmentService assignmentService, ActingUserContext actingUser) {
    this.assignmentService = assignmentService;
    this.actingUser = actingUser;
  }

  @PostMapping
  @RequiresRole(Role.DISTRICT_OFFICER)
  ResponseEntity<ApiResponse<AssignmentDto>> create(@Valid @RequestBody AssignmentRequest request) {
    var user = actingUser.require();
    TeamAssignmentService.CreateAssignmentCommand command =
        new TeamAssignmentService.CreateAssignmentCommand(
            request.eventId(),
            request.warningId(),
            request.districtId(),
            request.latitude(),
            request.longitude(),
            request.locationText(),
            request.task(),
            request.priority(),
            request.peopleEstimated(),
            request.destinationShelterId(),
            request.teamId(),
            user.id());
    AssignmentDto created = assignmentService.createAssignment(command);
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(created));
  }

  @PostMapping("/{id}/assign")
  @RequiresRole(Role.DISTRICT_OFFICER)
  ApiResponse<AssignmentDto> assign(
      @PathVariable UUID id, @Valid @RequestBody AssignTeamRequest request) {
    return ApiResponse.of(assignmentService.assignTeam(id, request.teamId()));
  }

  @PostMapping("/{id}/cancel")
  @RequiresRole(Role.DISTRICT_OFFICER)
  ApiResponse<AssignmentDto> cancel(
      @PathVariable UUID id, @Valid @RequestBody CancelAssignmentRequest request) {
    return ApiResponse.of(assignmentService.cancelAssignment(id, request.reason()));
  }

  @GetMapping
  @RequiresRole({Role.DISTRICT_OFFICER, Role.DMC_OFFICER})
  ApiResponse<?> list(
      @RequestParam(required = false) UUID districtId,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size) {
    if (page != null && size != null) {
      return ApiResponse.page(assignmentService.getAssignments(districtId, status, page, size));
    }
    return ApiResponse.of(assignmentService.listAssignments(districtId, status));
  }

  @GetMapping("/{id}")
  @RequiresRole({Role.DISTRICT_OFFICER, Role.DMC_OFFICER, Role.RESCUE_MEMBER})
  ApiResponse<AssignmentDto> get(@PathVariable UUID id) {
    return ApiResponse.of(assignmentService.getAssignment(id));
  }

  @GetMapping("/mine")
  @RequiresRole(Role.RESCUE_MEMBER)
  ResponseEntity<ApiResponse<AssignmentDto>> mine() {
    AssignmentDto dto = assignmentService.getMyAssignment();
    if (dto == null) {
      return ResponseEntity.noContent().build();
    }
    return ResponseEntity.ok(ApiResponse.of(dto));
  }

  @PostMapping("/{id}/respond")
  @RequiresRole(Role.RESCUE_MEMBER)
  ApiResponse<AssignmentDto> respond(
      @PathVariable UUID id, @Valid @RequestBody RespondRequest request) {
    return ApiResponse.of(assignmentService.respond(id, request.accept(), request.declineReason()));
  }
}
