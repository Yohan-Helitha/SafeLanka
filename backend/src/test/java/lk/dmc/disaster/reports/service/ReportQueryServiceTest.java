package lk.dmc.disaster.reports.service;

import static lk.dmc.disaster.reports.service.ReportFixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lk.dmc.disaster.reports.entity.HazardReport;
import lk.dmc.disaster.reports.entity.ReportPhoto;
import lk.dmc.disaster.reports.entity.ReportStatus;
import lk.dmc.disaster.reports.repository.HazardReportRepository;
import lk.dmc.disaster.reports.repository.ReportPhotoRepository;
import lk.dmc.disaster.shared.actor.ActingUser;
import lk.dmc.disaster.shared.actor.UserDirectory;
import lk.dmc.disaster.shared.actor.UserSummary;
import lk.dmc.disaster.shared.domain.Role;
import lk.dmc.disaster.shared.error.ForbiddenRoleException;
import lk.dmc.disaster.shared.error.NotFoundException;
import lk.dmc.disaster.shared.storage.FileStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ReportQueryServiceTest {

  private static final UUID REPORTER = UUID.randomUUID();
  private static final ActingUser OFFICER = actor(Role.DMC_OFFICER);

  @Mock HazardReportRepository reports;
  @Mock ReportPhotoRepository photos;
  @Mock DuplicateDetector duplicates;
  @Mock UserDirectory users;
  @Mock FileStorage fileStorage;
  @InjectMocks ReportQueryService service;

  private static ActingUser actor(Role role) {
    return new ActingUser(UUID.randomUUID(), role, null, null, null);
  }

  private static ActingUser asReporter() {
    return new ActingUser(REPORTER, Role.CITIZEN, null, null, null);
  }

  private static HazardReport report() {
    return ReportFixtures.gps(REPORTER, 6.9391, 79.8921, NOW);
  }

  private static ReportPhoto photoOf(HazardReport r) {
    return ReportPhoto.attach(r.getId(), "reports/" + r.getId() + ".jpg", "image/jpeg", 100);
  }

  // ---- mine ---------------------------------------------------------------------------------

  @Test
  void mine_returnsTheReportersPageWithPhotosAttached() {
    HazardReport withPhoto = report();
    HazardReport without = report();
    ReportPhoto photo = photoOf(withPhoto);
    when(reports.findByReporterIdOrderByCapturedAtDesc(any(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(withPhoto, without)));
    when(photos.findByReportIdIn(any())).thenReturn(List.of(photo));

    Page<ReportWithPhoto> page = service.mine(REPORTER, 0, 20);

    assertThat(page.getContent())
        .containsExactly(new ReportWithPhoto(withPhoto, photo), new ReportWithPhoto(without, null));
  }

  @Test
  void mine_emptyPageDoesNotAskForPhotos() {
    when(reports.findByReporterIdOrderByCapturedAtDesc(any(), any(Pageable.class)))
        .thenReturn(Page.empty());

    assertThat(service.mine(REPORTER, 0, 20)).isEmpty();
    verifyNoInteractions(photos);
  }

  @Test
  void mine_pageAndSizeAreClamped() {
    when(reports.findByReporterIdOrderByCapturedAtDesc(any(), any(Pageable.class)))
        .thenReturn(Page.empty());

    service.mine(REPORTER, -3, 5000);
    service.mine(REPORTER, 2, 0);

    ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
    verify(reports, org.mockito.Mockito.times(2))
        .findByReporterIdOrderByCapturedAtDesc(any(), pageable.capture());
    assertThat(pageable.getAllValues().get(0)).isEqualTo(PageRequest.of(0, 100));
    assertThat(pageable.getAllValues().get(1)).isEqualTo(PageRequest.of(2, 1));
  }

  // ---- queue --------------------------------------------------------------------------------

  @Test
  void queue_isOldestFirstAndFlagsPhotosAndOpenDuplicates() {
    HazardReport pendingWithDuplicate = report();
    HazardReport pendingAlone = report();
    HazardReport verified = report();
    verified.verify(UUID.randomUUID(), null, ReportFixtures.CLOCK);
    ReportPhoto photo = photoOf(pendingAlone);
    when(reports.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(pendingWithDuplicate, pendingAlone, verified)));
    when(photos.findByReportIdIn(any())).thenReturn(List.of(photo));
    when(duplicates.findDuplicates(pendingWithDuplicate))
        .thenReturn(List.of(new DuplicateMatch(pendingAlone, 80)));
    when(duplicates.findDuplicates(pendingAlone)).thenReturn(List.of());

    Page<QueueItem> page = service.queue(null, null, null, 0, 20);

    assertThat(page.getContent())
        .containsExactly(
            new QueueItem(pendingWithDuplicate, false, true),
            new QueueItem(pendingAlone, true, false),
            new QueueItem(verified, false, false));
    verify(duplicates, never()).findDuplicates(verified);
    ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
    verify(reports).findAll(any(Specification.class), pageable.capture());
    assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "capturedAt"));
  }

  @Test
  void queue_emptyResultIsAnEmptyPage() {
    when(reports.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

    assertThat(service.queue(ReportStatus.PENDING, UUID.randomUUID(), UUID.randomUUID(), 0, 20))
        .isEmpty();
  }

  // ---- detail -------------------------------------------------------------------------------

  @Test
  void detail_reporterSeesOwnReportWithoutDuplicates() {
    HazardReport report = report();
    UserSummary summary = new UserSummary(REPORTER, "Ruwan Fernando", Role.CITIZEN);
    when(reports.findById(report.getId())).thenReturn(Optional.of(report));
    when(photos.findByReportId(report.getId())).thenReturn(Optional.empty());
    when(users.require(REPORTER)).thenReturn(summary);

    ReportDetailView view = service.detail(report.getId(), asReporter());

    assertThat(view.report()).isSameAs(report);
    assertThat(view.reporter()).isEqualTo(summary);
    assertThat(view.duplicates()).isEmpty();
    assertThat(view.photo()).isNull();
    verifyNoInteractions(duplicates);
  }

  @Test
  void detail_officerSeesDuplicates() {
    HazardReport report = report();
    HazardReport other = report();
    List<DuplicateMatch> matches = List.of(new DuplicateMatch(other, 85));
    when(reports.findById(report.getId())).thenReturn(Optional.of(report));
    when(photos.findByReportId(report.getId())).thenReturn(Optional.of(photoOf(report)));
    when(users.require(REPORTER))
        .thenReturn(new UserSummary(REPORTER, "Ruwan Fernando", Role.CITIZEN));
    when(duplicates.findDuplicates(report)).thenReturn(matches);

    ReportDetailView view = service.detail(report.getId(), OFFICER);

    assertThat(view.duplicates()).isEqualTo(matches);
    assertThat(view.photo()).isNotNull();
  }

  @Test
  void detail_anotherCitizenIsForbidden() {
    HazardReport report = report();
    when(reports.findById(report.getId())).thenReturn(Optional.of(report));

    assertThatThrownBy(() -> service.detail(report.getId(), actor(Role.CITIZEN)))
        .isInstanceOf(ForbiddenRoleException.class);
    verifyNoInteractions(users);
  }

  @Test
  void detail_otherStaffRolesAreForbidden() {
    HazardReport report = report();
    when(reports.findById(report.getId())).thenReturn(Optional.of(report));

    assertThatThrownBy(() -> service.detail(report.getId(), actor(Role.DISTRICT_OFFICER)))
        .isInstanceOf(ForbiddenRoleException.class);
  }

  @Test
  void detail_unknownReportIsNotFoundBeforeAnyAccessCheck() {
    UUID unknown = UUID.randomUUID();
    when(reports.findById(unknown)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.detail(unknown, actor(Role.CITIZEN)))
        .isInstanceOf(NotFoundException.class);
  }

  // ---- photo --------------------------------------------------------------------------------

  @Test
  void photo_reporterGetsTheStoredBytesAndContentType() {
    HazardReport report = report();
    ReportPhoto photo = photoOf(report);
    byte[] bytes = {1, 2, 3};
    when(reports.findById(report.getId())).thenReturn(Optional.of(report));
    when(photos.findByReportId(report.getId())).thenReturn(Optional.of(photo));
    when(fileStorage.load(photo.getFilePath())).thenReturn(bytes);

    PhotoContent content = service.photo(report.getId(), asReporter());

    assertThat(content.bytes()).isEqualTo(bytes);
    assertThat(content.contentType()).isEqualTo("image/jpeg");
  }

  @Test
  void photo_officerMayView() {
    HazardReport report = report();
    ReportPhoto photo = photoOf(report);
    when(reports.findById(report.getId())).thenReturn(Optional.of(report));
    when(photos.findByReportId(report.getId())).thenReturn(Optional.of(photo));
    when(fileStorage.load(photo.getFilePath())).thenReturn(new byte[] {1});

    assertThat(service.photo(report.getId(), OFFICER).bytes()).hasSize(1);
  }

  @Test
  void photo_strangerIsForbiddenAndNothingIsLoaded() {
    HazardReport report = report();
    when(reports.findById(report.getId())).thenReturn(Optional.of(report));

    assertThatThrownBy(() -> service.photo(report.getId(), actor(Role.VOLUNTEER)))
        .isInstanceOf(ForbiddenRoleException.class);
    verifyNoInteractions(fileStorage);
  }

  @Test
  void photo_reportWithoutPhotoIsNotFound() {
    HazardReport report = report();
    when(reports.findById(report.getId())).thenReturn(Optional.of(report));
    when(photos.findByReportId(report.getId())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.photo(report.getId(), asReporter()))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void photo_unknownReportIsNotFound() {
    UUID unknown = UUID.randomUUID();
    when(reports.findById(unknown)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.photo(unknown, OFFICER)).isInstanceOf(NotFoundException.class);
  }
}
