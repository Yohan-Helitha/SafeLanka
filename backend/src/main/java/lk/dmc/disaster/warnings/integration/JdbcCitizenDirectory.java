package lk.dmc.disaster.warnings.integration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lk.dmc.disaster.shared.domain.Role;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Reads reachable people straight from the shared users table. */
@Component
class JdbcCitizenDirectory implements CitizenDirectory {

  private static final List<String> REACHABLE_ROLES =
      List.of(Role.CITIZEN.name(), Role.VOLUNTEER.name());

  private final JdbcClient jdbc;

  JdbcCitizenDirectory(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public List<UUID> findCitizenIdsInAreas(
      Collection<UUID> districtIds, Collection<UUID> riverBasinIds) {
    if (districtIds.isEmpty() && riverBasinIds.isEmpty()) {
      return List.of();
    }
    return query("select id", districtIds, riverBasinIds, " order by id").query(UUID.class).list();
  }

  @Override
  public long countCitizensInAreas(Collection<UUID> districtIds, Collection<UUID> riverBasinIds) {
    if (districtIds.isEmpty() && riverBasinIds.isEmpty()) {
      return 0;
    }
    return query("select count(*)", districtIds, riverBasinIds, "").query(Long.class).single();
  }

  @Override
  public Map<UUID, UUID> districtsOf(Collection<UUID> citizenIds) {
    if (citizenIds.isEmpty()) {
      return Map.of();
    }
    return jdbc
        .sql("select id, district_id from users where id in (:ids)")
        .param("ids", citizenIds)
        .query(
            (rs, row) ->
                Map.entry(rs.getObject("id", UUID.class), rs.getObject("district_id", UUID.class)))
        .list()
        .stream()
        .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  /** Builds the shared WHERE clause once so listing and counting can never disagree. */
  private JdbcClient.StatementSpec query(
      String select, Collection<UUID> districtIds, Collection<UUID> riverBasinIds, String suffix) {
    List<String> areas = new ArrayList<>();
    if (!districtIds.isEmpty()) {
      areas.add("district_id in (:districtIds)");
    }
    if (!riverBasinIds.isEmpty()) {
      areas.add("river_basin_id in (:basinIds)");
    }
    String sql =
        select
            + " from users where role in (:roles) and status <> 'DISABLED' and ("
            + String.join(" or ", areas)
            + ")"
            + suffix;
    JdbcClient.StatementSpec spec = jdbc.sql(sql).param("roles", REACHABLE_ROLES);
    if (!districtIds.isEmpty()) {
      spec = spec.param("districtIds", districtIds);
    }
    if (!riverBasinIds.isEmpty()) {
      spec = spec.param("basinIds", riverBasinIds);
    }
    return spec;
  }
}
