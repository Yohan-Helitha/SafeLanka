package lk.dmc.disaster.reports.controller;

import lk.dmc.disaster.reports.entity.HazardReport;
import lk.dmc.disaster.reports.service.DuplicateMatch;
import lk.dmc.disaster.reports.service.QueueItem;
import lk.dmc.disaster.reports.service.ReportDetailView;
import lk.dmc.disaster.reports.service.ReportWithPhoto;
import lk.dmc.disaster.reports.service.SubmissionResult;
import org.springframework.stereotype.Component;

/** Turns service results into the response records; entities never reach the JSON layer. */
@Component
class ReportMapper {

  ReportListItemResponse toListItem(ReportWithPhoto result) {
    return listItem(result.report(), result.photo() != null, false);
  }

  ReportListItemResponse toListItem(SubmissionResult result) {
    return listItem(result.report(), result.photo() != null, false);
  }

  ReportListItemResponse toListItem(QueueItem item) {
    return listItem(item.report(), item.hasPhoto(), item.possibleDuplicate());
  }

  ReportDetailResponse toDetail(ReportDetailView view) {
    HazardReport r = view.report();
    var reporter = view.reporter();
    return new ReportDetailResponse(
        r.getId(),
        r.getReferenceNo(),
        r.getHazardTypeId(),
        r.getCategory(),
        r.getDescription(),
        r.getDistrictId(),
        r.getStatus(),
        r.getCapturedAt(),
        r.getSyncedAt(),
        view.photo() != null,
        !view.duplicates().isEmpty(),
        r.getRejectionReason(),
        r.getReviewComment(),
        r.getLatitude(),
        r.getLongitude(),
        r.isManualLocation(),
        r.getManualLocationText(),
        view.photo() == null ? null : "/api/reports/" + r.getId() + "/photo",
        new ReportDetailResponse.Reporter(
            reporter.id(), reporter.fullName(), reporter.role().name()),
        view.reviewer() == null ? null : view.reviewer().fullName(),
        r.getReviewedAt(),
        view.duplicates().stream().map(ReportMapper::toDuplicate).toList(),
        r.getReporterReply(),
        r.getRepliedAt());
  }

  private static ReportDetailResponse.Duplicate toDuplicate(DuplicateMatch match) {
    return new ReportDetailResponse.Duplicate(
        match.report().getId(),
        match.report().getReferenceNo(),
        Math.round(match.distanceMetres()));
  }

  private static ReportListItemResponse listItem(
      HazardReport r, boolean hasPhoto, boolean hasDuplicates) {
    return new ReportListItemResponse(
        r.getId(),
        r.getReferenceNo(),
        r.getHazardTypeId(),
        r.getCategory(),
        r.getDescription(),
        r.getDistrictId(),
        r.getStatus(),
        r.getCapturedAt(),
        r.getSyncedAt(),
        hasPhoto,
        hasDuplicates,
        r.getRejectionReason(),
        r.getReviewComment());
  }
}
