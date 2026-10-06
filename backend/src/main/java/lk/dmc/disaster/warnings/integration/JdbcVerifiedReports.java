package lk.dmc.disaster.warnings.integration;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Read-only check against the reports table. */
@Component
class JdbcVerifiedReports implements VerifiedReports {

  private final JdbcClient jdbc;

  JdbcVerifiedReports(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Set<UUID> filterVerified(Collection<UUID> reportIds) {
    if (reportIds.isEmpty()) {
      return Set.of();
    }
    return Set.copyOf(
        jdbc.sql("select id from hazard_reports where status = 'VERIFIED' and id in (:ids)")
            .param("ids", reportIds)
            .query(UUID.class)
            .list());
  }
}
