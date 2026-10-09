package lk.dmc.disaster.reports.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lk.dmc.disaster.reports.entity.HazardReport;
import lk.dmc.disaster.reports.entity.ReportPhoto;
import lk.dmc.disaster.reports.entity.ReportStatus;
import lk.dmc.disaster.reports.repository.HazardReportRepository;
import lk.dmc.disaster.reports.repository.ReportPhotoRepository;
import lk.dmc.disaster.shared.error.BusinessRuleException;
import lk.dmc.disaster.shared.error.ForbiddenRoleException;
import lk.dmc.disaster.shared.error.InvalidStateTransitionException;
import lk.dmc.disaster.shared.error.NotFoundException;
import lk.dmc.disaster.shared.storage.FileStorage;
import lk.dmc.disaster.shared.storage.StoredFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportReplyServiceTest {

  private static final Instant NOW = Instant.parse("2026-10-04T09:00:00Z");
  private static final UUID REPORTER = UUID.randomUUID();
  private static final UUID OFFICER = UUID.randomUUID();
  private static final PhotoUpload PHOTO = new PhotoUpload("image/jpeg", new byte[] {1, 2, 3});

  @Mock HazardReportRepository reports;
  @Mock ReportPhotoRepository photos;
  @Mock ReportQueryService queries;
  @Mock FileStorage storage;

  private ReportReplyService service;
  private HazardReport report;
  private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
  private final ReportDetailView view = new ReportDetailView(null, null, null, List.of(), null);

  @BeforeEach
  void setUp() {
    service = new ReportReplyService(reports, photos, queries, storage, clock);
    report = ReportFixtures.gps(REPORTER, 6.9391, 79.8921, NOW.minusSeconds(3600));
  }

  private void openQuestion() {
    report.requestInfo(OFFICER, "Which side of the bridge?", clock);
    when(reports.findWithLockById(report.getId())).thenReturn(Optional.of(report));
  }

  @Test
  void reply_savesTheAnswerAndReturnsTheCitizensView() {
    openQuestion();
    when(queries.viewOf(report, false)).thenReturn(view);

    ReportDetailView result = service.reply(report.getId(), REPORTER, "The north side", null);

    assertThat(result).isSameAs(view);
    assertThat(report.getStatus()).isEqualTo(ReportStatus.PENDING);
    assertThat(report.getReporterReply()).isEqualTo("The north side");
    verify(reports).save(report);
    verify(storage, never()).store(anyString(), anyString(), any());
  }

  @Test
  void reply_withAPhotoStoresItAndReplacesTheOldOne() {
    openQuestion();
    ReportPhoto old = ReportPhoto.attach(report.getId(), "reports/old.jpg", "image/jpeg", 10);
    when(photos.findByReportId(report.getId())).thenReturn(Optional.of(old));
    when(storage.store("reports", "image/jpeg", PHOTO.content()))
        .thenReturn(new StoredFile("reports/new.jpg", "image/jpeg", 3));
    when(queries.viewOf(report, false)).thenReturn(view);

    service.reply(report.getId(), REPORTER, "A wider photo", PHOTO);

    verify(photos).delete(old);
    verify(photos)
        .save(
            org.mockito.ArgumentMatchers.argThat(
                p -> p.getFilePath().equals("reports/new.jpg") && p.getReportId().equals(report.getId())));
    verify(storage).delete("reports/old.jpg");
  }

  @Test
  void reply_withAPhotoWhenThereWasNoneJustAddsIt() {
    openQuestion();
    when(photos.findByReportId(report.getId())).thenReturn(Optional.empty());
    when(storage.store("reports", "image/jpeg", PHOTO.content()))
        .thenReturn(new StoredFile("reports/new.jpg", "image/jpeg", 3));
    when(queries.viewOf(report, false)).thenReturn(view);

    service.reply(report.getId(), REPORTER, "A wider photo", PHOTO);

    verify(photos, never()).delete(any());
    verify(photos).save(any(ReportPhoto.class));
    verify(storage, never()).delete(anyString());
  }

  @Test
  void reply_aStoredFileThePhotoRulesRefuseIsRemovedAgain() {
    openQuestion();
    when(storage.store("reports", "image/jpeg", PHOTO.content()))
        .thenReturn(new StoredFile("reports/huge.jpg", "image/jpeg", 6L * 1024 * 1024));

    assertThatThrownBy(() -> service.reply(report.getId(), REPORTER, "A wider photo", PHOTO))
        .isInstanceOf(BusinessRuleException.class);

    verify(storage).delete("reports/huge.jpg");
    verify(photos, never()).save(any(ReportPhoto.class));
  }

  @Test
  void reply_anOldFileThatCannotBeRemovedDoesNotUndoTheAnswer() {
    openQuestion();
    ReportPhoto old = ReportPhoto.attach(report.getId(), "reports/old.jpg", "image/jpeg", 10);
    when(photos.findByReportId(report.getId())).thenReturn(Optional.of(old));
    when(storage.store("reports", "image/jpeg", PHOTO.content()))
        .thenReturn(new StoredFile("reports/new.jpg", "image/jpeg", 3));
    doThrow(new IllegalStateException("storage down")).when(storage).delete("reports/old.jpg");
    when(queries.viewOf(report, false)).thenReturn(view);

    assertThat(service.reply(report.getId(), REPORTER, "A wider photo", PHOTO)).isSameAs(view);
  }

  @Test
  void reply_unknownReportIs404() {
    UUID id = UUID.randomUUID();
    when(reports.findWithLockById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.reply(id, REPORTER, "The north side", null))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void reply_someoneElsesReportIs403_andNothingIsStored() {
    openQuestion();

    assertThatThrownBy(
            () -> service.reply(report.getId(), UUID.randomUUID(), "The north side", PHOTO))
        .isInstanceOf(ForbiddenRoleException.class);
    verify(storage, never()).store(anyString(), anyString(), any());
  }

  @Test
  void reply_withoutAnOpenQuestionIs409() {
    when(reports.findWithLockById(report.getId())).thenReturn(Optional.of(report));

    assertThatThrownBy(() -> service.reply(report.getId(), REPORTER, "The north side", null))
        .isInstanceOf(InvalidStateTransitionException.class);
  }
}
