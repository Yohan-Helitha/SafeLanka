package lk.dmc.disaster.response.service;

import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.shared.actor.ActingUserContext;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import lk.dmc.disaster.response.dto.request.AllocationRequest;
import lk.dmc.disaster.response.dto.request.DistributionRequest;
import lk.dmc.disaster.response.dto.response.AllocationDto;
import lk.dmc.disaster.response.dto.response.DistributionDto;
import lk.dmc.disaster.response.dto.response.ReliefStockDto;
import lk.dmc.disaster.response.entity.AllocationStatus;
import lk.dmc.disaster.response.entity.ReliefDistribution;
import lk.dmc.disaster.response.entity.ReliefStock;
import lk.dmc.disaster.response.entity.ResourceAllocation;
import lk.dmc.disaster.response.repository.ReliefDistributionRepository;
import lk.dmc.disaster.response.repository.ReliefStockRepository;
import lk.dmc.disaster.response.repository.ResourceAllocationRepository;
import lk.dmc.disaster.response.validation.ReliefSuppliesScreenValidator;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class ReliefSuppliesServiceImpl implements ReliefSuppliesService {

  private final ReliefStockRepository stocks;
  private final ResourceAllocationRepository allocations;
  private final ReliefDistributionRepository distributions;
  private final ActingUserContext actingUser;
  private final ReliefSuppliesScreenValidator validator;
  private final JdbcClient jdbc;

  ReliefSuppliesServiceImpl(
      ReliefStockRepository stocks,
      ResourceAllocationRepository allocations,
      ReliefDistributionRepository distributions,
      ActingUserContext actingUser,
      ReliefSuppliesScreenValidator validator,
      JdbcClient jdbc) {
    this.stocks = stocks;
    this.allocations = allocations;
    this.distributions = distributions;
    this.actingUser = actingUser;
    this.validator = validator;
    this.jdbc = jdbc;
  }

  @Override
  public AllocationDto allocate(CreateAllocationCommand command) {
    var user = actingUser.require();
    AllocationRequest req = new AllocationRequest(
        command.stockId(), command.shelterId(), command.eventId(), command.quantity());
    ReliefStock stock = validator.validateAllocation(req, user.districtId());

    ResourceAllocation a = ResourceAllocation.create(
        command.stockId(), command.shelterId(), command.eventId(),
        command.quantity(), command.allocatedBy());
    ResourceAllocation saved = allocations.save(a);

    stock.decrease(command.quantity());
    stocks.save(stock);

    return AllocationDto.from(saved);
  }

  @Override
  public DistributionDto recordDistribution(UUID allocationId, CreateDistributionCommand command) {
    ResourceAllocation allocation = allocations.findById(allocationId)
        .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Allocation not found"));

    DistributionRequest req = new DistributionRequest(
        command.quantityDistributed(), command.distributedAt(), command.clientRef(), command.recordedOffline());
    validator.validateDistribution(allocation, req);

    ReliefDistribution d = ReliefDistribution.create(
        allocationId, command.quantityDistributed(), command.distributedBy(),
        command.distributedAt() != null ? command.distributedAt() : java.time.Instant.now(),
        command.recordedOffline(), command.clientRef());
    distributions.save(d);

    allocation.markDistributed();
    allocations.save(allocation);

    return DistributionDto.from(d);
  }


  @Override
  public List<ReliefStockDto> getStocks(UUID districtId, UUID itemId) {
    StringBuilder sql = new StringBuilder("""
        SELECT rs.id, rs.item_id, ri.name AS item_name, ri.unit,
               rs.organisation_id, o.name AS organisation_name,
               rs.district_id, rs.quantity_available, rs.version,
               rs.created_at, rs.updated_at
        FROM relief_stocks rs
        LEFT JOIN relief_items ri ON rs.item_id = ri.id
        LEFT JOIN organisations o ON rs.organisation_id = o.id
        WHERE 1=1
        """);
    if (districtId != null) {
      sql.append(" AND rs.district_id = :districtId");
    }
    if (itemId != null) {
      sql.append(" AND rs.item_id = :itemId");
    }
    sql.append(" ORDER BY ri.name, rs.quantity_available DESC");

    var client = jdbc.sql(sql.toString());
    if (districtId != null) {
      client = client.param("districtId", districtId);
    }
    if (itemId != null) {
      client = client.param("itemId", itemId);
    }
    return client
        .query((rs, rowNum) -> {
          var createdAt = rs.getTimestamp("created_at");
          var updatedAt = rs.getTimestamp("updated_at");
          return new ReliefStockDto(
              rs.getObject("id", UUID.class),
              rs.getObject("item_id", UUID.class),
              rs.getString("item_name"),
              rs.getString("unit"),
              rs.getObject("organisation_id", UUID.class),
              rs.getString("organisation_name"),
              rs.getObject("district_id", UUID.class),
              rs.getInt("quantity_available"),
              rs.getLong("version"),
              createdAt != null ? createdAt.toInstant() : null,
              updatedAt != null ? updatedAt.toInstant() : null);
        })
        .list();
  }

  @Override
  public List<AllocationDto> listAllocations(UUID shelterId, UUID eventId) {
    List<ResourceAllocation> list;
    if (shelterId != null) {
      list = allocations.findByShelterId(shelterId);
    } else if (eventId != null) {
      list = allocations.findByEventId(eventId);
    } else {
      list = allocations.findAll();
    }
    return list.stream().map(AllocationDto::from).toList();
  }

  @Override
  public Page<AllocationDto> getAllocations(UUID shelterId, UUID eventId, int page, int size) {
    var pageable = PageRequest.of(page, size);
    return allocations.findByShelterIdOrEventId(shelterId, eventId, pageable)
        .map(AllocationDto::from);
  }
}
