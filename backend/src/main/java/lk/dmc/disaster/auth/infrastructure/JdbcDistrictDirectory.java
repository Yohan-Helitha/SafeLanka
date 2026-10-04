package lk.dmc.disaster.auth.infrastructure;

import java.util.Optional;
import java.util.UUID;
import lk.dmc.disaster.auth.application.port.DistrictDirectory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
class JdbcDistrictDirectory implements DistrictDirectory {

  private final JdbcClient jdbc;

  JdbcDistrictDirectory(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public boolean exists(UUID districtId) {
    return jdbc.sql("select count(*) from districts where id = :id")
            .param("id", districtId)
            .query(Integer.class)
            .single()
        > 0;
  }

  @Override
  public Optional<UUID> riverBasinOf(UUID districtId) {
    return jdbc.sql(
            "select river_basin_id from district_river_basins where district_id = :id limit 1")
        .param("id", districtId)
        .query(UUID.class)
        .optional();
  }
}
