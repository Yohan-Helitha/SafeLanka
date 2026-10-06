package lk.dmc.disaster.warnings.controllers;

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

@RestController
@RequestMapping("/api/warnings")
public class WarningController {

  public record WarningListItemDto(
      UUID id,
      UUID hazardId,
      String level,
      String status,
      String title,
      List<UUID> districtIds,
      List<UUID> riverBasinIds,
      Instant issuedAt,
      int reached) {}

  private final JdbcClient jdbc;

  public WarningController(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @GetMapping
  public ApiResponse<List<WarningListItemDto>> list(
      @RequestParam(required = false) String status) {
    StringBuilder sql = new StringBuilder("""
        SELECT w.id, w.hazard_id, w.level, w.status, w.title, w.issued_at,
               coalesce(array_agg(wta.district_id) filter (where wta.district_id is not null), '{}') as district_ids,
               coalesce(array_agg(wta.river_basin_id) filter (where wta.river_basin_id is not null), '{}') as river_basin_ids,
               (select count(*) from notification_deliveries nd where nd.warning_id = w.id and nd.status = 'DELIVERED') as reached
        FROM warnings w
        LEFT JOIN warning_target_areas wta ON w.id = wta.warning_id
        """);
    boolean filterStatus = status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status);
    if (filterStatus) {
      sql.append(" WHERE w.status = :status");
    }
    sql.append("""
        GROUP BY w.id, w.hazard_id, w.level, w.status, w.title, w.issued_at
        ORDER BY w.issued_at DESC
        """);

    var client = jdbc.sql(sql.toString());
    if (filterStatus) {
      client = client.param("status", status);
    }
    List<WarningListItemDto> items = client
        .query((rs, rowNum) -> {
          var issuedAt = rs.getTimestamp("issued_at");
          List<UUID> districtIds = parseUuidArray(rs.getArray("district_ids"));
          List<UUID> riverBasinIds = parseUuidArray(rs.getArray("river_basin_ids"));
          return new WarningListItemDto(
              rs.getObject("id", UUID.class),
              rs.getObject("hazard_id", UUID.class),
              rs.getString("level"),
              rs.getString("status"),
              rs.getString("title"),
              districtIds,
              riverBasinIds,
              issuedAt != null ? issuedAt.toInstant() : Instant.now(),
              rs.getInt("reached"));
        })
        .list();
    return ApiResponse.of(items);
  }

  @GetMapping("/active/mine")
  public ApiResponse<List<WarningListItemDto>> myActiveAlerts() {
    return ApiResponse.of(List.of());
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
}
