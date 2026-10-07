package lk.dmc.disaster.reports.service;

import lk.dmc.disaster.reports.entity.HazardReport;
import lk.dmc.disaster.reports.entity.ReportPhoto;

/**
 * Outcome of a submission.
 *
 * @param report the stored report
 * @param photo its photo, or null when none was sent
 * @param created false when the same offline key had already been synced (HTTP 200, not 201)
 */
public record SubmissionResult(HazardReport report, ReportPhoto photo, boolean created) {}
