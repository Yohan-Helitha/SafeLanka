package lk.dmc.disaster.analytics.query;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lk.dmc.disaster.analytics.entity.ReportContext;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * The optional conditions every section query shares: a time window and a set of districts. A
 * condition that was not asked for adds nothing, so the same query serves a whole-event report and
 * a narrowed one.
 */
final class ContextFilters {

  /** SQL to append to a WHERE clause (each condition starts with AND) and its named parameters. */
  record Filter(String sql, Map<String, Object> params) {

    static final Filter NONE = new Filter("", Map.of());

    Filter and(Filter other) {
      Map<String, Object> merged = new LinkedHashMap<>(params);
      merged.putAll(other.params);
      return new Filter(sql + other.sql, merged);
    }
  }

  private ContextFilters() {}

  /** {@code column} between the report's start and end, whichever of the two is set (inclusive). */
  static Filter window(ReportContext context, String column) {
    Filter filter = Filter.NONE;
    if (context.fromTime() != null) {
      filter =
          filter.and(
              new Filter(
                  "AND " + column + " >= :fromTime ",
                  Map.of("fromTime", OffsetDateTime.ofInstant(context.fromTime(), ZoneOffset.UTC))));
    }
    if (context.toTime() != null) {
      filter =
          filter.and(
              new Filter(
                  "AND " + column + " <= :toTime ",
                  Map.of("toTime", OffsetDateTime.ofInstant(context.toTime(), ZoneOffset.UTC))));
    }
    return filter;
  }

  /** {@code column} is one of the report's districts; nothing when the report is not narrowed. */
  static Filter districts(ReportContext context, String column) {
    if (context.isGlobal()) {
      return Filter.NONE;
    }
    return new Filter(
        "AND " + column + " = ANY(CAST(:districtIds AS uuid[])) ",
        Map.of("districtIds", context.districtIds().toArray(new UUID[0])));
  }

  /** Time window on {@code timeColumn} and districts on {@code districtColumn}. */
  static Filter of(ReportContext context, String timeColumn, String districtColumn) {
    return window(context, timeColumn).and(districts(context, districtColumn));
  }

  /** A statement with the event id and the filter's parameters bound. */
  static JdbcClient.StatementSpec statement(
      JdbcClient jdbc, String sql, ReportContext context, Filter filter) {
    return jdbc.sql(sql).param("eventId", context.eventId()).params(filter.params());
  }
}
