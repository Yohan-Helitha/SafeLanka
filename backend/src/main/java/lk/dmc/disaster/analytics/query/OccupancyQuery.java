package lk.dmc.disaster.analytics.query;

import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.analytics.domain.ReportContext;
import lk.dmc.disaster.analytics.domain.ShelterOccupancy.OccupancyPoint;
import lk.dmc.disaster.analytics.domain.ShelterOccupancy.ShelterPeak;
import lk.dmc.disaster.analytics.domain.ShelterOccupancy.ShelterSeries;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class OccupancyQuery {
    private final JdbcClient jdbcClient;
    public OccupancyQuery(JdbcClient jdbcClient) { this.jdbcClient = jdbcClient; }

    public List<ShelterSeriesRecord> getSeries(ReportContext context) {
        StringBuilder sql = new StringBuilder("SELECT s.id AS shelterId, s.name AS shelterName, s.district_id AS districtId, s.capacity, " +
                     "o.recorded_at AS recordedAt, o.occupancy " +
                     "FROM occupancy_logs o " +
                     "JOIN shelters s ON o.shelter_id = s.id " +
                     "WHERE o.event_id = :eventId ");
        appendFilters(sql, context);
        sql.append("ORDER BY s.id, o.recorded_at");
        return query(sql.toString(), context, ShelterSeriesRecord.class);
    }

    public List<ShelterPeakRecord> getPeaks(ReportContext context) {
        StringBuilder sql = new StringBuilder("WITH max_logs AS ( " +
                     "  SELECT s.id AS shelterId, s.capacity, o.occupancy AS peakOccupancy, o.recorded_at AS peakAt, " +
                     "  ROW_NUMBER() OVER (PARTITION BY s.id ORDER BY o.occupancy DESC, o.recorded_at ASC) as rn " +
                     "  FROM occupancy_logs o " +
                     "  JOIN shelters s ON o.shelter_id = s.id " +
                     "  WHERE o.event_id = :eventId ");
        appendFilters(sql, context);
        sql.append(") " +
                   "SELECT shelterId, capacity, peakOccupancy, peakAt " +
                   "FROM max_logs WHERE rn = 1 ORDER BY shelterId");
        return query(sql.toString(), context, ShelterPeakRecord.class);
    }

    private void appendFilters(StringBuilder sql, ReportContext context) {
        if (context.fromTime() != null) sql.append("AND o.recorded_at >= :fromTime ");
        if (context.toTime() != null) sql.append("AND o.recorded_at <= :toTime ");
        if (context.districtIds() != null && !context.districtIds().isEmpty()) {
            sql.append("AND s.district_id = ANY(CAST(:districtIds AS uuid[])) ");
        }
    }

    private <T> List<T> query(String sql, ReportContext context, Class<T> clazz) {
        var query = jdbcClient.sql(sql).param("eventId", context.eventId());
        if (context.fromTime() != null) query = query.param("fromTime", java.time.OffsetDateTime.ofInstant(context.fromTime(), java.time.ZoneOffset.UTC));
        if (context.toTime() != null) query = query.param("toTime", java.time.OffsetDateTime.ofInstant(context.toTime(), java.time.ZoneOffset.UTC));
        if (context.districtIds() != null && !context.districtIds().isEmpty()) {
            query = query.param("districtIds", context.districtIds().toArray(new UUID[0]));
        }
        return query.query(clazz).list();
    }
    
    public record ShelterSeriesRecord(UUID shelterId, String shelterName, UUID districtId, int capacity, java.time.Instant recordedAt, int occupancy) {}
    public record ShelterPeakRecord(UUID shelterId, int capacity, int peakOccupancy, java.time.Instant peakAt) {}
}
