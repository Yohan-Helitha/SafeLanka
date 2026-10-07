package lk.dmc.disaster.reports.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lk.dmc.disaster.reports.VerifiedReportFilter;
import lk.dmc.disaster.reports.VerifiedReportQuery;
import lk.dmc.disaster.reports.VerifiedReportSummary;
import lk.dmc.disaster.reports.entity.HazardReport;
import lk.dmc.disaster.reports.entity.ReportPhoto;
import lk.dmc.disaster.reports.entity.ReportRules;
import lk.dmc.disaster.reports.entity.ReportStatus;
import lk.dmc.disaster.reports.repository.HazardReportRepository;
import lk.dmc.disaster.reports.repository.ReportPhotoRepository;
import lk.dmc.disaster.reports.repository.ReportSpecifications;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The contract the warnings module uses; it never sees entities, only summaries. */
@Service
class VerifiedReportQueryImpl implements VerifiedReportQuery {

  private final HazardReportRepository reports;
  private final ReportPhotoRepository photos;

  VerifiedReportQueryImpl(HazardReportRepository reports, ReportPhotoRepository photos) {
    this.reports = reports;
    this.photos = photos;
  }

  @Override
  @Transactional(readOnly = true)
  public List<VerifiedReportSummary> findVerified(VerifiedReportFilter filter) {
    VerifiedReportFilter f = filter == null ? VerifiedReportFilter.any() : filter;
    List<HazardReport> found =
        reports
            .findAll(
                ReportSpecifications.verified(f.hazardTypeId(), f.districtId(), f.since()),
                PageRequest.of(
                    0,
                    ReportRules.VERIFIED_QUERY_LIMIT,
                    Sort.by(Sort.Direction.DESC, "reviewedAt")))
            .getContent();
    Set<UUID> withPhoto = idsWithPhoto(found);
    return found.stream().map(r -> summary(r, withPhoto.contains(r.getId()))).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<VerifiedReportSummary> findVerifiedById(UUID reportId) {
    return reports
        .findById(reportId)
        .filter(r -> r.getStatus() == ReportStatus.VERIFIED)
        .map(r -> summary(r, photos.findByReportId(r.getId()).isPresent()));
  }

  private Set<UUID> idsWithPhoto(List<HazardReport> found) {
    if (found.isEmpty()) {
      return Set.of();
    }
    List<UUID> ids = found.stream().map(HazardReport::getId).toList();
    return photos.findByReportIdIn(ids).stream()
        .map(ReportPhoto::getReportId)
        .collect(Collectors.toSet());
  }

  private static VerifiedReportSummary summary(HazardReport r, boolean hasPhoto) {
    return new VerifiedReportSummary(
        r.getId(),
        r.getReferenceNo(),
        r.getHazardTypeId(),
        r.getCategory(),
        r.getDescription(),
        r.getDistrictId(),
        r.getLatitude(),
        r.getLongitude(),
        r.isManualLocation(),
        hasPhoto ? "/api/reports/" + r.getId() + "/photo" : null,
        r.getCapturedAt(),
        r.getReviewedAt(),
        r.getReviewedBy());
  }
}
