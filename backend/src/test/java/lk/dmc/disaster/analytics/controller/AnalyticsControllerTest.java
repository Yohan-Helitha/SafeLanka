package lk.dmc.disaster.analytics.controller;

import static lk.dmc.disaster.analytics.AnalyticsFixtures.AUTHOR_ID;
import static lk.dmc.disaster.analytics.AnalyticsFixtures.EVENT_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lk.dmc.disaster.analytics.AnalyticsFixtures;
import lk.dmc.disaster.analytics.dto.DisasterReportResponse;
import lk.dmc.disaster.analytics.dto.ReportSummaryResponse;
import lk.dmc.disaster.analytics.entity.DisasterReport;
import lk.dmc.disaster.analytics.entity.EventSummary;
import lk.dmc.disaster.analytics.entity.ReportContext;
import lk.dmc.disaster.analytics.export.ExportService;
import lk.dmc.disaster.analytics.export.ExportedFile;
import lk.dmc.disaster.analytics.mapper.AnalyticsMapper;
import lk.dmc.disaster.analytics.service.AnalyticsService;
import lk.dmc.disaster.shared.actor.ActingUser;
import lk.dmc.disaster.shared.actor.ActingUserContext;
import lk.dmc.disaster.shared.domain.Role;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.BusinessRuleException;
import lk.dmc.disaster.shared.error.ErrorCode;
import lk.dmc.disaster.shared.error.GlobalExceptionHandler;
import lk.dmc.disaster.shared.error.NotFoundException;
import lk.dmc.disaster.support.TestIds;
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

/**
 * The web layer alone: status codes, the {@code {data, meta}} envelope and the error envelope. Who
 * may call what is covered end to end in {@code OfficerAccessTest}.
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsControllerTest {

  private static final UUID REPORT_ID = UUID.fromString("f0000000-0000-0000-0000-000000000001");

  @Mock AnalyticsService analyticsService;
  @Mock ExportService exportService;
  @Mock AnalyticsMapper mapper;
  @Mock ActingUserContext actingUserContext;

  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    var controller = new AnalyticsController(analyticsService, exportService, mapper, actingUserContext);
    mvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  private static DisasterReportResponse response() {
    return new DisasterReportResponse(
        REPORT_ID, EVENT_ID, "Event 1", Map.of(), Map.of(), List.of(),
        new DisasterReportResponse.GeneratedBy(AUTHOR_ID, "Nimal Perera"), Instant.now());
  }

  private static ReportSummaryResponse summary() {
    return new ReportSummaryResponse(REPORT_ID, EVENT_ID, "Event 1", Instant.now(), "Nimal Perera", 0);
  }

  // ---- events ------------------------------------------------------------------------------

  @Test
  void events_areListedUnderBothPaths() throws Exception {
    when(analyticsService.listAvailableEvents("ACTIVE"))
        .thenReturn(
            List.of(
                new EventSummary(
                    EVENT_ID, "Event 1", TestIds.hazardType(1), "ACTIVE", Instant.now(), null, new UUID[0], 5, 2)));

    for (String path : new String[] {"/api/analytics/events", "/api/analytics/disaster-events"}) {
      mvc.perform(get(path).param("status", "ACTIVE"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data[0].name").value("Event 1"))
          .andExpect(jsonPath("$.data[0].warningCount").value(5))
          .andExpect(jsonPath("$.data[0].reportCount").value(2));
    }
  }

  // ---- generate ----------------------------------------------------------------------------

  @Test
  void generate_is201WithTheSavedReport() throws Exception {
    var author = new ActingUser(AUTHOR_ID, Role.DMC_OFFICER, null, null, null);
    var report = AnalyticsFixtures.emptyReport();
    when(actingUserContext.require()).thenReturn(author);
    when(mapper.toContext(any(), eq(AUTHOR_ID)))
        .thenReturn(new ReportContext(EVENT_ID, null, null, null, AUTHOR_ID));
    when(analyticsService.generateReport(any())).thenReturn(report);
    when(mapper.toResponse(report)).thenReturn(response());

    mvc.perform(
            post("/api/analytics/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"eventId\":\"" + EVENT_ID + "\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.eventName").value("Event 1"))
        .andExpect(jsonPath("$.data.generatedBy.fullName").value("Nimal Perera"));
  }

  @Test
  void generate_withoutAnEventId_is400NamingTheField() throws Exception {
    mvc.perform(post("/api/analytics/reports").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.error.details.fields.eventId").exists());

    verifyNoInteractions(analyticsService);
  }

  @Test
  void generate_unknownEvent_is404() throws Exception {
    when(actingUserContext.require()).thenReturn(new ActingUser(AUTHOR_ID, Role.DMC_OFFICER, null, null, null));
    when(mapper.toContext(any(), any())).thenReturn(new ReportContext(EVENT_ID, null, null, null, AUTHOR_ID));
    when(analyticsService.generateReport(any())).thenThrow(new NotFoundException("Event not found"));

    mvc.perform(
            post("/api/analytics/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"eventId\":\"" + EVENT_ID + "\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
  }

  @Test
  void generate_badFilters_is422() throws Exception {
    when(actingUserContext.require()).thenReturn(new ActingUser(AUTHOR_ID, Role.DMC_OFFICER, null, null, null));
    when(mapper.toContext(any(), any())).thenReturn(new ReportContext(EVENT_ID, null, null, null, AUTHOR_ID));
    when(analyticsService.generateReport(any()))
        .thenThrow(new BusinessRuleException("The time window must lie inside the event."));

    mvc.perform(
            post("/api/analytics/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"eventId\":\"" + EVENT_ID + "\"}"))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.error.code").value("BUSINESS_RULE"))
        .andExpect(jsonPath("$.error.message").value("The time window must lie inside the event."));
  }

  // ---- saved reports -----------------------------------------------------------------------

  @Test
  void savedReports_arePagedInTheEnvelope() throws Exception {
    var report = AnalyticsFixtures.emptyReport();
    when(analyticsService.listSavedReports(EVENT_ID)).thenReturn(List.of(report));
    when(mapper.toSummaryResponse(report)).thenReturn(summary());

    mvc.perform(get("/api/analytics/reports").param("eventId", EVENT_ID.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].eventName").value("Event 1"))
        .andExpect(jsonPath("$.meta.totalElements").value(1))
        .andExpect(jsonPath("$.meta.page").value(0));
  }

  @Test
  void savedReports_aPageBeyondTheEndIsEmptyButKeepsTheTotal() throws Exception {
    var report = AnalyticsFixtures.emptyReport();
    when(analyticsService.listSavedReports(null)).thenReturn(List.of(report));
    when(mapper.toSummaryResponse(report)).thenReturn(summary());

    mvc.perform(get("/api/analytics/reports").param("page", "1").param("size", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isEmpty())
        .andExpect(jsonPath("$.meta.totalElements").value(1));
  }

  @Test
  void savedReports_pageSizeCutsTheList() throws Exception {
    var report = AnalyticsFixtures.emptyReport();
    when(analyticsService.listSavedReports(null)).thenReturn(List.of(report, report, report));
    when(mapper.toSummaryResponse(report)).thenReturn(summary());

    mvc.perform(get("/api/analytics/reports").param("size", "2"))
        .andExpect(jsonPath("$.data.length()").value(2))
        .andExpect(jsonPath("$.meta.totalPages").value(2));
  }

  @Test
  void oneReport_isReturnedInTheEnvelope() throws Exception {
    var report = AnalyticsFixtures.emptyReport();
    when(analyticsService.getReport(REPORT_ID)).thenReturn(report);
    when(mapper.toResponse(report)).thenReturn(response());

    mvc.perform(get("/api/analytics/reports/{id}", REPORT_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(REPORT_ID.toString()))
        .andExpect(jsonPath("$.data.eventName").value("Event 1"));
  }

  @Test
  void oneReport_unknownId_is404() throws Exception {
    when(analyticsService.getReport(REPORT_ID)).thenThrow(new NotFoundException("Report not found."));

    mvc.perform(get("/api/analytics/reports/{id}", REPORT_ID))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
  }

  // ---- export ------------------------------------------------------------------------------

  @Test
  void export_pdf_isAnAttachmentWithTheServiceFileName() throws Exception {
    var report = AnalyticsFixtures.emptyReport();
    when(analyticsService.getReport(REPORT_ID)).thenReturn(report);
    when(exportService.export(report, "PDF"))
        .thenReturn(new ExportedFile("disaster-report-kalu-20261004.pdf", "application/pdf", "PDF_BYTES".getBytes()));

    mvc.perform(get("/api/analytics/reports/{id}/export", REPORT_ID).param("format", "PDF"))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"disaster-report-kalu-20261004.pdf\""))
        .andExpect(content().contentType(MediaType.APPLICATION_PDF))
        .andExpect(content().string("PDF_BYTES"));
  }

  @Test
  void export_csv_hasTheCsvContentType() throws Exception {
    var report = AnalyticsFixtures.emptyReport();
    when(analyticsService.getReport(REPORT_ID)).thenReturn(report);
    when(exportService.export(report, "CSV"))
        .thenReturn(new ExportedFile("disaster-report-kalu-20261004.csv", "text/csv", "CSV_BYTES".getBytes()));

    mvc.perform(get("/api/analytics/reports/{id}/export", REPORT_ID).param("format", "CSV"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("text/csv"))
        .andExpect(content().string("CSV_BYTES"));
  }

  @Test
  void export_unknownFormat_is400() throws Exception {
    var report = AnalyticsFixtures.emptyReport();
    when(analyticsService.getReport(REPORT_ID)).thenReturn(report);
    when(exportService.export(report, "EXCEL"))
        .thenThrow(new AppException(ErrorCode.VALIDATION_ERROR, "Unsupported export format. Use PDF or CSV."));

    mvc.perform(get("/api/analytics/reports/{id}/export", REPORT_ID).param("format", "EXCEL"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
  }

  @Test
  void export_withoutAFormat_is400() throws Exception {
    mvc.perform(get("/api/analytics/reports/{id}/export", REPORT_ID))
        .andExpect(status().isBadRequest());
  }

  @Test
  void export_unknownReport_is404() throws Exception {
    when(analyticsService.getReport(REPORT_ID)).thenThrow(new NotFoundException("Report not found."));

    mvc.perform(get("/api/analytics/reports/{id}/export", REPORT_ID).param("format", "PDF"))
        .andExpect(status().isNotFound());
  }
}
