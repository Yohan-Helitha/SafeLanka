package lk.dmc.disaster.reports.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import lk.dmc.disaster.TestcontainersConfiguration;
import lk.dmc.disaster.reports.entity.HazardReport;
import lk.dmc.disaster.reports.entity.RejectionReason;
import lk.dmc.disaster.reports.entity.ReportDraft;
import lk.dmc.disaster.reports.entity.ReportPhoto;
import lk.dmc.disaster.reports.entity.ReportStatus;
import lk.dmc.disaster.shared.config.JpaAuditingConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

/**
 * Runs against the configured database, but inside a transaction that is rolled back after every
 * test, so no rows are left behind. Reporters are throw-away users created in that transaction; the
 * seeded districts, hazard types and DMC officer are only referenced. Capture times sit in 2031 so
 * existing data can never match.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, JpaAuditingConfig.class})
class HazardReportRepositoryTest {

  private static final UUID COLOMBO = UUID.fromString("00000000-0000-0000-0001-000000000001");
  private static final UUID GAMPAHA = UUID.fromString("00000000-0000-0000-0001-000000000002");
  private static final UUID FLOOD = UUID.fromString("00000000-0000-0000-0003-000000000001");
  private static final UUID LANDSLIDE = UUID.fromString("00000000-0000-0000-0003-000000000002");
  private static final UUID OFFICER = UUID.fromString("00000000-0000-0000-0006-000000000001");
  private static final Instant T0 = Instant.parse("2031-01-01T08:00:00Z");
  private static final Clock CLOCK = Clock.fixed(T0.plusSeconds(3 * 3600), ZoneOffset.UTC);

  @Autowired HazardReportRepository reports;
  @Autowired ReportPhotoRepository photos;
  @Autowired JdbcTemplate jdbcTemplate;
  @Autowired EntityManager em;

  private UUID reporter;
  private UUID otherReporter;

  @BeforeEach
  void createReporters() {
    reporter = insertUser();
    otherReporter = insertUser();
  }

  private UUID insertUser() {
    UUID id = UUID.randomUUID();
    jdbcTemplate.update(
        "insert into users (id, role, full_name, district_id) values (?, 'CITIZEN', 'Test Reporter', ?)",
        id,
        COLOMBO);
    return id;
  }

  private HazardReport report(
      UUID by, UUID type, UUID district, Double lat, Double lng, Instant capturedAt) {
    ReportDraft draft =
        new ReportDraft(
            UUID.randomUUID(),
            type,
            "OTHER",
            "Water over the road near the canal bridge",
            lat,
            lng,
            lat == null ? "Near the old railway bridge" : null,
            district,
            capturedAt);
    return reports.saveAndFlush(
        HazardReport.submit(draft, by, "TST-" + UUID.randomUUID().toString().substring(0, 8), CLOCK));
  }

  private HazardReport floodAt(UUID by, Instant capturedAt) {
    return report(by, FLOOD, COLOMBO, 6.9391, 79.8921, capturedAt);
  }

  // ---- mapping ------------------------------------------------------------------------------

  @Test
  void save_roundTripKeepsEveryField() {
    HazardReport saved = floodAt(reporter, T0.minusSeconds(60));
    saved.reject(OFFICER, RejectionReason.OTHER, "Not a hazard on site", CLOCK);
    reports.saveAndFlush(saved);
    em.clear();

    HazardReport found = reports.findById(saved.getId()).orElseThrow();

    assertThat(found.getReferenceNo()).isEqualTo(saved.getReferenceNo());
    assertThat(found.getReporterId()).isEqualTo(reporter);
    assertThat(found.getHazardTypeId()).isEqualTo(FLOOD);
    assertThat(found.getDistrictId()).isEqualTo(COLOMBO);
    assertThat(found.getLatitude()).isEqualTo(6.9391);
    assertThat(found.getStatus()).isEqualTo(ReportStatus.REJECTED);
    assertThat(found.getRejectionReason()).isEqualTo(RejectionReason.OTHER);
    assertThat(found.getReviewedBy()).isEqualTo(OFFICER);
    assertThat(found.getReviewComment()).isEqualTo("Not a hazard on site");
    assertThat(found.getCreatedAt()).isNotNull();
    assertThat(found.getUpdatedAt()).isNotNull();
  }

  @Test
  void save_manualLocationReportHasNoCoordinates() {
    HazardReport saved = report(reporter, FLOOD, COLOMBO, null, null, T0);
    em.clear();

    HazardReport found = reports.findById(saved.getId()).orElseThrow();

    assertThat(found.isManualLocation()).isTrue();
    assertThat(found.getLatitude()).isNull();
    assertThat(found.getManualLocationText()).isEqualTo("Near the old railway bridge");
  }

  // ---- idempotency and reporter list --------------------------------------------------------

  @Test
  void findByClientRef_returnsTheReportSyncedUnderThatKey() {
    HazardReport saved = floodAt(reporter, T0);

    assertThat(reports.findByClientRef(saved.getClientRef())).get().isEqualTo(saved);
  }

  @Test
  void findByClientRef_unknownKeyIsEmpty() {
    assertThat(reports.findByClientRef(UUID.randomUUID())).isEmpty();
  }

  @Test
  void save_sameClientRefTwiceViolatesTheUniqueKey() {
    HazardReport first = floodAt(reporter, T0);
    ReportDraft clash =
        new ReportDraft(
            first.getClientRef(),
            FLOOD,
            "OTHER",
            "Another description of the same thing",
            6.9,
            79.9,
            null,
            COLOMBO,
            T0);

    assertThatThrownBy(
            () -> reports.saveAndFlush(HazardReport.submit(clash, reporter, "TST-CLASH001", CLOCK)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void findByReporter_newestFirstAndOnlyThatReportersReports() {
    HazardReport older = floodAt(reporter, T0.minusSeconds(3600));
    HazardReport newer = floodAt(reporter, T0);
    floodAt(otherReporter, T0.minusSeconds(60));

    var page = reports.findByReporterIdOrderByCapturedAtDesc(reporter, PageRequest.of(0, 10));

    assertThat(page.getContent()).containsExactly(newer, older);
    assertThat(page.getTotalElements()).isEqualTo(2);
  }

  @Test
  void findByReporter_pagesAreCutBySize() {
    floodAt(reporter, T0.minusSeconds(120));
    HazardReport newest = floodAt(reporter, T0);
    floodAt(reporter, T0.minusSeconds(60));

    var page = reports.findByReporterIdOrderByCapturedAtDesc(reporter, PageRequest.of(0, 1));

    assertThat(page.getContent()).containsExactly(newest);
    assertThat(page.getTotalPages()).isEqualTo(3);
  }

  @Test
  void findByReporter_noReportsGivesEmptyPage() {
    assertThat(reports.findByReporterIdOrderByCapturedAtDesc(reporter, PageRequest.of(0, 10)))
        .isEmpty();
  }

  // ---- queue specifications -----------------------------------------------------------------

  private List<HazardReport> queue(ReportStatus status, UUID type, UUID district) {
    var onlyMine = ReportSpecifications.queue(status, type, district)
        .and((root, q, cb) -> cb.equal(root.get("reporterId"), reporter));
    return reports.findAll(onlyMine, Sort.by("capturedAt").ascending());
  }

  @Test
  void queue_noFiltersReturnsEverythingOldestFirst() {
    HazardReport newer = floodAt(reporter, T0);
    HazardReport older = report(reporter, LANDSLIDE, GAMPAHA, 7.0, 80.0, T0.minusSeconds(600));

    assertThat(queue(null, null, null)).containsExactly(older, newer);
  }

  @Test
  void queue_filtersByStatus() {
    HazardReport pending = floodAt(reporter, T0);
    HazardReport verified = floodAt(reporter, T0.minusSeconds(60));
    verified.verify(OFFICER, null, CLOCK);
    reports.saveAndFlush(verified);

    assertThat(queue(ReportStatus.PENDING, null, null)).containsExactly(pending);
    assertThat(queue(ReportStatus.VERIFIED, null, null)).containsExactly(verified);
    assertThat(queue(ReportStatus.REJECTED, null, null)).isEmpty();
  }

  @Test
  void queue_filtersByHazardTypeAndDistrictTogether() {
    HazardReport floodColombo = floodAt(reporter, T0);
    HazardReport slideGampaha = report(reporter, LANDSLIDE, GAMPAHA, 7.0, 80.0, T0);
    HazardReport floodGampaha = report(reporter, FLOOD, GAMPAHA, 7.0, 80.0, T0);

    assertThat(queue(null, FLOOD, null)).containsExactlyInAnyOrder(floodColombo, floodGampaha);
    assertThat(queue(null, null, GAMPAHA)).containsExactlyInAnyOrder(slideGampaha, floodGampaha);
    assertThat(queue(null, FLOOD, GAMPAHA)).containsExactly(floodGampaha);
  }

  // ---- duplicate candidates -----------------------------------------------------------------

  private List<HazardReport> candidates(HazardReport subject) {
    return reports.findDuplicateCandidates(
        subject.getHazardTypeId(),
        subject.getId(),
        subject.getCapturedAt().minusSeconds(7200),
        subject.getCapturedAt().plusSeconds(7200));
  }

  @Test
  void duplicateCandidates_sameTypeInsideWindowIsReturnedButNotTheSubject() {
    HazardReport subject = floodAt(reporter, T0);
    HazardReport near = floodAt(otherReporter, T0.plusSeconds(1800));

    assertThat(candidates(subject)).containsExactly(near);
  }

  @Test
  void duplicateCandidates_windowEdgesAreInclusiveAndBeyondIsExcluded() {
    HazardReport subject = floodAt(reporter, T0);
    HazardReport onEdge = floodAt(otherReporter, T0.minusSeconds(7200));
    floodAt(otherReporter, T0.plusSeconds(7201));

    assertThat(candidates(subject)).containsExactly(onEdge);
  }

  @Test
  void duplicateCandidates_ignoresOtherTypesRejectedAndManualLocationReports() {
    HazardReport subject = floodAt(reporter, T0);
    report(otherReporter, LANDSLIDE, COLOMBO, 6.9391, 79.8921, T0);
    report(otherReporter, FLOOD, COLOMBO, null, null, T0);
    HazardReport rejected = floodAt(otherReporter, T0);
    rejected.reject(OFFICER, RejectionReason.DUPLICATE, null, CLOCK);
    reports.saveAndFlush(rejected);
    HazardReport needsInfo = floodAt(otherReporter, T0);
    needsInfo.requestInfo(OFFICER, "Which side of the bridge?", CLOCK);
    reports.saveAndFlush(needsInfo);

    assertThat(candidates(subject)).containsExactly(needsInfo);
  }

  // ---- constraints and photos ---------------------------------------------------------------

  @Test
  void database_rejectsARejectedReportWithoutAReason() {
    HazardReport saved = floodAt(reporter, T0);

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    "update hazard_reports set status = 'REJECTED', reviewed_by = ? where id = ?",
                    OFFICER,
                    saved.getId()))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void photo_isFoundByReportId() {
    HazardReport saved = floodAt(reporter, T0);
    photos.saveAndFlush(ReportPhoto.attach(saved.getId(), "reports/a.jpg", "image/jpeg", 2048));

    ReportPhoto found = photos.findByReportId(saved.getId()).orElseThrow();

    assertThat(found.getFilePath()).isEqualTo("reports/a.jpg");
    assertThat(found.getSizeBytes()).isEqualTo(2048);
    assertThat(photos.findByReportId(UUID.randomUUID())).isEmpty();
  }

  // ---- reference numbers --------------------------------------------------------------------

  @Test
  void referenceNumbers_increaseAndUseTheClockYear() {
    var generator = new ReferenceNumberGenerator(JdbcClient.create(jdbcTemplate), CLOCK);

    String first = generator.next();
    String second = generator.next();

    assertThat(first).matches("RPT-2031-\\d{4,}");
    assertThat(sequenceOf(second)).isGreaterThan(sequenceOf(first));
  }

  private static long sequenceOf(String reference) {
    return Long.parseLong(reference.substring(reference.lastIndexOf('-') + 1));
  }
}
