package lk.dmc.disaster.analytics.mapper;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.UUID;
import lk.dmc.disaster.analytics.dto.DisasterReportResponse;
import lk.dmc.disaster.analytics.dto.GenerateReportRequest;
import lk.dmc.disaster.analytics.dto.ReportSummaryResponse;
import lk.dmc.disaster.analytics.entity.DisasterReport;
import lk.dmc.disaster.analytics.entity.ReportContext;
import lk.dmc.disaster.analytics.entity.SectionKey;
import lk.dmc.disaster.shared.reference.ReferenceData;
import lk.dmc.disaster.shared.reference.UserDirectory;
import lk.dmc.disaster.shared.reference.UserView;
import org.springframework.stereotype.Component;

/** Turns requests into a report context and saved reports into response records. */
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
    UserView author = userDirectory.require(report.getGeneratedBy());
    return new DisasterReportResponse(
        report.getId(),
        report.getEventId(),
        referenceData.event(report.getEventId()).name(),
        filtersOf(report.getFilters()),
        sectionsOf(report.getSections()),
        report.getUnavailableSections(),
        new DisasterReportResponse.GeneratedBy(author.id(), author.fullName()),
        report.getGeneratedAt());
  }

  public ReportSummaryResponse toSummaryResponse(DisasterReport report) {
    return new ReportSummaryResponse(
        report.getId(),
        report.getEventId(),
        referenceData.event(report.getEventId()).name(),
        report.getGeneratedAt(),
        userDirectory.require(report.getGeneratedBy()).fullName(),
        report.getUnavailableSections() != null ? report.getUnavailableSections().size() : 0);
  }

  /** Always {@code districtIds}, {@code from} and {@code to}; reports saved earlier used other names. */
  private static Map<String, Object> filtersOf(Map<String, Object> saved) {
    Map<String, Object> filters = new LinkedHashMap<>();
    filters.put("districtIds", saved.getOrDefault("districtIds", List.of()));
    filters.put("from", saved.getOrDefault("from", saved.get("fromTime")));
    filters.put("to", saved.getOrDefault("to", saved.get("toTime")));
    return filters;
  }

  /** Sections in report order under their camelCase keys, whichever spelling they were saved with. */
  private static Map<String, Object> sectionsOf(Map<String, Object> saved) {
    Map<String, Object> sections = new LinkedHashMap<>();
    for (SectionKey key : SectionKey.values()) {
      Object data = saved.getOrDefault(key.name(), saved.get(key.apiName()));
      if (data != null) {
        sections.put(key.apiName(), data);
      }
    }
    return sections;
  }
}
