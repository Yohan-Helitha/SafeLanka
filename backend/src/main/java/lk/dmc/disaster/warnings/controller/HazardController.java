package lk.dmc.disaster.warnings.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.shared.actor.RequiresRole;
import lk.dmc.disaster.shared.api.ApiResponse;
import lk.dmc.disaster.shared.domain.Role;
import lk.dmc.disaster.warnings.dto.CreateHazardRequest;
import lk.dmc.disaster.warnings.dto.HazardDetail;
import lk.dmc.disaster.warnings.dto.HazardListItem;
import lk.dmc.disaster.warnings.dto.HazardStatusRequest;
import lk.dmc.disaster.warnings.entity.Hazard;
import lk.dmc.disaster.warnings.entity.HazardStatus;
import lk.dmc.disaster.warnings.mapper.HazardMapper;
import lk.dmc.disaster.warnings.service.HazardAssessmentService;
import lk.dmc.disaster.warnings.service.HazardQueryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Hazard assessment for DMC duty officers. HTTP only: every rule lives in the services. */
@Tag(name = "Hazards", description = "Assess hazards before deciding whether to warn the public")
@RestController
@RequestMapping("/api/hazards")
@RequiresRole(Role.DMC_OFFICER)
class HazardController {

  private final HazardQueryService query;
  private final HazardAssessmentService assessment;
  private final HazardMapper mapper;

  HazardController(
      HazardQueryService query, HazardAssessmentService assessment, HazardMapper mapper) {
    this.query = query;
    this.assessment = assessment;
    this.mapper = mapper;
  }

  @Operation(
      summary = "List hazards, most severe first",
      description =
          "Open hazards (under assessment, warned, monitoring) unless status is given, for"
              + " example status=UNDER_ASSESSMENT,WARNED.")
  @GetMapping
  ApiResponse<List<HazardListItem>> list(
      @RequestParam(required = false) List<HazardStatus> status,
      @RequestParam(required = false) UUID hazardTypeId,
      @RequestParam(required = false) UUID districtId) {
    return ApiResponse.of(mapper.toListItems(query.list(status, hazardTypeId, districtId)));
  }

  @Operation(summary = "Hazard detail with verified reports, gauge readings and warnings")
  @GetMapping("/{id}")
  ApiResponse<HazardDetail> detail(@PathVariable UUID id) {
    return ApiResponse.of(mapper.toDetail(query.detail(id)));
  }

  @Operation(
      summary = "Record a hazard by hand",
      description = "Example: a flood at Hanwella in Colombo district, severity 4.")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  ApiResponse<HazardDetail> create(@Valid @RequestBody CreateHazardRequest request) {
    Hazard hazard = assessment.create(request.toCommand());
    return ApiResponse.of(mapper.toDetail(query.detail(hazard.getId())));
  }

  @Operation(summary = "Keep a hazard under monitoring, or resolve it")
  @PatchMapping("/{id}/status")
  ApiResponse<HazardDetail> setStatus(
      @PathVariable UUID id, @Valid @RequestBody HazardStatusRequest request) {
    assessment.setStatus(id, request.status());
    return ApiResponse.of(mapper.toDetail(query.detail(id)));
  }
}
