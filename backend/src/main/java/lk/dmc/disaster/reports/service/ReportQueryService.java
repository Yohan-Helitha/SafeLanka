package lk.dmc.disaster.reports.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lk.dmc.disaster.reports.entity.HazardReport;
import lk.dmc.disaster.reports.entity.ReportPhoto;
import lk.dmc.disaster.reports.entity.ReportRules;
import lk.dmc.disaster.reports.entity.ReportStatus;
import lk.dmc.disaster.reports.repository.HazardReportRepository;
import lk.dmc.disaster.reports.repository.ReportPhotoRepository;
import lk.dmc.disaster.reports.repository.ReportSpecifications;
import lk.dmc.disaster.shared.actor.ActingUser;
import lk.dmc.disaster.shared.actor.UserDirectory;
import lk.dmc.disaster.shared.actor.UserSummary;
import lk.dmc.disaster.shared.domain.Role;
import lk.dmc.disaster.shared.error.ForbiddenRoleException;
import lk.dmc.disaster.shared.error.NotFoundException;
import lk.dmc.disaster.shared.storage.FileStorage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read side of UC02: the reporter's own list, the officer queue, report detail and photo. */
@Service
public class ReportQueryService {

  private final HazardReportRepository reports;
  private final ReportPhotoRepository photos;
  private final DuplicateDetector duplicates;
  private final UserDirectory users;
  private final FileStorage fileStorage;

  public ReportQueryService(
      HazardReportRepository reports,
      ReportPhotoRepository photos,
      DuplicateDetector duplicates,
      UserDirectory users,
      FileStorage fileStorage) {
    this.reports = reports;
    this.photos = photos;
    this.duplicates = duplicates;
    this.users = users;
    this.fileStorage = fileStorage;
  }

  /** The reporter's own reports, newest first. */
  @Transactional(readOnly = true)
  public Page<ReportWithPhoto> mine(UUID reporterId, int page, int size) {
    Page<HazardReport> found =
        reports.findByReporterIdOrderByCapturedAtDesc(reporterId, pageable(page, size, Sort.unsorted()));
    Map<UUID, ReportPhoto> photoByReport = photosOf(found.getContent());
    return found.map(r -> new ReportWithPhoto(r, photoByReport.get(r.getId())));
  }

  /** The officer queue, oldest first. Every filter is optional (null means any). */
  @Transactional(readOnly = true)
  public Page<QueueItem> queue(
      ReportStatus status, UUID hazardTypeId, UUID districtId, int page, int size) {
    Page<HazardReport> found =
        reports.findAll(
            ReportSpecifications.queue(status, hazardTypeId, districtId),
            pageable(page, size, Sort.by(Sort.Direction.ASC, "capturedAt")));
    Map<UUID, ReportPhoto> photoByReport = photosOf(found.getContent());
    return found.map(
        r ->
            new QueueItem(
                r, photoByReport.containsKey(r.getId()), isOpenWithDuplicates(r)));
  }

  /**
   * One report with its reporter. Only DMC officers and the reporter may open it.
   *
   * @throws NotFoundException (404) when it does not exist
   * @throws ForbiddenRoleException (403) for anyone else
   */
  @Transactional(readOnly = true)
  public ReportDetailView detail(UUID reportId, ActingUser actor) {
    return viewOf(findAccessible(reportId, actor), actor.hasRole(Role.DMC_OFFICER));
  }

  /** Builds the detail view; duplicates are looked up only for officers. */
  ReportDetailView viewOf(HazardReport report, boolean forOfficer) {
    ReportPhoto photo = photos.findByReportId(report.getId()).orElse(null);
    List<DuplicateMatch> matches = forOfficer ? duplicates.findDuplicates(report) : List.of();
    UserSummary reviewer =
        report.getReviewedBy() == null ? null : users.find(report.getReviewedBy()).orElse(null);
    return new ReportDetailView(
        report, photo, users.require(report.getReporterId()), matches, reviewer);
  }

  /** The photo bytes, under the same access rule as {@link #detail}; 404 when there is none. */
  @Transactional(readOnly = true)
  public PhotoContent photo(UUID reportId, ActingUser actor) {
    findAccessible(reportId, actor);
    ReportPhoto photo =
        photos
            .findByReportId(reportId)
            .orElseThrow(() -> new NotFoundException("This report has no photo."));
    return new PhotoContent(fileStorage.load(photo.getFilePath()), photo.getMimeType());
  }

  private HazardReport findAccessible(UUID reportId, ActingUser actor) {
    HazardReport report =
        reports.findById(reportId).orElseThrow(() -> new NotFoundException("Report not found."));
    if (!actor.hasRole(Role.DMC_OFFICER) && !report.isReportedBy(actor.id())) {
      throw new ForbiddenRoleException("You cannot open this report.");
    }
    return report;
  }

  /** Duplicate hints matter only while a decision is still open. */
  private boolean isOpenWithDuplicates(HazardReport report) {
    return !report.getStatus().isFinal() && !duplicates.findDuplicates(report).isEmpty();
  }

  private Map<UUID, ReportPhoto> photosOf(Collection<HazardReport> found) {
    if (found.isEmpty()) {
      return Map.of();
    }
    List<UUID> ids = found.stream().map(HazardReport::getId).toList();
    return photos.findByReportIdIn(ids).stream()
        .collect(Collectors.toMap(ReportPhoto::getReportId, Function.identity(), (a, b) -> a));
  }

  private static Pageable pageable(int page, int size, Sort sort) {
    int safeSize = Math.min(Math.max(size, 1), ReportRules.MAX_PAGE_SIZE);
    return PageRequest.of(Math.max(page, 0), safeSize, sort);
  }
}
