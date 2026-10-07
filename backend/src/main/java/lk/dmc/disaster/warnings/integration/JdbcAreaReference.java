package lk.dmc.disaster.warnings.integration;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Reads the shared district and river basin reference tables. */
@Component
class JdbcAreaReference implements AreaReference {

  private final JdbcClient jdbc;

  JdbcAreaReference(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Set<UUID> districtsInBasins(Collection<UUID> riverBasinIds) {
    if (riverBasinIds.isEmpty()) {
      return Set.of();
    }
    return Set.copyOf(
        jdbc.sql(
                "select distinct district_id from district_river_basins"
                    + " where river_basin_id in (:basinIds)")
            .param("basinIds", riverBasinIds)
            .query(UUID.class)
            .list());
  }

  @Override
  public Set<UUID> basinsOfDistrict(UUID districtId) {
    return Set.copyOf(
        jdbc.sql("select river_basin_id from district_river_basins where district_id = :id")
            .param("id", districtId)
            .query(UUID.class)
            .list());
  }

  @Override
  public boolean districtExists(UUID districtId) {
    return exists("districts", districtId);
  }

  @Override
  public boolean riverBasinExists(UUID riverBasinId) {
    return exists("river_basins", riverBasinId);
  }

  private boolean exists(String table, UUID id) {
    return jdbc.sql("select count(*) from " + table + " where id = :id")
            .param("id", id)
            .query(Long.class)
            .single()
        > 0;
  }
}
