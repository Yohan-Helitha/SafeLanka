package lk.dmc.disaster.analytics.application;

import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.analytics.domain.DisasterReport;
import lk.dmc.disaster.analytics.domain.EventSummary;
import lk.dmc.disaster.analytics.domain.ReportContext;

public interface AnalyticsService {
    List<EventSummary> listAvailableEvents(String status);
    DisasterReport generateReport(ReportContext context);
    List<DisasterReport> listSavedReports(UUID eventId);
    DisasterReport getReport(UUID reportId);
}
