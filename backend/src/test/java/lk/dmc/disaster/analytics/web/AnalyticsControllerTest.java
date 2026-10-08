package lk.dmc.disaster.analytics.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import lk.dmc.disaster.analytics.application.AnalyticsService;
import lk.dmc.disaster.analytics.domain.DisasterReport;
import lk.dmc.disaster.analytics.domain.EventSummary;
import lk.dmc.disaster.analytics.domain.ReportContext;
import lk.dmc.disaster.analytics.export.ExportService;
import lk.dmc.disaster.shared.actor.ActingUser;
import lk.dmc.disaster.shared.actor.ActingUserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import lk.dmc.disaster.support.TestIds;

@ExtendWith(MockitoExtension.class)
class AnalyticsControllerTest {

    private MockMvc mvc;

    @Mock AnalyticsService analyticsService;
    @Mock ExportService exportService;
    @Mock AnalyticsMapper mapper;
    @Mock ActingUserContext actingUserContext;

    @BeforeEach
    void setUp() {
        AnalyticsController controller = new AnalyticsController(analyticsService, exportService, mapper, actingUserContext);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void getEvents() throws Exception {
        when(analyticsService.listAvailableEvents("ACTIVE")).thenReturn(List.of(
            new EventSummary(TestIds.event(1), "Event 1", TestIds.hazardType(1), "ACTIVE", Instant.now(), null, new java.util.UUID[0], 5, 2)
        ));

        mvc.perform(get("/api/analytics/events").param("status", "ACTIVE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Event 1"))
            .andExpect(jsonPath("$[0].warningCount").value(5));
    }

    @Test
    void generateReport() throws Exception {
        var user = mock(ActingUser.class);
        when(user.id()).thenReturn(TestIds.user(1));
        when(actingUserContext.require()).thenReturn(user);

        var report = new DisasterReport(TestIds.event(1), Map.of(), Map.of(), List.of(), TestIds.user(1), Instant.now());
        var response = new DisasterReportResponse(java.util.UUID.fromString("f0000000-0000-0000-0000-000000000001"), TestIds.event(1), "Event 1", Map.of(), Map.of(), List.of(), "User 1", Instant.now());
        
        when(mapper.toContext(any(), eq(TestIds.user(1)))).thenReturn(new ReportContext(TestIds.event(1), null, null, null, TestIds.user(1)));
        when(analyticsService.generateReport(any())).thenReturn(report);
        when(mapper.toResponse(report)).thenReturn(response);

        mvc.perform(post("/api/analytics/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"eventId\":\"" + TestIds.event(1) + "\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.eventName").value("Event 1"));
    }

    @Test
    void getReports() throws Exception {
        var report = new DisasterReport(TestIds.event(1), Map.of(), Map.of(), List.of(), TestIds.user(1), Instant.now());
        var summary = new ReportSummaryResponse(java.util.UUID.fromString("f0000000-0000-0000-0000-000000000001"), TestIds.event(1), "Event 1", Instant.now(), "User 1", 0);
        
        when(analyticsService.listSavedReports(TestIds.event(1))).thenReturn(List.of(report));
        when(mapper.toSummaryResponse(report)).thenReturn(summary);

        mvc.perform(get("/api/analytics/reports").param("eventId", TestIds.event(1).toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].eventName").value("Event 1"));
    }

    @Test
    void getReports_outOfBounds_returnsEmptyPage() throws Exception {
        var report = new DisasterReport(TestIds.event(1), Map.of(), Map.of(), List.of(), TestIds.user(1), Instant.now());
        var summary = new ReportSummaryResponse(java.util.UUID.fromString("f0000000-0000-0000-0000-000000000001"), TestIds.event(1), "Event 1", Instant.now(), "User 1", 0);
        
        when(analyticsService.listSavedReports(null)).thenReturn(List.of(report));
        when(mapper.toSummaryResponse(report)).thenReturn(summary);

        // page=1 with size=20 means offset=20. List size is 1. Start (20) > list.size (1).
        mvc.perform(get("/api/analytics/reports").param("page", "1").param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void getReport() throws Exception {
        var report = new DisasterReport(TestIds.event(1), Map.of(), Map.of(), List.of(), TestIds.user(1), Instant.now());
        var response = new DisasterReportResponse(java.util.UUID.fromString("f0000000-0000-0000-0000-000000000001"), TestIds.event(1), "Event 1", Map.of(), Map.of(), List.of(), "User 1", Instant.now());

        when(analyticsService.getReport(java.util.UUID.fromString("f0000000-0000-0000-0000-000000000001"))).thenReturn(report);
        when(mapper.toResponse(report)).thenReturn(response);

        mvc.perform(get("/api/analytics/reports/{id}", java.util.UUID.fromString("f0000000-0000-0000-0000-000000000001")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.eventName").value("Event 1"));
    }

    @Test
    void exportReport() throws Exception {
        var report = new DisasterReport(TestIds.event(1), Map.of(), Map.of(), List.of(), TestIds.user(1), Instant.now());
        when(analyticsService.getReport(java.util.UUID.fromString("f0000000-0000-0000-0000-000000000001"))).thenReturn(report);
        when(exportService.exportReport(report, "PDF")).thenReturn("PDF_BYTES".getBytes());

        mvc.perform(get("/api/analytics/reports/{id}/export", java.util.UUID.fromString("f0000000-0000-0000-0000-000000000001"))
                .param("format", "PDF"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"disaster-report-" + TestIds.event(1) + ".pdf\""))
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
            .andExpect(content().string("PDF_BYTES"));
    }

    @Test
    void exportReport_csv() throws Exception {
        var report = new DisasterReport(TestIds.event(1), Map.of(), Map.of(), List.of(), TestIds.user(1), Instant.now());
        when(analyticsService.getReport(java.util.UUID.fromString("f0000000-0000-0000-0000-000000000001"))).thenReturn(report);
        when(exportService.exportReport(report, "CSV")).thenReturn("CSV_BYTES".getBytes());

        mvc.perform(get("/api/analytics/reports/{id}/export", java.util.UUID.fromString("f0000000-0000-0000-0000-000000000001"))
                .param("format", "CSV"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"disaster-report-" + TestIds.event(1) + ".csv\""))
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/csv"))
            .andExpect(content().string("CSV_BYTES"));
    }
}
