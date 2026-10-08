package lk.dmc.disaster.reports;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lk.dmc.disaster.TestcontainersConfiguration;
import lk.dmc.disaster.shared.geo.GeoDistance;
import lk.dmc.disaster.shared.geo.GeoPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Read-only checks that {@code V6_0_2__seed_demo_reports.sql} produced the agreed demo data. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ReportSeedDataTest {

  private static final String SEED_IDS = "id::text like '00000000-0000-0000-0012-%'";

  @Autowired JdbcTemplate jdbc;

  @Test
  void twelveDemoReportsExist() {
    assertThat(
            jdbc.queryForObject(
                "select count(*) from hazard_reports where " + SEED_IDS, Integer.class))
        .isEqualTo(12);
  }

  /**
   * Pending demo reports can be decided in the app, so their status is not asserted. Verified and
   * rejected ones are final and can never change.
   */
  @Test
  void decidedDemoReportsKeepTheirFinalStatusAndReasons() {
    Map<String, Long> finalByStatus =
        jdbc
            .queryForList(
                "select status, count(*) c from hazard_reports where "
                    + SEED_IDS
                    + " and id::text ~ '-00000000000[1-4]$|-00000000001[12]$' group by status")
            .stream()
            .collect(Collectors.toMap(r -> (String) r.get("status"), r -> (Long) r.get("c")));

    assertThat(finalByStatus)
        .containsEntry("VERIFIED", 4L)
        .containsEntry("REJECTED", 2L)
        .hasSize(2);
    assertThat(
            jdbc.queryForList(
                "select rejection_reason from hazard_reports"
                    + " where id in ('00000000-0000-0000-0012-000000000011',"
                    + " '00000000-0000-0000-0012-000000000012') order by id",
                String.class))
        .containsExactly("INSUFFICIENT_EVIDENCE", "DUPLICATE");
  }

  @Test
  void referenceNumbersAreSequentialFromOneToTwelve() {
    List<String> numbers =
        jdbc.queryForList(
            "select reference_no from hazard_reports where " + SEED_IDS + " order by reference_no",
            String.class);

    assertThat(numbers).hasSize(12).first().isEqualTo("RPT-2026-0001");
    assertThat(numbers).last().isEqualTo("RPT-2026-0012");
  }

  @Test
  void sedawattePairIsWithinOneHundredMetres() {
    GeoPoint first = point("00000000-0000-0000-0012-000000000005");
    GeoPoint second = point("00000000-0000-0000-0012-000000000006");

    assertThat(GeoDistance.metresBetween(first, second)).isLessThan(100);
  }

  @Test
  void ratnapuraReportWasVerifiedForTheKaluEvent() {
    Map<String, Object> row =
        jdbc.queryForMap(
            "select status, reviewed_at, district_id::text d from hazard_reports"
                + " where id = '00000000-0000-0000-0012-000000000001'");

    assertThat(row.get("status")).isEqualTo("VERIFIED");
    assertThat(((java.sql.Timestamp) row.get("reviewed_at")).toInstant())
        .isEqualTo(Instant.parse("2026-05-14T00:05:00Z"));
    assertThat(row.get("d")).isEqualTo("00000000-0000-0000-0001-000000000004");
  }

  @Test
  void everyRejectedReportHasAReasonAndEveryDecidedReportAReviewer() {
    assertThat(
            jdbc.queryForObject(
                "select count(*) from hazard_reports where "
                    + SEED_IDS
                    + " and status = 'REJECTED' and rejection_reason is null",
                Integer.class))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from hazard_reports where "
                    + SEED_IDS
                    + " and status <> 'PENDING' and reviewed_by is null",
                Integer.class))
        .isZero();
  }

  @Test
  void seedCoversEveryDemoDistrictWithGps() {
    assertThat(
            jdbc.queryForObject(
                "select count(distinct district_id) from hazard_reports where "
                    + SEED_IDS
                    + " and latitude is not null",
                Integer.class))
        .isEqualTo(5);
  }

  private GeoPoint point(String id) {
    return jdbc.queryForObject(
        "select latitude, longitude from hazard_reports where id = ?::uuid",
        (rs, i) -> GeoPoint.of(rs.getDouble("latitude"), rs.getDouble("longitude")),
        id);
  }
}
