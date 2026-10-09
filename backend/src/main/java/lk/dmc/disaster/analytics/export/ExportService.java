package lk.dmc.disaster.analytics.export;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import lk.dmc.disaster.analytics.entity.AnalyticsRules;
import lk.dmc.disaster.analytics.entity.DisasterReport;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import lk.dmc.disaster.shared.reference.ReferenceData;
import lk.dmc.disaster.shared.reference.UserDirectory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Picks the exporter for a format and names the file it produces. */
@Service
public class ExportService {

  private final List<ReportExporter> exporters;
  private final ReferenceData referenceData;
  private final UserDirectory userDirectory;

  public ExportService(
      List<ReportExporter> exporters, ReferenceData referenceData, UserDirectory userDirectory) {
    this.exporters = exporters;
    this.referenceData = referenceData;
    this.userDirectory = userDirectory;
  }

  /**
   * Exports a saved report.
   *
   * @param format {@code PDF} or {@code CSV}, in any case
   * @throws AppException (400) for any other format
   */
  @Transactional(readOnly = true)
  public ExportedFile export(DisasterReport report, String format) {
    ExportFormat chosen = ExportFormat.parse(format);
    ReportExporter exporter =
        exporters.stream()
            .filter(e -> e.format() == chosen)
            .findFirst()
            .orElseThrow(
                () -> new AppException(ErrorCode.VALIDATION_ERROR, "No exporter for " + chosen));
    String eventName = referenceData.event(report.getEventId()).name();
    DisasterReportView view =
        new DisasterReportView(
            report.getId(),
            report.getEventId(),
            eventName,
            report.getFilters(),
            report.getSections(),
            report.getUnavailableSections(),
            userDirectory.require(report.getGeneratedBy()).fullName(),
            report.getGeneratedAt());
    return new ExportedFile(
        fileName(eventName, report.getGeneratedAt(), chosen),
        chosen.contentType(),
        exporter.export(view));
  }

  /** For example {@code disaster-report-kalu-flood-may-2026-20261004.pdf}. */
  static String fileName(String eventName, Instant generatedAt, ExportFormat format) {
    String slug =
        eventName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
    String date = AnalyticsRules.EXPORT_FILE_DATE.format(generatedAt.atOffset(ZoneOffset.UTC).toLocalDate());
    return AnalyticsRules.EXPORT_FILE_PREFIX + slug + "-" + date + "." + format.extension();
  }
}
