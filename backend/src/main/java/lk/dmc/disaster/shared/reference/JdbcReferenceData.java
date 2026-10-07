package lk.dmc.disaster.shared.reference;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
class JdbcReferenceData implements ReferenceData {

  private final JdbcClient jdbc;

  JdbcReferenceData(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<HazardTypeInfo> hazardType(UUID id) {
    return jdbc.sql(
            "select id, code, name, active, report_categories from hazard_types where id = :id")
        .param("id", id)
        .query(
            (rs, row) ->
                new HazardTypeInfo(
                    rs.getObject("id", UUID.class),
                    rs.getString("code"),
                    rs.getString("name"),
                    rs.getBoolean("active"),
                    categories(rs.getArray("report_categories"))))
        .optional();
  }

  @Override
  public Optional<String> districtName(UUID id) {
    return jdbc.sql("select name from districts where id = :id")
        .param("id", id)
        .query(String.class)
        .optional();
  }

  private static List<String> categories(java.sql.Array array) throws SQLException {
    return List.of((String[]) array.getArray());
  }
}
