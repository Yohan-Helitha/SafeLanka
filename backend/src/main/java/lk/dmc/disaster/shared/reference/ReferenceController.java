package lk.dmc.disaster.shared.reference;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.shared.api.ApiResponse;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public reference data including districts, river basins, hazard types, organisations, relief
 * items, and disaster events.
 */
@RestController
@RequestMapping("/api/reference")
class ReferenceController {

  record DistrictDto(UUID id, String code, String name, String province) {}

  record RiverBasinDto(UUID id, String code, String name, List<UUID> districtIds) {}

  record HazardTypeDto(
      UUID id,
      String code,
      String name,
      String onsetSpeed,
      List<String> reportCategories,
      boolean active) {}

  record OrganisationDto(UUID id, String name, String type) {}

  record ReliefItemDto(UUID id, String code, String name, String unit, String category) {}

  record DisasterEventDto(
      UUID id,
      String name,
      UUID hazardTypeId,
      String status,
      Instant startedAt,
      Instant endedAt,
      List<UUID> districtIds) {}

  private final JdbcClient jdbc;

  ReferenceController(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @GetMapping("/districts")
  ApiResponse<List<DistrictDto>> districts() {
    List<DistrictDto> districts =
        jdbc.sql("select id, code, name, province from districts order by name")
            .query(DistrictDto.class)
            .list();
    return ApiResponse.of(districts);
  }

  @GetMapping("/river-basins")
  ApiResponse<List<RiverBasinDto>> riverBasins() {
    String sql =
        """
        select rb.id, rb.code, rb.name,
               coalesce(array_agg(drb.district_id) filter (where drb.district_id is not null), '{}') as district_ids
        from river_basins rb
        left join district_river_basins drb on rb.id = drb.river_basin_id
        group by rb.id, rb.code, rb.name
        order by rb.name
        """;
    List<RiverBasinDto> basins =
        jdbc.sql(sql)
            .query(
                (rs, rowNum) -> {
                  java.sql.Array arr = rs.getArray("district_ids");
                  List<UUID> districtIds = parseUuidArray(arr);
                  return new RiverBasinDto(
                      rs.getObject("id", UUID.class),
                      rs.getString("code"),
                      rs.getString("name"),
                      districtIds);
                })
            .list();
    return ApiResponse.of(basins);
  }

  @GetMapping("/hazard-types")
  ApiResponse<List<HazardTypeDto>> hazardTypes(
      @RequestParam(name = "activeOnly", required = false, defaultValue = "false")
          boolean activeOnly) {
    String sql =
        activeOnly
            ? "select id, code, name, onset_speed, report_categories, active from hazard_types where active = true order by name"
            : "select id, code, name, onset_speed, report_categories, active from hazard_types order by name";
    List<HazardTypeDto> hazardTypes =
        jdbc.sql(sql)
            .query(
                (rs, rowNum) -> {
                  java.sql.Array arr = rs.getArray("report_categories");
                  List<String> categories = parseStringArray(arr);
                  return new HazardTypeDto(
                      rs.getObject("id", UUID.class),
                      rs.getString("code"),
                      rs.getString("name"),
                      rs.getString("onset_speed"),
                      categories,
                      rs.getBoolean("active"));
                })
            .list();
    return ApiResponse.of(hazardTypes);
  }

  @GetMapping("/organisations")
  ApiResponse<List<OrganisationDto>> organisations(
      @RequestParam(name = "type", required = false) String type) {
    if (type != null && !type.isBlank()) {
      return ApiResponse.of(
          jdbc.sql("select id, name, type from organisations where type = :type order by name")
              .param("type", type)
              .query(OrganisationDto.class)
              .list());
    }
    return ApiResponse.of(
        jdbc.sql("select id, name, type from organisations order by name")
            .query(OrganisationDto.class)
            .list());
  }

  @GetMapping("/relief-items")
  ApiResponse<List<ReliefItemDto>> reliefItems() {
    List<ReliefItemDto> items =
        jdbc.sql("select id, code, name, unit, category from relief_items order by name")
            .query(ReliefItemDto.class)
            .list();
    return ApiResponse.of(items);
  }

  @GetMapping("/events")
  ApiResponse<List<DisasterEventDto>> events(
      @RequestParam(name = "status", required = false) String status) {
    StringBuilder sql =
        new StringBuilder(
            """
        select de.id, de.name, de.hazard_type_id, de.status, de.started_at, de.ended_at,
               coalesce(array_agg(ed.district_id) filter (where ed.district_id is not null), '{}') as district_ids
        from disaster_events de
        left join event_districts ed on de.id = ed.event_id
        """);
    boolean filterStatus = status != null && !status.isBlank();
    if (filterStatus) {
      sql.append(" where de.status = :status");
    }
    sql.append(
        """
        group by de.id, de.name, de.hazard_type_id, de.status, de.started_at, de.ended_at
        order by de.started_at desc
        """);

    var client = jdbc.sql(sql.toString());
    if (filterStatus) {
      client = client.param("status", status);
    }
    List<DisasterEventDto> events =
        client
            .query(
                (rs, rowNum) -> {
                  java.sql.Array arr = rs.getArray("district_ids");
                  List<UUID> districtIds = parseUuidArray(arr);
                  var started = rs.getTimestamp("started_at");
                  var ended = rs.getTimestamp("ended_at");
                  return new DisasterEventDto(
                      rs.getObject("id", UUID.class),
                      rs.getString("name"),
                      rs.getObject("hazard_type_id", UUID.class),
                      rs.getString("status"),
                      started != null ? started.toInstant() : null,
                      ended != null ? ended.toInstant() : null,
                      districtIds);
                })
            .list();
    return ApiResponse.of(events);
  }

  private static List<UUID> parseUuidArray(java.sql.Array arr) {
    List<UUID> list = new ArrayList<>();
    if (arr != null) {
      try {
        Object arrayObj = arr.getArray();
        if (arrayObj instanceof Object[] objs) {
          for (Object o : objs) {
            if (o instanceof UUID u) {
              list.add(u);
            } else if (o != null) {
              list.add(UUID.fromString(o.toString()));
            }
          }
        }
      } catch (Exception ignored) {
      }
    }
    return list;
  }

  private static List<String> parseStringArray(java.sql.Array arr) {
    List<String> list = new ArrayList<>();
    if (arr != null) {
      try {
        Object arrayObj = arr.getArray();
        if (arrayObj instanceof Object[] objs) {
          for (Object o : objs) {
            if (o != null) {
              list.add(o.toString());
            }
          }
        }
      } catch (Exception ignored) {
      }
    }
    return list;
  }
}
