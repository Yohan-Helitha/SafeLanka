package lk.dmc.disaster.analytics.query;

import java.util.List;
import lk.dmc.disaster.analytics.entity.ReportContext;
import lk.dmc.disaster.analytics.entity.ResourceDistribution.DistrictDistribution;
import lk.dmc.disaster.analytics.entity.ResourceDistribution.ItemDistribution;
import lk.dmc.disaster.analytics.entity.ResourceDistribution.OrganisationTypeDistribution;
import lk.dmc.disaster.analytics.query.ContextFilters.Filter;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Relief allocated and distributed during an event, for the chosen districts and window: by
 * district and item, by the type of organisation that owned the stock, and by item.
 */
@Component
@Transactional(readOnly = true)
public class DistributionQuery {

  private final JdbcClient jdbcClient;

  public DistributionQuery(JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  /** An allocation that has no distributions yet counts as 0 distributed. */
  public List<DistrictDistribution> getByDistrict(ReportContext context) {
    Filter filter = filter(context);
    String sql =
        "SELECT s.district_id AS districtId, d.name AS districtName, ri.code AS itemCode, ri.unit, "
            + "SUM(ra.quantity) AS allocated, "
            + "COALESCE(SUM(rd.quantity_distributed), 0) AS distributed "
            + "FROM resource_allocations ra "
            + "JOIN shelters s ON ra.shelter_id = s.id "
            + "JOIN districts d ON s.district_id = d.id "
            + "JOIN relief_stocks rs ON ra.stock_id = rs.id "
            + "JOIN relief_items ri ON rs.item_id = ri.id "
            + "LEFT JOIN relief_distributions rd ON ra.id = rd.allocation_id "
            + "WHERE ra.event_id = :eventId "
            + filter.sql()
            + "GROUP BY s.district_id, d.name, ri.code, ri.unit ORDER BY d.name, ri.code";
    return ContextFilters.statement(jdbcClient, sql, context, filter)
        .query(DistrictDistribution.class)
        .list();
  }

  public List<OrganisationTypeDistribution> getByOrganisationType(ReportContext context) {
    Filter filter = filter(context);
    String sql =
        "SELECT org.type, COALESCE(SUM(rd.quantity_distributed), 0) AS distributed "
            + "FROM resource_allocations ra "
            + "JOIN shelters s ON ra.shelter_id = s.id "
            + "JOIN relief_stocks rs ON ra.stock_id = rs.id "
            + "JOIN organisations org ON rs.organisation_id = org.id "
            + "JOIN relief_distributions rd ON ra.id = rd.allocation_id "
            + "WHERE ra.event_id = :eventId "
            + filter.sql()
            + "GROUP BY org.type ORDER BY org.type";
    return ContextFilters.statement(jdbcClient, sql, context, filter)
        .query(OrganisationTypeDistribution.class)
        .list();
  }

  public List<ItemDistribution> getByItem(ReportContext context) {
    Filter filter = filter(context);
    String sql =
        "SELECT ri.code AS itemCode, ri.unit, "
            + "COALESCE(SUM(rd.quantity_distributed), 0) AS distributed "
            + "FROM resource_allocations ra "
            + "JOIN shelters s ON ra.shelter_id = s.id "
            + "JOIN relief_stocks rs ON ra.stock_id = rs.id "
            + "JOIN relief_items ri ON rs.item_id = ri.id "
            + "JOIN relief_distributions rd ON ra.id = rd.allocation_id "
            + "WHERE ra.event_id = :eventId "
            + filter.sql()
            + "GROUP BY ri.code, ri.unit ORDER BY ri.code";
    return ContextFilters.statement(jdbcClient, sql, context, filter)
        .query(ItemDistribution.class)
        .list();
  }

  private static Filter filter(ReportContext context) {
    return ContextFilters.of(context, "ra.allocated_at", "s.district_id");
  }
}
