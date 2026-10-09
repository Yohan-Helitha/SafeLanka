package lk.dmc.disaster.analytics.controller;

import lk.dmc.disaster.analytics.mapper.AnalyticsMapper;
import lk.dmc.disaster.analytics.dto.DisasterReportResponse;
import lk.dmc.disaster.analytics.dto.GenerateReportRequest;
import lk.dmc.disaster.analytics.dto.ReportSummaryResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.analytics.service.AnalyticsService;
import lk.dmc.disaster.analytics.export.ExportService;
import lk.dmc.disaster.analytics.export.ExportedFile;
import lk.dmc.disaster.shared.api.ApiResponse;
import lk.dmc.disaster.analytics.entity.EventSummary;
import lk.dmc.disaster.shared.actor.ActingUserContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import lk.dmc.disaster.shared.actor.RequiresRole;
import lk.dmc.disaster.shared.domain.Role;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Analytics", description = "Disaster Analytics API")
// Reading is open to both officer roles (as in AccessRules); generating a report is DMC-only.
@RequiresRole({Role.DMC_OFFICER, Role.DISTRICT_OFFICER})
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final ExportService exportService;
    private final AnalyticsMapper mapper;
    private final ActingUserContext actingUser;

    public AnalyticsController(AnalyticsService analyticsService, ExportService exportService, AnalyticsMapper mapper, ActingUserContext actingUser) {
        this.analyticsService = analyticsService;
        this.exportService = exportService;
        this.mapper = mapper;
        this.actingUser = actingUser;
    }

    // /events is the path in the module spec. Browser ad blockers block any request to
    // /analytics/events (it looks like a tracking call), so the frontend uses /disaster-events.
    @GetMapping({"/events", "/disaster-events"})
    @Operation(summary = "List available events for analytics")
    public ApiResponse<List<EventSummary>> getEvents(@RequestParam(required = false) String status) {
        return ApiResponse.of(analyticsService.listAvailableEvents(status));
    }

    @PostMapping("/reports")
    @RequiresRole(Role.DMC_OFFICER)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Generate a new disaster report")
    public ApiResponse<DisasterReportResponse> generateReport(
        @Valid @RequestBody GenerateReportRequest request
    ) {
        var context = mapper.toContext(request, actingUser.require().id());
        var report = analyticsService.generateReport(context);
        return ApiResponse.of(mapper.toResponse(report));
    }

    @GetMapping("/reports")
    @Operation(summary = "List saved reports")
    public ApiResponse<List<ReportSummaryResponse>> getReports(
        @RequestParam(required = false) UUID eventId,
        @Parameter(hidden = true) Pageable pageable
    ) {
        var list = analyticsService.listSavedReports(eventId).stream()
            .map(mapper::toSummaryResponse)
            .toList();
            
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), list.size());
        if (start > list.size()) {
            return ApiResponse.page(new PageImpl<>(List.of(), pageable, list.size()));
        }
        return ApiResponse.page(new PageImpl<>(list.subList(start, end), pageable, list.size()));
    }

    @GetMapping("/reports/{id}")
    @Operation(summary = "Get a saved report")
    public ApiResponse<DisasterReportResponse> getReport(@PathVariable UUID id) {
        return ApiResponse.of(mapper.toResponse(analyticsService.getReport(id)));
    }

    @GetMapping("/reports/{id}/export")
    @Operation(summary = "Export a saved report as PDF or CSV (format=PDF|CSV)")
    public ResponseEntity<byte[]> exportReport(
        @PathVariable UUID id,
        @RequestParam String format
    ) {
        ExportedFile file = exportService.export(analyticsService.getReport(id), format);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.filename() + "\"")
            .contentType(MediaType.parseMediaType(file.contentType()))
            .body(file.content());
    }
}
