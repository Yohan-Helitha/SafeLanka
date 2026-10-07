package lk.dmc.disaster.reports.service;

import lk.dmc.disaster.reports.entity.HazardReport;
import lk.dmc.disaster.reports.entity.ReportPhoto;

/** A report and its photo (null when none was sent), ready to be mapped to a response. */
public record ReportWithPhoto(HazardReport report, ReportPhoto photo) {}
