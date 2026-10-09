package lk.dmc.disaster.analytics.query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.analytics.entity.ReportContext;
import lk.dmc.disaster.analytics.query.ContextFilters.Filter;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Shelter occupancy logged during an event, for the chosen districts and window: every reading in
 * time order, and each shelter's highest reading (the earliest one when it was reached twice).
 */
@Component
@Transactional(readOnly = true)
public class OccupancyQuery {

  private final JdbcClient jdbcClient;

  public OccupancyQuery(JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  public List<ShelterSeriesRecord> getSeries(ReportContext context) {
    Filter filter = filter(context);
    String sql =
        "SELECT s.id AS shelterId, s.name AS shelterName, s.district_id AS districtId, s.capacity, "
            + "o.recorded_at AS recordedAt, o.occupancy "
            + "FROM occupancy_logs o "
            + "JOIN shelters s ON o.shelter_id = s.id "
            + "WHERE o.event_id = :eventId "
            + filter.sql()
            + "ORDER BY s.id, o.recorded_at";
    return ContextFilters.statement(jdbcClient, sql, context, filter)
        .query(ShelterSeriesRecord.class)
        .list();
  }

  public List<ShelterPeakRecord> getPeaks(ReportContext context) {
    Filter filter = filter(context);
    String sql =
        "WITH max_logs AS ( "
            + "  SELECT s.id AS shelterId, s.capacity, o.occupancy AS peakOccupancy, "
            + "  o.recorded_at AS peakAt, "
            + "  ROW_NUMBER() OVER (PARTITION BY s.id ORDER BY o.occupancy DESC, o.recorded_at ASC) AS rn "
            + "  FROM occupancy_logs o "
            + "  JOIN shelters s ON o.shelter_id = s.id "
            + "  WHERE o.event_id = :eventId "
            + filter.sql()
            + ") "
            + "SELECT shelterId, capacity, peakOccupancy, peakAt "
            + "FROM max_logs WHERE rn = 1 ORDER BY shelterId";
    return ContextFilters.statement(jdbcClient, sql, context, filter)
        .query(ShelterPeakRecord.class)
        .list();
  }

  private static Filter filter(ReportContext context) {
    return ContextFilters.of(context, "o.recorded_at", "s.district_id");
  }

  public record ShelterSeriesRecord(
      UUID shelterId,
      String shelterName,
      UUID districtId,
      int capacity,
      Instant recordedAt,
      int occupancy) {}

  public record ShelterPeakRecord(UUID shelterId, int capacity, int peakOccupancy, Instant peakAt) {}
}
