package lk.dmc.disaster.analytics.web;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.shared.api.ApiResponse;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

  public record AnalyticsEventDto(
      UUID id,
      String name,
      String status,
      Instant startedAt,
      Instant endedAt,
      List<UUID> districtIds,
      long warningCount,
      long linkedReportCount) {}

  public record ReportSummaryDto(
      UUID id,
      UUID eventId,
      Instant generatedAt,
      UUID generatedBy) {}

  private final JdbcClient jdbc;

  public AnalyticsController(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @GetMapping("/events")
  public ApiResponse<List<AnalyticsEventDto>> events() {
    String sql = """
        SELECT de.id, de.name, de.status, de.started_at, de.ended_at,
               coalesce(array_agg(ed.district_id) filter (where ed.district_id is not null), '{}') as district_ids,
               (select count(*) from warnings w where w.event_id = de.id) as warning_count,
               (select count(*) from disaster_reports dr where dr.event_id = de.id) as linked_report_count
        FROM disaster_events de
        LEFT JOIN event_districts ed ON de.id = ed.event_id
        GROUP BY de.id, de.name, de.status, de.started_at, de.ended_at
        ORDER BY de.started_at DESC
        """;
    List<AnalyticsEventDto> list =
        jdbc.sql(sql)
            .query((rs, rowNum) -> {
              List<UUID> districtIds = parseUuidArray(rs.getArray("district_ids"));
              var started = rs.getTimestamp("started_at");
              var ended = rs.getTimestamp("ended_at");
              return new AnalyticsEventDto(
                  rs.getObject("id", UUID.class),
                  rs.getString("name"),
                  rs.getString("status"),
                  started != null ? started.toInstant() : null,
                  ended != null ? ended.toInstant() : null,
                  districtIds,
                  rs.getLong("warning_count"),
                  rs.getLong("linked_report_count"));
            })
            .list();
    return ApiResponse.of(list);
  }

  @GetMapping("/reports")
  public ApiResponse<List<ReportSummaryDto>> reports() {
    String sql = """
        SELECT id, event_id, generated_at, generated_by
        FROM disaster_reports
        ORDER BY generated_at DESC
        """;
    List<ReportSummaryDto> list =
        jdbc.sql(sql)
            .query((rs, rowNum) -> {
              var genAt = rs.getTimestamp("generated_at");
              return new ReportSummaryDto(
                  rs.getObject("id", UUID.class),
                  rs.getObject("event_id", UUID.class),
                  genAt != null ? genAt.toInstant() : Instant.now(),
                  rs.getObject("generated_by", UUID.class));
            })
            .list();
    return ApiResponse.of(list);
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

