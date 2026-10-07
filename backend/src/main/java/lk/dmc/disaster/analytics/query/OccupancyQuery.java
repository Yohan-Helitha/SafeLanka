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
        String sql = "SELECT s.id AS shelterId, s.name AS shelterName, s.district_id AS districtId, s.capacity, " +
                     "o.recorded_at AS recordedAt, o.occupancy " +
                     "FROM occupancy_logs o " +
                     "JOIN shelters s ON o.shelter_id = s.id " +
                     "WHERE o.event_id = :eventId " +
                     "AND (:fromTime IS NULL OR o.recorded_at >= :fromTime) " +
                     "AND (:toTime IS NULL OR o.recorded_at <= :toTime) " +
                     "AND (COALESCE(array_length(:districtIds, 1), 0) = 0 OR s.district_id = ANY(:districtIds)) " +
                     "ORDER BY s.id, o.recorded_at";
        return jdbcClient.sql(sql)
            .param("eventId", context.eventId())
            .param("fromTime", context.fromTime())
            .param("toTime", context.toTime())
            .param("districtIds", context.districtIds() == null ? new UUID[0] : context.districtIds().toArray(new UUID[0]))
            .query(ShelterSeriesRecord.class).list();
    }

    public List<ShelterPeakRecord> getPeaks(ReportContext context) {
        String sql = "WITH max_logs AS ( " +
                     "  SELECT s.id AS shelterId, s.capacity, o.occupancy AS peakOccupancy, o.recorded_at AS peakAt, " +
                     "  ROW_NUMBER() OVER (PARTITION BY s.id ORDER BY o.occupancy DESC, o.recorded_at ASC) as rn " +
                     "  FROM occupancy_logs o " +
                     "  JOIN shelters s ON o.shelter_id = s.id " +
                     "  WHERE o.event_id = :eventId " +
                     "  AND (:fromTime IS NULL OR o.recorded_at >= :fromTime) " +
                     "  AND (:toTime IS NULL OR o.recorded_at <= :toTime) " +
                     "  AND (COALESCE(array_length(:districtIds, 1), 0) = 0 OR s.district_id = ANY(:districtIds)) " +
                     ") " +
                     "SELECT shelterId, capacity, peakOccupancy, peakAt " +
                     "FROM max_logs WHERE rn = 1 ORDER BY shelterId";
        return jdbcClient.sql(sql)
            .param("eventId", context.eventId())
            .param("fromTime", context.fromTime())
            .param("toTime", context.toTime())
            .param("districtIds", context.districtIds() == null ? new UUID[0] : context.districtIds().toArray(new UUID[0]))
            .query(ShelterPeakRecord.class).list();
    }
    
    public record ShelterSeriesRecord(UUID shelterId, String shelterName, UUID districtId, int capacity, java.time.Instant recordedAt, int occupancy) {}
    public record ShelterPeakRecord(UUID shelterId, int capacity, int peakOccupancy, java.time.Instant peakAt) {}
}
