package lk.dmc.disaster.warnings.integration;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Read-only query against the reports table. */
@Component
class JdbcVerifiedReports implements VerifiedReports {

  private final JdbcClient jdbc;

  JdbcVerifiedReports(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public List<VerifiedReportSummary> findVerified(Collection<UUID> reportIds) {
    if (reportIds.isEmpty()) {
      return List.of();
    }
    return jdbc.sql(
            """
            select id, reference_no, category, description, district_id, captured_at
            from hazard_reports
            where status = 'VERIFIED' and id in (:ids)
            order by captured_at desc
            """)
        .param("ids", reportIds)
        .query(
            (rs, row) ->
                new VerifiedReportSummary(
                    rs.getObject("id", UUID.class),
                    rs.getString("reference_no"),
                    rs.getString("category"),
                    rs.getString("description"),
                    rs.getObject("district_id", UUID.class),
                    rs.getObject("captured_at", OffsetDateTime.class).toInstant()))
        .list();
  }
}
