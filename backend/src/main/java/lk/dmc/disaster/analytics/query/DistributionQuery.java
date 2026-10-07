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
