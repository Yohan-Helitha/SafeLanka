$ErrorActionPreference = "Stop"
$queryDir = "src/main/java/lk/dmc/disaster/analytics/query"
$domainDir = "src/main/java/lk/dmc/disaster/analytics/domain"
$sectionDir = "src/main/java/lk/dmc/disaster/analytics/section"

# 1. Update Domain Objects

Set-Content -Path "$domainDir/AlertTimeline.java" -Value @"
package lk.dmc.disaster.analytics.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AlertTimeline(
    List<TimelineEntry> entries,
    Instant firstVerifiedReportAt,
    Instant firstWarningAt,
    Long reportToWarningMinutes
) {
    public record TimelineEntry(
        UUID warningId,
        String level,
        String status,
        Instant issuedAt,
        UUID supersedesId,
        List<UUID> resolvedDistrictIds
    ) {}
}
"@

Set-Content -Path "$domainDir/CitizensReached.java" -Value @"
package lk.dmc.disaster.analytics.domain;

import java.util.List;
import java.util.UUID;

public record CitizensReached(
    long uniqueCitizensTargeted,
    long uniqueCitizensReached,
    double deliveryRate,
    List<ChannelStats> byChannel,
    List<DistrictStats> byDistrict
) {
    public record ChannelStats(String channel, long delivered, long failed) {}
    public record DistrictStats(UUID districtId, String districtName, long targeted, long reached) {}
}
"@

Set-Content -Path "$domainDir/ShelterOccupancy.java" -Value @"
package lk.dmc.disaster.analytics.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ShelterOccupancy(
    List<ShelterSeries> series,
    List<ShelterPeak> peaks
) {
    public record ShelterSeries(
        UUID shelterId,
        String shelterName,
        UUID districtId,
        int capacity,
        List<OccupancyPoint> points
    ) {}
    public record OccupancyPoint(Instant recordedAt, int occupancy) {}
    public record ShelterPeak(
        UUID shelterId,
        int peakOccupancy,
        int capacity,
        double peakRatio,
        Instant peakAt
    ) {}
}
"@

Set-Content -Path "$domainDir/ResourceDistribution.java" -Value @"
package lk.dmc.disaster.analytics.domain;

import java.util.List;
import java.util.UUID;

public record ResourceDistribution(
    List<DistrictDistribution> byDistrict,
    List<OrganisationTypeDistribution> byOrganisationType,
    List<ItemDistribution> byItem
) {
    public record DistrictDistribution(
        UUID districtId,
        String districtName,
        String itemCode,
        String unit,
        long allocated,
        long distributed
    ) {}
    public record OrganisationTypeDistribution(String type, long distributed) {}
    public record ItemDistribution(String itemCode, String unit, long distributed) {}
}
"@

# 2. Write Queries

Set-Content -Path "$queryDir/EventSummaryQuery.java" -Value @"
package lk.dmc.disaster.analytics.query;

import java.util.List;
import lk.dmc.disaster.analytics.domain.EventSummary;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class EventSummaryQuery {
    private final JdbcClient jdbcClient;
    public EventSummaryQuery(JdbcClient jdbcClient) { this.jdbcClient = jdbcClient; }

    public List<EventSummary> execute(String status) {
        String sql = "SELECT e.id, e.name, e.hazard_type_id AS hazardTypeId, e.status, e.started_at AS startedAt, e.ended_at AS endedAt, " +
                     "COALESCE((SELECT array_agg(district_id) FROM event_districts ed WHERE ed.event_id = e.id), '{}') AS districtIds, " +
                     "(SELECT count(*) FROM warnings w WHERE w.event_id = e.id) AS warningCount, " +
                     "(SELECT count(*) FROM hazard_reports r JOIN hazard_evidence he ON r.id = he.report_id JOIN hazards h ON he.hazard_id = h.id WHERE h.event_id = e.id AND r.status = 'VERIFIED') AS reportCount " +
                     "FROM disaster_events e " +
                     "WHERE (:status IS NULL OR e.status = :status) " +
                     "ORDER BY e.started_at DESC";
        return jdbcClient.sql(sql).param("status", status).query(EventSummary.class).list();
    }
}
"@

Set-Content -Path "$queryDir/WarningTimelineQuery.java" -Value @"
package lk.dmc.disaster.analytics.query;

import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.analytics.domain.AlertTimeline.TimelineEntry;
import lk.dmc.disaster.analytics.domain.ReportContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class WarningTimelineQuery {
    private final JdbcClient jdbcClient;
    public WarningTimelineQuery(JdbcClient jdbcClient) { this.jdbcClient = jdbcClient; }

    public List<TimelineEntry> execute(ReportContext context) {
        String sql = "WITH target_basins AS ( " +
                     "  SELECT warning_id, river_basin_id FROM warning_target_areas WHERE river_basin_id IS NOT NULL " +
                     "), target_districts AS ( " +
                     "  SELECT warning_id, district_id FROM warning_target_areas WHERE district_id IS NOT NULL " +
                     "  UNION " +
                     "  SELECT tb.warning_id, drb.district_id FROM target_basins tb JOIN district_river_basins drb ON tb.river_basin_id = drb.river_basin_id " +
                     ") " +
                     "SELECT w.id AS warningId, w.level, w.status, w.issued_at AS issuedAt, w.supersedes_id AS supersedesId, " +
                     "COALESCE((SELECT array_agg(DISTINCT td.district_id) FROM target_districts td WHERE td.warning_id = w.id), '{}') AS resolvedDistrictIds " +
                     "FROM warnings w " +
                     "WHERE w.event_id = :eventId " +
                     "AND (:fromTime IS NULL OR w.issued_at >= :fromTime) " +
                     "AND (:toTime IS NULL OR w.issued_at <= :toTime) " +
                     "AND (COALESCE(array_length(:districtIds, 1), 0) = 0 OR EXISTS (SELECT 1 FROM target_districts td WHERE td.warning_id = w.id AND td.district_id = ANY(:districtIds))) " +
                     "ORDER BY w.issued_at ASC";
        
        return jdbcClient.sql(sql)
            .param("eventId", context.eventId())
            .param("fromTime", context.fromTime())
            .param("toTime", context.toTime())
            .param("districtIds", context.districtIds() == null ? new UUID[0] : context.districtIds().toArray(new UUID[0]))
            .query(TimelineEntry.class).list();
    }
}
"@

Set-Content -Path "$queryDir/ReportTimingQuery.java" -Value @"
package lk.dmc.disaster.analytics.query;

import java.time.Instant;
import java.util.Optional;
import lk.dmc.disaster.analytics.domain.ReportContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class ReportTimingQuery {
    private final JdbcClient jdbcClient;
    public ReportTimingQuery(JdbcClient jdbcClient) { this.jdbcClient = jdbcClient; }

    public Optional<Instant> execute(ReportContext context) {
        String sql = "SELECT MIN(hr.reviewed_at) " +
                     "FROM hazard_reports hr " +
                     "JOIN hazard_evidence he ON hr.id = he.report_id " +
                     "JOIN hazards h ON he.hazard_id = h.id " +
                     "WHERE h.event_id = :eventId AND hr.status = 'VERIFIED'";
        return jdbcClient.sql(sql)
            .param("eventId", context.eventId())
            .query(Instant.class)
            .optional();
    }
}
"@

Set-Content -Path "$queryDir/DeliveryStatsQuery.java" -Value @"
package lk.dmc.disaster.analytics.query;

import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.analytics.domain.CitizensReached.ChannelStats;
import lk.dmc.disaster.analytics.domain.CitizensReached.DistrictStats;
import lk.dmc.disaster.analytics.domain.ReportContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class DeliveryStatsQuery {
    private final JdbcClient jdbcClient;
    public DeliveryStatsQuery(JdbcClient jdbcClient) { this.jdbcClient = jdbcClient; }

    public long getUniqueTargeted(ReportContext context) {
        String sql = "SELECT COUNT(DISTINCT nd.citizen_id) FROM notification_deliveries nd " +
                     "JOIN warnings w ON nd.warning_id = w.id " +
                     "JOIN users u ON nd.citizen_id = u.id " +
                     "WHERE w.event_id = :eventId " +
                     "AND (:fromTime IS NULL OR nd.attempted_at >= :fromTime) " +
                     "AND (:toTime IS NULL OR nd.attempted_at <= :toTime) " +
                     "AND (COALESCE(array_length(:districtIds, 1), 0) = 0 OR u.district_id = ANY(:districtIds))";
        return queryLong(sql, context);
    }

    public long getUniqueReached(ReportContext context) {
        String sql = "SELECT COUNT(DISTINCT nd.citizen_id) FROM notification_deliveries nd " +
                     "JOIN warnings w ON nd.warning_id = w.id " +
                     "JOIN users u ON nd.citizen_id = u.id " +
                     "WHERE w.event_id = :eventId AND nd.status = 'DELIVERED' " +
                     "AND (:fromTime IS NULL OR nd.attempted_at >= :fromTime) " +
                     "AND (:toTime IS NULL OR nd.attempted_at <= :toTime) " +
                     "AND (COALESCE(array_length(:districtIds, 1), 0) = 0 OR u.district_id = ANY(:districtIds))";
        return queryLong(sql, context);
    }

    public List<ChannelStats> getChannelStats(ReportContext context) {
        String sql = "SELECT nd.channel, " +
                     "COUNT(CASE WHEN nd.status = 'DELIVERED' THEN 1 END) AS delivered, " +
                     "COUNT(CASE WHEN nd.status != 'DELIVERED' THEN 1 END) AS failed " +
                     "FROM notification_deliveries nd " +
                     "JOIN warnings w ON nd.warning_id = w.id " +
                     "JOIN users u ON nd.citizen_id = u.id " +
                     "WHERE w.event_id = :eventId " +
                     "AND (:fromTime IS NULL OR nd.attempted_at >= :fromTime) " +
                     "AND (:toTime IS NULL OR nd.attempted_at <= :toTime) " +
                     "AND (COALESCE(array_length(:districtIds, 1), 0) = 0 OR u.district_id = ANY(:districtIds)) " +
                     "GROUP BY nd.channel ORDER BY nd.channel";
        return jdbcClient.sql(sql)
            .param("eventId", context.eventId())
            .param("fromTime", context.fromTime())
            .param("toTime", context.toTime())
            .param("districtIds", context.districtIds() == null ? new UUID[0] : context.districtIds().toArray(new UUID[0]))
            .query(ChannelStats.class).list();
    }

    public List<DistrictStats> getDistrictStats(ReportContext context) {
        String sql = "SELECT u.district_id AS districtId, d.name AS districtName, " +
                     "COUNT(DISTINCT nd.citizen_id) AS targeted, " +
                     "COUNT(DISTINCT CASE WHEN nd.status = 'DELIVERED' THEN nd.citizen_id END) AS reached " +
                     "FROM notification_deliveries nd " +
                     "JOIN warnings w ON nd.warning_id = w.id " +
                     "JOIN users u ON nd.citizen_id = u.id " +
                     "JOIN districts d ON u.district_id = d.id " +
                     "WHERE w.event_id = :eventId " +
                     "AND (:fromTime IS NULL OR nd.attempted_at >= :fromTime) " +
                     "AND (:toTime IS NULL OR nd.attempted_at <= :toTime) " +
                     "AND (COALESCE(array_length(:districtIds, 1), 0) = 0 OR u.district_id = ANY(:districtIds)) " +
                     "GROUP BY u.district_id, d.name ORDER BY d.name";
        return jdbcClient.sql(sql)
            .param("eventId", context.eventId())
            .param("fromTime", context.fromTime())
            .param("toTime", context.toTime())
            .param("districtIds", context.districtIds() == null ? new UUID[0] : context.districtIds().toArray(new UUID[0]))
            .query(DistrictStats.class).list();
    }

    private long queryLong(String sql, ReportContext context) {
        Long val = jdbcClient.sql(sql)
            .param("eventId", context.eventId())
            .param("fromTime", context.fromTime())
            .param("toTime", context.toTime())
            .param("districtIds", context.districtIds() == null ? new UUID[0] : context.districtIds().toArray(new UUID[0]))
            .query(Long.class).single();
        return val != null ? val : 0L;
    }
}
"@

Set-Content -Path "$queryDir/OccupancyQuery.java" -Value @"
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
"@

Set-Content -Path "$queryDir/DistributionQuery.java" -Value @"
package lk.dmc.disaster.analytics.query;

import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.analytics.domain.ReportContext;
import lk.dmc.disaster.analytics.domain.ResourceDistribution.DistrictDistribution;
import lk.dmc.disaster.analytics.domain.ResourceDistribution.ItemDistribution;
import lk.dmc.disaster.analytics.domain.ResourceDistribution.OrganisationTypeDistribution;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class DistributionQuery {
    private final JdbcClient jdbcClient;
    public DistributionQuery(JdbcClient jdbcClient) { this.jdbcClient = jdbcClient; }

    public List<DistrictDistribution> getByDistrict(ReportContext context) {
        String sql = "SELECT s.district_id AS districtId, d.name AS districtName, ri.code AS itemCode, ri.unit, " +
                     "SUM(ra.quantity) AS allocated, " +
                     "COALESCE(SUM(rd.quantity_distributed), 0) AS distributed " +
                     "FROM resource_allocations ra " +
                     "JOIN shelters s ON ra.shelter_id = s.id " +
                     "JOIN districts d ON s.district_id = d.id " +
                     "JOIN relief_stocks rs ON ra.stock_id = rs.id " +
                     "JOIN relief_items ri ON rs.item_id = ri.id " +
                     "LEFT JOIN relief_distributions rd ON ra.id = rd.allocation_id " +
                     "WHERE ra.event_id = :eventId " +
                     "AND (:fromTime IS NULL OR ra.allocated_at >= :fromTime) " +
                     "AND (:toTime IS NULL OR ra.allocated_at <= :toTime) " +
                     "AND (COALESCE(array_length(:districtIds, 1), 0) = 0 OR s.district_id = ANY(:districtIds)) " +
                     "GROUP BY s.district_id, d.name, ri.code, ri.unit ORDER BY d.name, ri.code";
        return query(sql, context, DistrictDistribution.class);
    }

    public List<OrganisationTypeDistribution> getByOrganisationType(ReportContext context) {
        String sql = "SELECT org.type, COALESCE(SUM(rd.quantity_distributed), 0) AS distributed " +
                     "FROM resource_allocations ra " +
                     "JOIN shelters s ON ra.shelter_id = s.id " +
                     "JOIN relief_stocks rs ON ra.stock_id = rs.id " +
                     "JOIN organisations org ON rs.organisation_id = org.id " +
                     "JOIN relief_distributions rd ON ra.id = rd.allocation_id " +
                     "WHERE ra.event_id = :eventId " +
                     "AND (:fromTime IS NULL OR ra.allocated_at >= :fromTime) " +
                     "AND (:toTime IS NULL OR ra.allocated_at <= :toTime) " +
                     "AND (COALESCE(array_length(:districtIds, 1), 0) = 0 OR s.district_id = ANY(:districtIds)) " +
                     "GROUP BY org.type ORDER BY org.type";
        return query(sql, context, OrganisationTypeDistribution.class);
    }

    public List<ItemDistribution> getByItem(ReportContext context) {
        String sql = "SELECT ri.code AS itemCode, ri.unit, COALESCE(SUM(rd.quantity_distributed), 0) AS distributed " +
                     "FROM resource_allocations ra " +
                     "JOIN shelters s ON ra.shelter_id = s.id " +
                     "JOIN relief_stocks rs ON ra.stock_id = rs.id " +
                     "JOIN relief_items ri ON rs.item_id = ri.id " +
                     "JOIN relief_distributions rd ON ra.id = rd.allocation_id " +
                     "WHERE ra.event_id = :eventId " +
                     "AND (:fromTime IS NULL OR ra.allocated_at >= :fromTime) " +
                     "AND (:toTime IS NULL OR ra.allocated_at <= :toTime) " +
                     "AND (COALESCE(array_length(:districtIds, 1), 0) = 0 OR s.district_id = ANY(:districtIds)) " +
                     "GROUP BY ri.code, ri.unit ORDER BY ri.code";
        return query(sql, context, ItemDistribution.class);
    }

    private <T> List<T> query(String sql, ReportContext context, Class<T> clazz) {
        return jdbcClient.sql(sql)
            .param("eventId", context.eventId())
            .param("fromTime", context.fromTime())
            .param("toTime", context.toTime())
            .param("districtIds", context.districtIds() == null ? new UUID[0] : context.districtIds().toArray(new UUID[0]))
            .query(clazz).list();
    }
}
"@

# 3. Write Sections

Set-Content -Path "$sectionDir/AlertTimelineSection.java" -Value @"
package lk.dmc.disaster.analytics.section;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lk.dmc.disaster.analytics.domain.AlertTimeline;
import lk.dmc.disaster.analytics.domain.ReportContext;
import lk.dmc.disaster.analytics.domain.SectionKey;
import lk.dmc.disaster.analytics.domain.SectionResult;
import lk.dmc.disaster.analytics.query.ReportTimingQuery;
import lk.dmc.disaster.analytics.query.WarningTimelineQuery;
import org.springframework.stereotype.Component;

@Component
public class AlertTimelineSection implements ReportSection {
    private final WarningTimelineQuery warningQuery;
    private final ReportTimingQuery timingQuery;

    public AlertTimelineSection(WarningTimelineQuery warningQuery, ReportTimingQuery timingQuery) {
        this.warningQuery = warningQuery;
        this.timingQuery = timingQuery;
    }

    @Override
    public SectionKey getKey() { return SectionKey.ALERT_TIMELINE; }

    @Override
    public SectionResult<?> generate(ReportContext context) {
        List<AlertTimeline.TimelineEntry> entries = warningQuery.execute(context);
        if (entries.isEmpty()) {
            return SectionResult.unavailable(getKey(), "No warnings were issued for this event in the selected time window and districts.");
        }
        Instant firstVerified = timingQuery.execute(context).orElse(null);
        Instant firstWarning = entries.get(0).issuedAt();
        Long diff = (firstVerified != null) ? Duration.between(firstVerified, firstWarning).toMinutes() : null;
        return SectionResult.success(getKey(), new AlertTimeline(entries, firstVerified, firstWarning, diff));
    }
}
"@

Set-Content -Path "$sectionDir/CitizensReachedSection.java" -Value @"
package lk.dmc.disaster.analytics.section;

import lk.dmc.disaster.analytics.domain.CitizensReached;
import lk.dmc.disaster.analytics.domain.ReportContext;
import lk.dmc.disaster.analytics.domain.SectionKey;
import lk.dmc.disaster.analytics.domain.SectionResult;
import lk.dmc.disaster.analytics.query.DeliveryStatsQuery;
import org.springframework.stereotype.Component;

@Component
public class CitizensReachedSection implements ReportSection {
    private final DeliveryStatsQuery query;

    public CitizensReachedSection(DeliveryStatsQuery query) { this.query = query; }

    @Override
    public SectionKey getKey() { return SectionKey.CITIZENS_REACHED; }

    @Override
    public SectionResult<?> generate(ReportContext context) {
        long targeted = query.getUniqueTargeted(context);
        if (targeted == 0) {
            return SectionResult.unavailable(getKey(), "No notification deliveries were recorded for this event.");
        }
        long reached = query.getUniqueReached(context);
        double rate = (double) reached / targeted;
        // round to 2 decimals
        rate = Math.round(rate * 100.0) / 100.0;
        return SectionResult.success(getKey(), new CitizensReached(
            targeted, reached, rate, query.getChannelStats(context), query.getDistrictStats(context)
        ));
    }
}
"@

Set-Content -Path "$sectionDir/ShelterOccupancySection.java" -Value @"
package lk.dmc.disaster.analytics.section;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lk.dmc.disaster.analytics.domain.ReportContext;
import lk.dmc.disaster.analytics.domain.SectionKey;
import lk.dmc.disaster.analytics.domain.SectionResult;
import lk.dmc.disaster.analytics.domain.ShelterOccupancy;
import lk.dmc.disaster.analytics.domain.ShelterOccupancy.OccupancyPoint;
import lk.dmc.disaster.analytics.domain.ShelterOccupancy.ShelterPeak;
import lk.dmc.disaster.analytics.domain.ShelterOccupancy.ShelterSeries;
import lk.dmc.disaster.analytics.query.OccupancyQuery;
import org.springframework.stereotype.Component;

@Component
public class ShelterOccupancySection implements ReportSection {
    private final OccupancyQuery query;

    public ShelterOccupancySection(OccupancyQuery query) { this.query = query; }

    @Override
    public SectionKey getKey() { return SectionKey.SHELTER_OCCUPANCY; }

    @Override
    public SectionResult<?> generate(ReportContext context) {
        List<OccupancyQuery.ShelterSeriesRecord> records = query.getSeries(context);
        if (records.isEmpty()) {
            return SectionResult.unavailable(getKey(), "No shelter occupancy was logged for this event.");
        }
        Map<UUID, List<OccupancyQuery.ShelterSeriesRecord>> grouped = records.stream()
            .collect(Collectors.groupingBy(OccupancyQuery.ShelterSeriesRecord::shelterId));
            
        List<ShelterSeries> series = new ArrayList<>();
        for (var entry : grouped.entrySet()) {
            var r = entry.getValue().get(0);
            List<OccupancyPoint> points = entry.getValue().stream()
                .map(x -> new OccupancyPoint(x.recordedAt(), x.occupancy()))
                .toList();
            series.add(new ShelterSeries(r.shelterId(), r.shelterName(), r.districtId(), r.capacity(), points));
        }
        
        List<ShelterPeak> peaks = query.getPeaks(context).stream().map(p -> {
            double ratio = p.capacity() > 0 ? Math.round(((double) p.peakOccupancy() / p.capacity()) * 100.0) / 100.0 : 0.0;
            return new ShelterPeak(p.shelterId(), p.peakOccupancy(), p.capacity(), ratio, p.peakAt());
        }).toList();

        return SectionResult.success(getKey(), new ShelterOccupancy(series, peaks));
    }
}
"@

Set-Content -Path "$sectionDir/ResourceDistributionSection.java" -Value @"
package lk.dmc.disaster.analytics.section;

import java.util.List;
import lk.dmc.disaster.analytics.domain.ReportContext;
import lk.dmc.disaster.analytics.domain.ResourceDistribution;
import lk.dmc.disaster.analytics.domain.SectionKey;
import lk.dmc.disaster.analytics.domain.SectionResult;
import lk.dmc.disaster.analytics.query.DistributionQuery;
import org.springframework.stereotype.Component;

@Component
public class ResourceDistributionSection implements ReportSection {
    private final DistributionQuery query;

    public ResourceDistributionSection(DistributionQuery query) { this.query = query; }

    @Override
    public SectionKey getKey() { return SectionKey.RESOURCE_DISTRIBUTION; }

    @Override
    public SectionResult<?> generate(ReportContext context) {
        var byDistrict = query.getByDistrict(context);
        if (byDistrict.isEmpty()) {
            return SectionResult.unavailable(getKey(), "No resource allocations were found for this event.");
        }
        return SectionResult.success(getKey(), new ResourceDistribution(
            byDistrict, query.getByOrganisationType(context), query.getByItem(context)
        ));
    }
}
"@

Remove-Item "$queryDir/AlertTimelineQuery.java" -ErrorAction SilentlyContinue
Remove-Item "$queryDir/CitizensReachedQuery.java" -ErrorAction SilentlyContinue
Remove-Item "$queryDir/ResourceDistributionQuery.java" -ErrorAction SilentlyContinue
Remove-Item "$queryDir/ShelterOccupancyQuery.java" -ErrorAction SilentlyContinue

echo "Refactoring completed"
