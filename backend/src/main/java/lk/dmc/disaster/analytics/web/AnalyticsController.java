package lk.dmc.disaster.analytics.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.analytics.application.AnalyticsService;
import lk.dmc.disaster.analytics.export.ExportService;
import lk.dmc.disaster.analytics.domain.EventSummary;
import lk.dmc.disaster.shared.actor.ActingUserContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import lk.dmc.disaster.shared.actor.RequiresRole;
import lk.dmc.disaster.shared.actor.Role;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Analytics", description = "Disaster Analytics API")
@RequiresRole(Role.DMC_OFFICER)
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

    @GetMapping("/events")
    @Operation(summary = "List available events for analytics")
    public lk.dmc.disaster.shared.api.ApiResponse<List<EventSummary>> getEvents(@RequestParam(required = false) String status) {
        return lk.dmc.disaster.shared.api.ApiResponse.of(analyticsService.listAvailableEvents(status));
    }

    @PostMapping("/reports")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Generate a new disaster report")
    public lk.dmc.disaster.shared.api.ApiResponse<DisasterReportResponse> generateReport(
        @Valid @RequestBody GenerateReportRequest request
    ) {
        var context = mapper.toContext(request, actingUser.require().id());
        var report = analyticsService.generateReport(context);
        return lk.dmc.disaster.shared.api.ApiResponse.of(mapper.toResponse(report));
    }

    @GetMapping("/reports")
    @Operation(summary = "List saved reports")
    public lk.dmc.disaster.shared.api.ApiResponse<List<ReportSummaryResponse>> getReports(
        @RequestParam(required = false) UUID eventId,
        @Parameter(hidden = true) Pageable pageable
    ) {
        var list = analyticsService.listSavedReports(eventId).stream()
            .map(mapper::toSummaryResponse)
            .toList();
            
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), list.size());
        if (start > list.size()) {
            return lk.dmc.disaster.shared.api.ApiResponse.page(new PageImpl<>(List.of(), pageable, list.size()));
        }
        return lk.dmc.disaster.shared.api.ApiResponse.page(new PageImpl<>(list.subList(start, end), pageable, list.size()));
    }

    @GetMapping("/reports/{id}")
    @Operation(summary = "Get a saved report")
    public lk.dmc.disaster.shared.api.ApiResponse<DisasterReportResponse> getReport(@PathVariable UUID id) {
        return lk.dmc.disaster.shared.api.ApiResponse.of(mapper.toResponse(analyticsService.getReport(id)));
    }

    @GetMapping("/reports/{id}/export")
    @Operation(summary = "Export a report to PDF or CSV")
    public ResponseEntity<byte[]> exportReport(
        @PathVariable UUID id,
        @RequestParam String format
    ) {
        var report = analyticsService.getReport(id);
        byte[] bytes = exportService.exportReport(report, format);

        String ext = "PDF".equalsIgnoreCase(format) ? "pdf" : "csv";
        String contentType = "PDF".equalsIgnoreCase(format) ? MediaType.APPLICATION_PDF_VALUE : "text/csv";
        String filename = "disaster-report-" + report.getEventId() + "." + ext;

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .header(HttpHeaders.CONTENT_TYPE, contentType)
            .body(bytes);
    }
}
