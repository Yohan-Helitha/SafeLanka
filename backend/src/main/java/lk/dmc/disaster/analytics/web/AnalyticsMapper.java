package lk.dmc.disaster.analytics.web;

import lk.dmc.disaster.analytics.domain.DisasterReport;
import lk.dmc.disaster.analytics.domain.ReportContext;
import lk.dmc.disaster.shared.reference.ReferenceData;
import lk.dmc.disaster.shared.reference.UserDirectory;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class AnalyticsMapper {

    private final ReferenceData referenceData;
    private final UserDirectory userDirectory;

    public AnalyticsMapper(ReferenceData referenceData, UserDirectory userDirectory) {
        this.referenceData = referenceData;
        this.userDirectory = userDirectory;
    }

    public ReportContext toContext(GenerateReportRequest req, UUID userId) {
        return new ReportContext(req.eventId(), req.districtIds(), req.from(), req.to(), userId);
    }

    public DisasterReportResponse toResponse(DisasterReport report) {
        return new DisasterReportResponse(
            report.getId(),
            report.getEventId(),
            referenceData.event(report.getEventId()).name(),
            report.getFilters(),
            report.getSections(),
            report.getUnavailableSections(),
            userDirectory.require(report.getGeneratedBy()).fullName(),
            report.getGeneratedAt()
        );
    }

    public ReportSummaryResponse toSummaryResponse(DisasterReport report) {
        return new ReportSummaryResponse(
            report.getId(),
            report.getEventId(),
            referenceData.event(report.getEventId()).name(),
            report.getGeneratedAt(),
            userDirectory.require(report.getGeneratedBy()).fullName(),
            report.getUnavailableSections() != null ? report.getUnavailableSections().size() : 0
        );
    }
}
