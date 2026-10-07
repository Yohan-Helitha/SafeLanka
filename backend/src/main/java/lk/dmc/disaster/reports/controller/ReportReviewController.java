package lk.dmc.disaster.reports.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.reports.entity.ReportStatus;
import lk.dmc.disaster.reports.service.ReportQueryService;
import lk.dmc.disaster.reports.service.ReportVerificationService;
import lk.dmc.disaster.shared.actor.ActingUserContext;
import lk.dmc.disaster.shared.actor.RequiresRole;
import lk.dmc.disaster.shared.api.ApiResponse;
import lk.dmc.disaster.shared.domain.Role;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** DMC officer endpoints of UC02: the ground reports queue and the three decisions. */
@Tag(name = "Report review", description = "DMC officer queue and decisions (UC02)")
@RestController
@RequestMapping("/api/reports")
@RequiresRole(Role.DMC_OFFICER)
class ReportReviewController {

  private final ReportQueryService queries;
  private final ReportVerificationService verification;
  private final ReportMapper mapper;
  private final ActingUserContext actingUser;

  ReportReviewController(
      ReportQueryService queries,
      ReportVerificationService verification,
      ReportMapper mapper,
      ActingUserContext actingUser) {
    this.queries = queries;
    this.verification = verification;
    this.mapper = mapper;
    this.actingUser = actingUser;
  }

  /** Oldest first; every filter is optional. */
  @Operation(
      summary = "Ground reports queue",
      description = "Role: DMC_OFFICER. Oldest first; filters status, hazardTypeId, districtId are optional.")
  @GetMapping
  ApiResponse<List<ReportListItemResponse>> queue(
      @RequestParam(required = false) ReportStatus status,
      @RequestParam(required = false) UUID hazardTypeId,
      @RequestParam(required = false) UUID districtId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return ApiResponse.page(
        queries.queue(status, hazardTypeId, districtId, page, size).map(mapper::toListItem));
  }

  @Operation(
      summary = "Verify a report",
      description = "Role: DMC_OFFICER. Publishes ReportVerifiedEvent. 409 when already decided, 422 for your own report.")
  @PatchMapping("/{id}/verify")
  ApiResponse<ReportDetailResponse> verify(
      @PathVariable UUID id, @RequestBody(required = false) @Valid VerifyRequest request) {
    String comment = request == null ? null : request.comment();
    Integer severity = request == null ? null : request.severity();
    return ApiResponse.of(
        mapper.toDetail(verification.verify(id, actingUser.require().id(), comment, severity)));
  }

  @Operation(
      summary = "Reject a report",
      description = "Role: DMC_OFFICER. A reason is required; the comment is required when the reason is OTHER.")
  @PatchMapping("/{id}/reject")
  ApiResponse<ReportDetailResponse> reject(
      @PathVariable UUID id, @RequestBody @Valid RejectRequest request) {
    return ApiResponse.of(
        mapper.toDetail(
            verification.reject(
                id, actingUser.require().id(), request.reason(), request.comment())));
  }

  @Operation(
      summary = "Ask the reporter for more information",
      description = "Role: DMC_OFFICER. Only from PENDING; the comment (5-300 characters) says what is missing.")
  @PatchMapping("/{id}/request-info")
  ApiResponse<ReportDetailResponse> requestInfo(
      @PathVariable UUID id, @RequestBody @Valid RequestInfoRequest request) {
    return ApiResponse.of(
        mapper.toDetail(verification.requestInfo(id, actingUser.require().id(), request.comment())));
  }
}
