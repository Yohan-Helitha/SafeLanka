package lk.dmc.disaster.warnings.integration;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Reads the shared hazard_types table. */
@Component
class JdbcHazardTypeDirectory implements HazardTypeDirectory {

  private final JdbcClient jdbc;

  JdbcHazardTypeDirectory(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Map<UUID, String> codesById() {
    return jdbc
        .sql("select id, code from hazard_types")
        .query((rs, row) -> Map.entry(rs.getObject("id", UUID.class), rs.getString("code")))
        .list()
        .stream()
        .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
  }
}
