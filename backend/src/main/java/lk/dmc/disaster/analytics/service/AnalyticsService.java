package lk.dmc.disaster.analytics.service;

import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.analytics.entity.DisasterReport;
import lk.dmc.disaster.analytics.entity.EventSummary;
import lk.dmc.disaster.analytics.entity.ReportContext;

/** Use cases of the analytics module: list events, generate, read and list disaster reports. */
public interface AnalyticsService {
    List<EventSummary> listAvailableEvents(String status);
    DisasterReport generateReport(ReportContext context);
    List<DisasterReport> listSavedReports(UUID eventId);
    DisasterReport getReport(UUID reportId);
}
