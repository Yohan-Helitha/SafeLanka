package lk.dmc.disaster.warnings.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lk.dmc.disaster.shared.actor.ActingUserContext;
import lk.dmc.disaster.shared.actor.RequiresRole;
import lk.dmc.disaster.shared.api.ApiResponse;
import lk.dmc.disaster.shared.domain.Role;
import lk.dmc.disaster.warnings.dto.CancelWarningRequest;
import lk.dmc.disaster.warnings.dto.EscalateWarningRequest;
import lk.dmc.disaster.warnings.dto.PageResponse;
import lk.dmc.disaster.warnings.dto.PublishWarningRequest;
import lk.dmc.disaster.warnings.dto.UpdateWarningRequest;
import lk.dmc.disaster.warnings.dto.WarningResponse;
import lk.dmc.disaster.warnings.entity.Warning;
import lk.dmc.disaster.warnings.entity.WarningStatus;
import lk.dmc.disaster.warnings.mapper.WarningMapper;
import lk.dmc.disaster.warnings.service.WarningPublicationService;
import lk.dmc.disaster.warnings.service.WarningQueryService;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Issuing and managing public warnings. HTTP only: every rule lives in the services. */
@Tag(name = "Warnings", description = "Issue, edit, escalate and cancel public warnings")
@RestController
@RequestMapping("/api/warnings")
@RequiresRole(Role.DMC_OFFICER)
class WarningController {

  private final WarningPublicationService publication;
  private final WarningQueryService query;
  private final WarningMapper mapper;
  private final ActingUserContext actingUser;

  WarningController(
      WarningPublicationService publication,
      WarningQueryService query,
      WarningMapper mapper,
      ActingUserContext actingUser) {
    this.publication = publication;
    this.query = query;
    this.mapper = mapper;
    this.actingUser = actingUser;
  }

  @Operation(
      summary = "Issue a warning",
      description =
          "Sends the warning to everyone in the chosen districts or river basins. confirm must be"
              + " true. 409 if an ACTIVE warning of the same hazard already covers the area.")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  ApiResponse<WarningResponse> publish(@Valid @RequestBody PublishWarningRequest request) {
    Warning warning = publication.publish(request.toCommand(actingUser.require().id()));
    return respond(warning.getId());
  }

  @Operation(summary = "Change the texts of an ACTIVE warning (nothing is sent again)")
  @PutMapping("/{id}")
  ApiResponse<WarningResponse> update(
      @PathVariable UUID id, @Valid @RequestBody UpdateWarningRequest request) {
    publication.updateContent(id, request.toContent());
    return respond(id);
  }

  @Operation(
      summary = "Escalate to a higher level",
      description = "Creates a new warning that supersedes this one and sends it again.")
  @PostMapping("/{id}/escalate")
  @ResponseStatus(HttpStatus.CREATED)
  ApiResponse<WarningResponse> escalate(
      @PathVariable UUID id, @Valid @RequestBody EscalateWarningRequest request) {
    Warning next = publication.escalate(request.toCommand(id, actingUser.require().id()));
    return respond(next.getId());
  }

  @Operation(summary = "Cancel an ACTIVE warning with a reason")
  @PostMapping("/{id}/cancel")
  ApiResponse<WarningResponse> cancel(
      @PathVariable UUID id, @Valid @RequestBody CancelWarningRequest request) {
    publication.cancel(id, request.reason());
    return respond(id);
  }

  @Operation(summary = "List warnings, newest first")
  @GetMapping
  @RequiresRole({Role.DMC_OFFICER, Role.DISTRICT_OFFICER})
  ApiResponse<PageResponse<WarningResponse>> list(
      @RequestParam(required = false) WarningStatus status,
      @RequestParam(required = false) UUID eventId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    Sort newestFirst = Sort.by(Sort.Direction.DESC, "issuedAt");
    return ApiResponse.of(
        mapper.toPage(query.list(status, eventId, Paging.of(page, size, newestFirst))));
  }

  @Operation(summary = "One warning with its delivery totals")
  @GetMapping("/{id}")
  @RequiresRole({Role.DMC_OFFICER, Role.DISTRICT_OFFICER})
  ApiResponse<WarningResponse> get(@PathVariable UUID id) {
    return respond(id);
  }

  private ApiResponse<WarningResponse> respond(UUID warningId) {
    return ApiResponse.of(mapper.toResponse(query.get(warningId)));
  }
}
