package lk.dmc.disaster.reports.controller;

import jakarta.validation.Valid;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.reports.service.PhotoContent;
import lk.dmc.disaster.reports.service.PhotoUpload;
import lk.dmc.disaster.reports.service.ReportQueryService;
import lk.dmc.disaster.reports.service.ReportSubmissionService;
import lk.dmc.disaster.reports.service.SubmissionResult;
import lk.dmc.disaster.reports.service.SubmitReportCommand;
import lk.dmc.disaster.shared.actor.ActingUser;
import lk.dmc.disaster.shared.actor.ActingUserContext;
import lk.dmc.disaster.shared.actor.RequiresRole;
import lk.dmc.disaster.shared.api.ApiResponse;
import lk.dmc.disaster.shared.domain.Role;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Citizen and volunteer endpoints of UC02: submit, my reports, one report, its photo. */
@RestController
@RequestMapping("/api/reports")
class ReportController {

  private static final String DEFAULT_PAGE_SIZE = "20";

  private final ReportSubmissionService submission;
  private final ReportQueryService queries;
  private final ReportMapper mapper;
  private final ActingUserContext actingUser;

  ReportController(
      ReportSubmissionService submission,
      ReportQueryService queries,
      ReportMapper mapper,
      ActingUserContext actingUser) {
    this.submission = submission;
    this.queries = queries;
    this.mapper = mapper;
    this.actingUser = actingUser;
  }

  /** 201 for a new report, 200 when the same {@code clientRef} had already been synced. */
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @RequiresRole({Role.CITIZEN, Role.VOLUNTEER})
  ResponseEntity<ApiResponse<ReportListItemResponse>> submit(
      @RequestPart("report") @Valid SubmitReportRequest report,
      @RequestPart(value = "photo", required = false) MultipartFile photo) {
    SubmissionResult result = submission.submit(actingUser.require().id(), toCommand(report, photo));
    HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
    return ResponseEntity.status(status)
        .body(ApiResponse.of(mapper.toListItem(result)));
  }

  @GetMapping("/mine")
  @RequiresRole({Role.CITIZEN, Role.VOLUNTEER})
  ApiResponse<List<ReportListItemResponse>> mine(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int size) {
    return ApiResponse.page(
        queries.mine(actingUser.require().id(), page, size).map(mapper::toListItem));
  }

  @GetMapping("/{id}")
  @RequiresRole({Role.CITIZEN, Role.VOLUNTEER, Role.DMC_OFFICER})
  ApiResponse<ReportDetailResponse> detail(@PathVariable UUID id) {
    ActingUser actor = actingUser.require();
    return ApiResponse.of(mapper.toDetail(queries.detail(id, actor)));
  }

  @GetMapping("/{id}/photo")
  @RequiresRole({Role.CITIZEN, Role.VOLUNTEER, Role.DMC_OFFICER})
  ResponseEntity<byte[]> photo(@PathVariable UUID id) {
    PhotoContent photo = queries.photo(id, actingUser.require());
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(photo.contentType()))
        .header("Cache-Control", "private, max-age=3600")
        .body(photo.bytes());
  }

  private static SubmitReportCommand toCommand(SubmitReportRequest r, MultipartFile photo) {
    return new SubmitReportCommand(
        r.clientRef(),
        r.hazardTypeId(),
        r.category(),
        r.description(),
        r.latitude(),
        r.longitude(),
        r.manualLocationText(),
        r.districtId(),
        r.capturedAt(),
        toUpload(photo));
  }

  private static PhotoUpload toUpload(MultipartFile photo) {
    if (photo == null || photo.isEmpty()) {
      return null;
    }
    try {
      return new PhotoUpload(photo.getContentType(), photo.getBytes());
    } catch (IOException e) {
      throw new UncheckedIOException("Could not read the uploaded photo.", e);
    }
  }
}
