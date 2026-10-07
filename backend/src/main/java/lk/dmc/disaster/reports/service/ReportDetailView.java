package lk.dmc.disaster.reports.service;

import java.util.List;
import lk.dmc.disaster.reports.entity.HazardReport;
import lk.dmc.disaster.reports.entity.ReportPhoto;
import lk.dmc.disaster.shared.actor.UserSummary;

/**
 * A report with its reporter. {@code duplicates} is filled for officers only, so a citizen never
 * sees other people's reports.
 */
public record ReportDetailView(
    HazardReport report, ReportPhoto photo, UserSummary reporter, List<DuplicateMatch> duplicates) {}
