package lk.dmc.disaster.analytics.export;

/** Turns a report into a file (Strategy); a new format is one new implementation. */
public interface ReportExporter {

  /** The format this exporter produces. */
  ExportFormat format();

  byte[] export(DisasterReportView report);
}
