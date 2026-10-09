package lk.dmc.disaster.reports.service;

import java.time.Clock;
import java.util.UUID;
import lk.dmc.disaster.reports.entity.HazardReport;
import lk.dmc.disaster.reports.entity.ReportPhoto;
import lk.dmc.disaster.reports.repository.HazardReportRepository;
import lk.dmc.disaster.reports.repository.ReportPhotoRepository;
import lk.dmc.disaster.shared.error.NotFoundException;
import lk.dmc.disaster.shared.storage.FileStorage;
import lk.dmc.disaster.shared.storage.StoredFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC02 alternative A4, the reporter's side: answers the officer's "more information" question,
 * optionally with a new photo that replaces the one sent before. The report goes back to the queue
 * as PENDING. The row is locked so a late answer cannot overwrite an officer's decision.
 */
@Slf4j
@Service
public class ReportReplyService {

  private static final String PHOTO_FOLDER = "reports";

  private final HazardReportRepository reports;
  private final ReportPhotoRepository photos;
  private final ReportQueryService queries;
  private final FileStorage fileStorage;
  private final Clock clock;

  public ReportReplyService(
      HazardReportRepository reports,
      ReportPhotoRepository photos,
      ReportQueryService queries,
      FileStorage fileStorage,
      Clock clock) {
    this.reports = reports;
    this.photos = photos;
    this.queries = queries;
    this.fileStorage = fileStorage;
    this.clock = clock;
  }

  /**
   * Records the answer of {@code reporterId} on the report; a given {@code photo} replaces the
   * report's current one.
   *
   * @throws NotFoundException (404) for an unknown report
   * @throws lk.dmc.disaster.shared.error.ForbiddenRoleException (403) when someone else reported it
   * @throws lk.dmc.disaster.shared.error.InvalidStateTransitionException (409) when no question is
   *     open
   * @throws lk.dmc.disaster.shared.error.BusinessRuleException (422) for a bad answer or photo
   */
  @Transactional
  public ReportDetailView reply(UUID reportId, UUID reporterId, String message, PhotoUpload photo) {
    HazardReport report =
        reports
            .findWithLockById(reportId)
            .orElseThrow(() -> new NotFoundException("Report not found."));
    report.reply(reporterId, message, clock);
    reports.save(report);
    if (photo != null) {
      replacePhoto(report, photo);
    }
    log.info("Report {} answered by its reporter {}", report.getId(), reporterId);
    return queries.viewOf(report, false);
  }

  private void replacePhoto(HazardReport report, PhotoUpload upload) {
    StoredFile stored = fileStorage.store(PHOTO_FOLDER, upload.contentType(), upload.content());
    ReportPhoto fresh;
    try {
      fresh =
          ReportPhoto.attach(
              report.getId(), stored.path(), stored.contentType(), (int) stored.sizeBytes());
    } catch (RuntimeException e) {
      fileStorage.delete(stored.path());
      throw e;
    }
    ReportPhoto old = photos.findByReportId(report.getId()).orElse(null);
    if (old != null) {
      photos.delete(old);
      photos.flush();
    }
    photos.save(fresh);
    if (old != null) {
      removeQuietly(old.getFilePath());
    }
  }

  /** An old file that cannot be removed is only wasted space; it must not undo the answer. */
  private void removeQuietly(String path) {
    try {
      fileStorage.delete(path);
    } catch (RuntimeException e) {
      log.warn("Could not remove the replaced photo {}", path, e);
    }
  }
}
