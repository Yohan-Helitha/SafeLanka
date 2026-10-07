package lk.dmc.disaster.reports.service;

import java.time.Clock;
import java.util.UUID;
import lk.dmc.disaster.reports.entity.HazardReport;
import lk.dmc.disaster.reports.entity.ReportDraft;
import lk.dmc.disaster.reports.entity.ReportPhoto;
import lk.dmc.disaster.reports.repository.HazardReportRepository;
import lk.dmc.disaster.reports.repository.ReferenceNumberGenerator;
import lk.dmc.disaster.reports.repository.ReportPhotoRepository;
import lk.dmc.disaster.shared.error.BusinessRuleException;
import lk.dmc.disaster.shared.error.ConflictException;
import lk.dmc.disaster.shared.reference.HazardTypeInfo;
import lk.dmc.disaster.shared.reference.ReferenceData;
import lk.dmc.disaster.shared.storage.FileStorage;
import lk.dmc.disaster.shared.storage.StoredFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** UC02 main flow, steps 1-4: a citizen or volunteer submits a ground report. */
@Slf4j
@Service
public class ReportSubmissionService {

  private static final String PHOTO_FOLDER = "reports";

  private final HazardReportRepository reports;
  private final ReportPhotoRepository photos;
  private final ReferenceNumberGenerator referenceNumbers;
  private final ReferenceData referenceData;
  private final FileStorage fileStorage;
  private final Clock clock;

  public ReportSubmissionService(
      HazardReportRepository reports,
      ReportPhotoRepository photos,
      ReferenceNumberGenerator referenceNumbers,
      ReferenceData referenceData,
      FileStorage fileStorage,
      Clock clock) {
    this.reports = reports;
    this.photos = photos;
    this.referenceNumbers = referenceNumbers;
    this.referenceData = referenceData;
    this.fileStorage = fileStorage;
    this.clock = clock;
  }

  /**
   * Saves a PENDING report. Sending the same {@code clientRef} again (offline sync) returns the
   * report already stored instead of creating a duplicate.
   *
   * @throws ConflictException (409) when the {@code clientRef} belongs to another reporter
   * @throws BusinessRuleException (422) for an unknown or inactive hazard type, a category the type
   *     does not accept, an unknown district, or data that breaks a report rule
   */
  @Transactional
  public SubmissionResult submit(UUID reporterId, SubmitReportCommand command) {
    var existing = reports.findByClientRef(command.clientRef());
    if (existing.isPresent()) {
      return replay(existing.get(), reporterId);
    }
    checkReferences(command);

    HazardReport report =
        HazardReport.submit(toDraft(command), reporterId, referenceNumbers.next(), clock);
    ReportPhoto photo = storePhoto(report, command.photo());
    reports.save(report);
    if (photo != null) {
      photos.save(photo);
    }
    log.info("Report {} submitted ({})", report.getId(), report.getReferenceNo());
    return new SubmissionResult(report, photo, true);
  }

  private SubmissionResult replay(HazardReport existing, UUID reporterId) {
    if (!existing.isReportedBy(reporterId)) {
      throw new ConflictException("This submission key was already used by another reporter.");
    }
    ReportPhoto photo = photos.findByReportId(existing.getId()).orElse(null);
    return new SubmissionResult(existing, photo, false);
  }

  private void checkReferences(SubmitReportCommand command) {
    HazardTypeInfo type =
        referenceData
            .hazardType(command.hazardTypeId())
            .orElseThrow(() -> new BusinessRuleException("Unknown hazard type."));
    if (!type.active()) {
      throw new BusinessRuleException(type.name() + " is not accepting reports right now.");
    }
    if (!type.categories().contains(command.category())) {
      throw new BusinessRuleException("That category is not available for " + type.name() + ".");
    }
    if (!referenceData.districtExists(command.districtId())) {
      throw new BusinessRuleException("Unknown district.");
    }
  }

  private ReportPhoto storePhoto(HazardReport report, PhotoUpload upload) {
    if (upload == null) {
      return null;
    }
    StoredFile stored = fileStorage.store(PHOTO_FOLDER, upload.contentType(), upload.content());
    return ReportPhoto.attach(
        report.getId(), stored.path(), stored.contentType(), (int) stored.sizeBytes());
  }

  private static ReportDraft toDraft(SubmitReportCommand c) {
    return new ReportDraft(
        c.clientRef(),
        c.hazardTypeId(),
        c.category(),
        c.description(),
        c.latitude(),
        c.longitude(),
        c.manualLocationText(),
        c.districtId(),
        c.capturedAt());
  }
}
