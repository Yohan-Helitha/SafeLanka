package lk.dmc.disaster.reports.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lk.dmc.disaster.reports.entity.HazardReport;
import lk.dmc.disaster.reports.entity.ReportDraft;
import lk.dmc.disaster.reports.entity.ReportPhoto;
import lk.dmc.disaster.reports.entity.ReportStatus;
import lk.dmc.disaster.reports.repository.HazardReportRepository;
import lk.dmc.disaster.reports.repository.ReferenceNumberGenerator;
import lk.dmc.disaster.reports.repository.ReportPhotoRepository;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.BusinessRuleException;
import lk.dmc.disaster.shared.error.ConflictException;
import lk.dmc.disaster.shared.error.ErrorCode;
import lk.dmc.disaster.shared.reference.HazardTypeInfo;
import lk.dmc.disaster.shared.reference.ReferenceData;
import lk.dmc.disaster.shared.storage.FileStorage;
import lk.dmc.disaster.shared.storage.StoredFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class ReportSubmissionServiceTest {

  private static final Instant NOW = Instant.parse("2026-10-04T08:10:02Z");
  private static final UUID REPORTER = UUID.randomUUID();
  private static final UUID FLOOD = UUID.randomUUID();
  private static final UUID COLOMBO = UUID.randomUUID();
  private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1};

  @Mock HazardReportRepository reports;
  @Mock ReportPhotoRepository photos;
  @Mock ReferenceNumberGenerator referenceNumbers;
  @Mock ReferenceData referenceData;
  @Mock FileStorage fileStorage;
  @Mock PlatformTransactionManager transactionManager;

  private ReportSubmissionService service;

  @BeforeEach
  void setUp() {
    service =
        new ReportSubmissionService(
            reports,
            photos,
            referenceNumbers,
            referenceData,
            fileStorage,
            new TransactionTemplate(transactionManager),
            Clock.fixed(NOW, ZoneOffset.UTC));
    lenient().when(reports.findByClientRef(any())).thenReturn(Optional.empty());
    lenient().when(reports.save(any(HazardReport.class))).thenAnswer(i -> i.getArgument(0));
    lenient().when(referenceNumbers.next()).thenReturn("RPT-2026-0013");
    lenient().when(referenceData.districtExists(COLOMBO)).thenReturn(true);
    givenHazardType(true);
  }

  private void givenHazardType(boolean active) {
    lenient()
        .when(referenceData.hazardType(FLOOD))
        .thenReturn(
            Optional.of(
                new HazardTypeInfo(
                    FLOOD, "FLOOD", "Flood", active, List.of("RISING_WATER", "OTHER"))));
  }

  private static SubmitReportCommand command(
      UUID clientRef, Instant capturedAt, PhotoUpload photo) {
    return new SubmitReportCommand(
        clientRef,
        FLOOD,
        "RISING_WATER",
        "Water over the road near Kolonnawa canal bridge",
        6.9391,
        79.8921,
        null,
        COLOMBO,
        capturedAt,
        photo);
  }

  private static SubmitReportCommand command() {
    return command(UUID.randomUUID(), NOW.minusSeconds(60), null);
  }

  private void assertNothingStored() {
    verify(reports, never()).save(any());
    verify(photos, never()).save(any());
    verifyNoInteractions(fileStorage);
  }

  // ---- main flow ----------------------------------------------------------------------------

  @Test
  void submit_savesPendingReportWithReferenceNumber() {
    SubmitReportCommand command = command();

    SubmissionResult result = service.submit(REPORTER, command);

    ArgumentCaptor<HazardReport> saved = ArgumentCaptor.forClass(HazardReport.class);
    verify(reports).save(saved.capture());
    HazardReport report = saved.getValue();
    assertThat(result.created()).isTrue();
    assertThat(result.report()).isSameAs(report);
    assertThat(report.getStatus()).isEqualTo(ReportStatus.PENDING);
    assertThat(report.getReferenceNo()).isEqualTo("RPT-2026-0013");
    assertThat(report.getReporterId()).isEqualTo(REPORTER);
    assertThat(report.getClientRef()).isEqualTo(command.clientRef());
    assertThat(report.getSyncedAt()).isEqualTo(NOW);
    assertThat(result.photo()).isNull();
    verify(photos, never()).save(any());
    verifyNoInteractions(fileStorage);
  }

  @Test
  void submit_storesPhotoAndLinksItToTheReport() {
    when(fileStorage.store("reports", "image/jpeg", JPEG))
        .thenReturn(new StoredFile("reports/abc.jpg", "image/jpeg", JPEG.length));

    SubmissionResult result =
        service.submit(
            REPORTER,
            command(UUID.randomUUID(), NOW.minusSeconds(60), new PhotoUpload("image/jpeg", JPEG)));

    ArgumentCaptor<ReportPhoto> saved = ArgumentCaptor.forClass(ReportPhoto.class);
    verify(photos).save(saved.capture());
    assertThat(saved.getValue().getReportId()).isEqualTo(result.report().getId());
    assertThat(saved.getValue().getFilePath()).isEqualTo("reports/abc.jpg");
    assertThat(saved.getValue().getMimeType()).isEqualTo("image/jpeg");
    assertThat(saved.getValue().getSizeBytes()).isEqualTo(JPEG.length);
    assertThat(result.photo()).isSameAs(saved.getValue());
  }

  @Test
  void submit_withoutGpsAcceptsAPlaceDescription() {
    SubmitReportCommand manual =
        new SubmitReportCommand(
            UUID.randomUUID(),
            FLOOD,
            "OTHER",
            "Water over the road near the canal bridge",
            null,
            null,
            "Next to the old railway bridge",
            COLOMBO,
            NOW,
            null);

    SubmissionResult result = service.submit(REPORTER, manual);

    assertThat(result.report().isManualLocation()).isTrue();
    assertThat(result.report().getLatitude()).isNull();
  }

  // ---- idempotent offline sync --------------------------------------------------------------

  @Test
  void submit_sameClientRefFromSameReporterReturnsExistingWithoutSaving() {
    SubmitReportCommand command = command();
    HazardReport existing =
        HazardReport.submit(
            new ReportDraft(
                command.clientRef(),
                FLOOD,
                "RISING_WATER",
                command.description(),
                6.9391,
                79.8921,
                null,
                COLOMBO,
                NOW),
            REPORTER,
            "RPT-2026-0001",
            Clock.fixed(NOW, ZoneOffset.UTC));
    ReportPhoto photo = ReportPhoto.attach(existing.getId(), "reports/x.jpg", "image/jpeg", 10);
    when(reports.findByClientRef(command.clientRef())).thenReturn(Optional.of(existing));
    when(photos.findByReportId(existing.getId())).thenReturn(Optional.of(photo));

    SubmissionResult result = service.submit(REPORTER, command);

    assertThat(result.created()).isFalse();
    assertThat(result.report()).isSameAs(existing);
    assertThat(result.photo()).isSameAs(photo);
    assertNothingStored();
    verifyNoInteractions(referenceNumbers);
  }

  @Test
  void submit_clientRefOfAnotherReporterIsAConflict() {
    SubmitReportCommand command = command();
    HazardReport someoneElses =
        HazardReport.submit(
            new ReportDraft(
                command.clientRef(),
                FLOOD,
                "RISING_WATER",
                command.description(),
                6.9391,
                79.8921,
                null,
                COLOMBO,
                NOW),
            UUID.randomUUID(),
            "RPT-2026-0002",
            Clock.fixed(NOW, ZoneOffset.UTC));
    when(reports.findByClientRef(command.clientRef())).thenReturn(Optional.of(someoneElses));

    assertThatThrownBy(() -> service.submit(REPORTER, command))
        .isInstanceOfSatisfying(
            ConflictException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.CONFLICT));
    assertNothingStored();
  }

  // ---- reference data checks (422) ----------------------------------------------------------

  @Test
  void submit_unknownHazardTypeIsRejected() {
    when(referenceData.hazardType(FLOOD)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.submit(REPORTER, command()))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("hazard type");
    assertNothingStored();
  }

  @Test
  void submit_inactiveHazardTypeIsRejected() {
    givenHazardType(false);

    assertThatThrownBy(() -> service.submit(REPORTER, command()))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("not accepting");
    assertNothingStored();
  }

  @Test
  void submit_categoryNotAcceptedByTheTypeIsRejected() {
    SubmitReportCommand wrongCategory =
        new SubmitReportCommand(
            UUID.randomUUID(),
            FLOOD,
            "LANDSLIDE_CRACK",
            "Water over the road near the canal bridge",
            6.9,
            79.9,
            null,
            COLOMBO,
            NOW,
            null);

    assertThatThrownBy(() -> service.submit(REPORTER, wrongCategory))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("category");
    assertNothingStored();
  }

  @Test
  void submit_unknownDistrictIsRejected() {
    when(referenceData.districtExists(COLOMBO)).thenReturn(false);

    assertThatThrownBy(() -> service.submit(REPORTER, command()))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("district");
    assertNothingStored();
  }

  // ---- clock skew ---------------------------------------------------------------------------

  @Test
  void submit_captureTimeFourMinutesAheadIsAccepted() {
    SubmissionResult result =
        service.submit(
            REPORTER, command(UUID.randomUUID(), NOW.plus(Duration.ofMinutes(4)), null));

    assertThat(result.created()).isTrue();
  }

  @Test
  void submit_captureTimeSixMinutesAheadIsRejectedAndNothingIsStored() {
    SubmitReportCommand future =
        command(
            UUID.randomUUID(),
            NOW.plus(Duration.ofMinutes(6)),
            new PhotoUpload("image/jpeg", JPEG));

    assertThatThrownBy(() -> service.submit(REPORTER, future))
        .isInstanceOf(BusinessRuleException.class);
    assertNothingStored();
  }

  // ---- failures while storing ---------------------------------------------------------------

  @Test
  void submit_whenTheStorageRejectsThePhotoNothingIsSaved() {
    when(fileStorage.store(anyString(), anyString(), any()))
        .thenThrow(new AppException(ErrorCode.VALIDATION_ERROR, "The photo must be at most 5 MB."));

    assertThatThrownBy(
            () ->
                service.submit(
                    REPORTER,
                    command(UUID.randomUUID(), NOW, new PhotoUpload("image/jpeg", JPEG))))
        .isInstanceOfSatisfying(
            AppException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    verify(reports, never()).save(any());
    verify(photos, never()).save(any());
  }

  // ---- two requests with the same clientRef at the same moment -------------------------------

  private HazardReport reportOf(SubmitReportCommand c, UUID reporter) {
    return HazardReport.submit(
        new ReportDraft(
            c.clientRef(),
            FLOOD,
            "RISING_WATER",
            c.description(),
            6.9391,
            79.8921,
            null,
            COLOMBO,
            NOW),
        reporter,
        "RPT-2026-0001",
        Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private void givenStoredPhoto() {
    when(fileStorage.store("reports", "image/jpeg", JPEG))
        .thenReturn(new StoredFile("reports/abc.jpg", "image/jpeg", JPEG.length));
  }

  private static SubmitReportCommand withPhoto() {
    return command(UUID.randomUUID(), NOW.minusSeconds(60), new PhotoUpload("image/jpeg", JPEG));
  }

  @Test
  void submit_losingTheRaceReturnsTheWinnerAsAReplayAndRemovesItsOwnPhoto() {
    SubmitReportCommand command = withPhoto();
    HazardReport winner = reportOf(command, REPORTER);
    givenStoredPhoto();
    when(reports.findByClientRef(command.clientRef()))
        .thenReturn(Optional.empty(), Optional.of(winner));
    when(reports.save(any(HazardReport.class))).thenThrow(new DataIntegrityViolationException("dup"));

    SubmissionResult result = service.submit(REPORTER, command);

    assertThat(result.created()).isFalse();
    assertThat(result.report()).isSameAs(winner);
    verify(fileStorage).delete("reports/abc.jpg");
  }

  @Test
  void submit_losingTheRaceToAnotherReportersKeyIsAConflict() {
    SubmitReportCommand command = command();
    HazardReport winner = reportOf(command, UUID.randomUUID());
    when(reports.findByClientRef(command.clientRef()))
        .thenReturn(Optional.empty(), Optional.of(winner));
    when(reports.save(any(HazardReport.class))).thenThrow(new DataIntegrityViolationException("dup"));

    assertThatThrownBy(() -> service.submit(REPORTER, command))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void submit_integrityViolationWithoutAWinnerIsRethrownAndThePhotoRemoved() {
    SubmitReportCommand command = withPhoto();
    givenStoredPhoto();
    DataIntegrityViolationException failure = new DataIntegrityViolationException("other");
    when(reports.save(any(HazardReport.class))).thenThrow(failure);

    assertThatThrownBy(() -> service.submit(REPORTER, command)).isSameAs(failure);
    verify(fileStorage).delete("reports/abc.jpg");
  }

  @Test
  void submit_anyOtherFailureWhileSavingRemovesThePhotoAndPropagates() {
    SubmitReportCommand command = withPhoto();
    givenStoredPhoto();
    when(photos.save(any(ReportPhoto.class))).thenThrow(new IllegalStateException("db down"));

    assertThatThrownBy(() -> service.submit(REPORTER, command))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("db down");
    verify(fileStorage).delete("reports/abc.jpg");
  }

  @Test
  void submit_failureWithoutAPhotoNeverTouchesStorage() {
    when(reports.save(any(HazardReport.class))).thenThrow(new IllegalStateException("db down"));

    assertThatThrownBy(() -> service.submit(REPORTER, command()))
        .isInstanceOf(IllegalStateException.class);
    verifyNoInteractions(fileStorage);
  }
}
