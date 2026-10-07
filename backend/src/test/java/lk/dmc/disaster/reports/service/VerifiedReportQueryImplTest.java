package lk.dmc.disaster.reports.service;

import static lk.dmc.disaster.reports.service.ReportFixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lk.dmc.disaster.reports.VerifiedReportFilter;
import lk.dmc.disaster.reports.VerifiedReportSummary;
import lk.dmc.disaster.reports.entity.HazardReport;
import lk.dmc.disaster.reports.entity.ReportPhoto;
import lk.dmc.disaster.reports.repository.HazardReportRepository;
import lk.dmc.disaster.reports.repository.ReportPhotoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class VerifiedReportQueryImplTest {

  private static final UUID OFFICER = UUID.randomUUID();
  private static final Instant VERIFIED_AT = Instant.parse("2026-10-04T09:00:00Z");

  @Mock HazardReportRepository reports;
  @Mock ReportPhotoRepository photos;
  @InjectMocks VerifiedReportQueryImpl query;

  private static HazardReport verified() {
    HazardReport r = ReportFixtures.gps(UUID.randomUUID(), 6.9391, 79.8921, NOW);
    r.verify(OFFICER, null, java.time.Clock.fixed(VERIFIED_AT, java.time.ZoneOffset.UTC));
    return r;
  }

  @Test
  void findVerified_mapsReportsToSummariesWithPhotoUrlOnlyWhenAPhotoExists() {
    HazardReport withPhoto = verified();
    HazardReport without = verified();
    when(reports.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(withPhoto, without)));
    when(photos.findByReportIdIn(any()))
        .thenReturn(List.of(ReportPhoto.attach(withPhoto.getId(), "reports/a.jpg", "image/jpeg", 9)));

    List<VerifiedReportSummary> result = query.findVerified(VerifiedReportFilter.any());

    assertThat(result).hasSize(2);
    VerifiedReportSummary first = result.get(0);
    assertThat(first.reportId()).isEqualTo(withPhoto.getId());
    assertThat(first.referenceNo()).isEqualTo(withPhoto.getReferenceNo());
    assertThat(first.hazardTypeId()).isEqualTo(withPhoto.getHazardTypeId());
    assertThat(first.category()).isEqualTo("RISING_WATER");
    assertThat(first.latitude()).isEqualTo(6.9391);
    assertThat(first.manualLocation()).isFalse();
    assertThat(first.photoUrl()).isEqualTo("/api/reports/" + withPhoto.getId() + "/photo");
    assertThat(first.capturedAt()).isEqualTo(NOW);
    assertThat(first.verifiedAt()).isEqualTo(VERIFIED_AT);
    assertThat(first.verifiedBy()).isEqualTo(OFFICER);
    assertThat(result.get(1).photoUrl()).isNull();
  }

  @Test
  void findVerified_asksNewestVerificationFirstWithATwoHundredRowCap() {
    when(reports.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

    query.findVerified(VerifiedReportFilter.any());

    ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
    verify(reports).findAll(any(Specification.class), pageable.capture());
    assertThat(pageable.getValue().getPageSize()).isEqualTo(200);
    assertThat(pageable.getValue().getSort())
        .isEqualTo(Sort.by(Sort.Direction.DESC, "reviewedAt"));
  }

  @Test
  void findVerified_nothingFoundMeansEmptyListAndNoPhotoQuery() {
    when(reports.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

    assertThat(query.findVerified(VerifiedReportFilter.any())).isEmpty();
    verifyNoInteractions(photos);
  }

  @Test
  void findVerified_nullFilterMeansAny() {
    when(reports.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

    assertThat(query.findVerified(null)).isEmpty();
  }

  @Test
  void findVerifiedById_verifiedReportIsReturned() {
    HazardReport r = verified();
    when(reports.findById(r.getId())).thenReturn(Optional.of(r));
    when(photos.findByReportId(r.getId())).thenReturn(Optional.empty());

    assertThat(query.findVerifiedById(r.getId()))
        .hasValueSatisfying(
            s -> {
              assertThat(s.reportId()).isEqualTo(r.getId());
              assertThat(s.photoUrl()).isNull();
            });
  }

  @Test
  void findVerifiedById_pendingReportIsEmpty() {
    HazardReport pending = ReportFixtures.gps(UUID.randomUUID(), 6.9, 79.9, NOW);
    when(reports.findById(pending.getId())).thenReturn(Optional.of(pending));

    assertThat(query.findVerifiedById(pending.getId())).isEmpty();
    verifyNoInteractions(photos);
  }

  @Test
  void findVerifiedById_unknownReportIsEmpty() {
    UUID unknown = UUID.randomUUID();
    when(reports.findById(unknown)).thenReturn(Optional.empty());

    assertThat(query.findVerifiedById(unknown)).isEmpty();
  }
}
