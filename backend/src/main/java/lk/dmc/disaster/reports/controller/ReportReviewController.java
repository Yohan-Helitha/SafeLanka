package lk.dmc.disaster.reports.controller;

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

  @PatchMapping("/{id}/verify")
  ApiResponse<ReportDetailResponse> verify(
      @PathVariable UUID id, @RequestBody(required = false) @Valid VerifyRequest request) {
    String comment = request == null ? null : request.comment();
    return ApiResponse.of(
        mapper.toDetail(verification.verify(id, actingUser.require().id(), comment)));
  }

  @PatchMapping("/{id}/reject")
  ApiResponse<ReportDetailResponse> reject(
      @PathVariable UUID id, @RequestBody @Valid RejectRequest request) {
    return ApiResponse.of(
        mapper.toDetail(
            verification.reject(
                id, actingUser.require().id(), request.reason(), request.comment())));
  }

  @PatchMapping("/{id}/request-info")
  ApiResponse<ReportDetailResponse> requestInfo(
      @PathVariable UUID id, @RequestBody @Valid RequestInfoRequest request) {
    return ApiResponse.of(
        mapper.toDetail(verification.requestInfo(id, actingUser.require().id(), request.comment())));
  }
}
