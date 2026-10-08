package lk.dmc.disaster.reports.service;

import static lk.dmc.disaster.reports.service.ReportFixtures.FLOOD;
import static lk.dmc.disaster.reports.service.ReportFixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.reports.entity.HazardReport;
import lk.dmc.disaster.reports.repository.HazardReportRepository;
import lk.dmc.disaster.shared.geo.GeoDistance;
import lk.dmc.disaster.shared.geo.GeoPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The 2-hour window and the "not rejected" rule are applied by the repository query (see {@code
 * HazardReportRepositoryTest}); this class covers the distance rule and the guards around it.
 */
@ExtendWith(MockitoExtension.class)
class DuplicateDetectorTest {

  private static final double LAT = 6.9391;
  private static final double LNG = 79.8921;
  private static final double METRES_PER_DEGREE_LAT = 111_194.93;
  private static final UUID REPORTER = UUID.randomUUID();

  @Mock HazardReportRepository reports;
  @InjectMocks DuplicateDetector detector;

  private static HazardReport subject() {
    return ReportFixtures.gps(REPORTER, LAT, LNG, NOW);
  }

  private static HazardReport northBy(double metres) {
    return ReportFixtures.gps(
        UUID.randomUUID(), LAT + metres / METRES_PER_DEGREE_LAT, LNG, NOW.minusSeconds(600));
  }

  private void givenCandidates(HazardReport subject, HazardReport... candidates) {
    when(reports.findDuplicateCandidates(any(), any(), any(), any()))
        .thenReturn(List.of(candidates));
  }

  @Test
  void findDuplicates_closeReportIsFlaggedWithItsDistance() {
    HazardReport subject = subject();
    HazardReport near = northBy(85);
    givenCandidates(subject, near);

    List<DuplicateMatch> matches = detector.findDuplicates(subject);

    assertThat(matches).hasSize(1);
    assertThat(matches.get(0).report()).isSameAs(near);
    assertThat(matches.get(0).distanceMetres()).isCloseTo(85, within(0.5));
  }

  @Test
  void findDuplicates_isFlaggedAt499MetresButNotAt501() {
    HazardReport subject = subject();
    HazardReport inside = northBy(499);
    HazardReport outside = northBy(501);
    givenCandidates(subject, inside, outside);

    assertThat(detector.findDuplicates(subject))
        .extracting(DuplicateMatch::report)
        .containsExactly(inside);
  }

  @Test
  void findDuplicates_testDistancesReallyStraddleTheLimit() {
    GeoPoint origin = GeoPoint.of(LAT, LNG);

    assertThat(
            GeoDistance.metresBetween(origin, GeoPoint.of(LAT + 499 / METRES_PER_DEGREE_LAT, LNG)))
        .isLessThan(500);
    assertThat(
            GeoDistance.metresBetween(origin, GeoPoint.of(LAT + 501 / METRES_PER_DEGREE_LAT, LNG)))
        .isGreaterThan(500);
  }

  @Test
  void findDuplicates_nearestComesFirst() {
    HazardReport subject = subject();
    HazardReport far = northBy(300);
    HazardReport near = northBy(50);
    givenCandidates(subject, far, near);

    assertThat(detector.findDuplicates(subject))
        .extracting(DuplicateMatch::report)
        .containsExactly(near, far);
  }

  @Test
  void findDuplicates_asksForTheSameTypeInsideTwoHoursEitherSide() {
    HazardReport subject = subject();
    givenCandidates(subject);

    detector.findDuplicates(subject);

    verify(reports)
        .findDuplicateCandidates(
            FLOOD,
            subject.getId(),
            subject.getCapturedAt().minus(Duration.ofHours(2)),
            subject.getCapturedAt().plus(Duration.ofHours(2)));
  }

  @Test
  void findDuplicates_manualLocationSubjectIsNeverFlaggedAndNothingIsQueried() {
    HazardReport manual = ReportFixtures.manual(REPORTER, NOW);

    assertThat(detector.findDuplicates(manual)).isEmpty();
    verifyNoInteractions(reports);
  }

  @Test
  void findDuplicates_candidateWithoutGpsIsSkipped() {
    HazardReport subject = subject();
    givenCandidates(subject, ReportFixtures.manual(UUID.randomUUID(), NOW.minusSeconds(600)));

    assertThat(detector.findDuplicates(subject)).isEmpty();
  }

  @Test
  void findDuplicates_noCandidatesMeansNoDuplicates() {
    HazardReport subject = subject();
    givenCandidates(subject);

    assertThat(detector.findDuplicates(subject)).isEmpty();
  }
}
