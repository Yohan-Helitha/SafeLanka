package lk.dmc.disaster.response.service;

import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.response.dto.response.AllocationDto;
import lk.dmc.disaster.response.dto.response.DistributionDto;
import lk.dmc.disaster.response.dto.response.ReliefStockDto;
import org.springframework.data.domain.Page;

public interface ReliefSuppliesService {

  AllocationDto allocate(CreateAllocationCommand command);

  DistributionDto recordDistribution(UUID allocationId, CreateDistributionCommand command);

  List<ReliefStockDto> getStocks(UUID districtId, UUID itemId);

  List<AllocationDto> listAllocations(UUID shelterId, UUID eventId);

  Page<AllocationDto> getAllocations(UUID shelterId, UUID eventId, int page, int size);

  record CreateAllocationCommand(
      UUID stockId, UUID shelterId, UUID eventId, int quantity, UUID allocatedBy) {}

  record CreateDistributionCommand(
      int quantityDistributed,
      java.time.Instant distributedAt,
      UUID clientRef,
      boolean recordedOffline,
      UUID distributedBy) {}
}
