package lk.dmc.disaster.response.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
import lk.dmc.disaster.shared.actor.ActingUser;
import lk.dmc.disaster.shared.actor.ActingUserContext;
import lk.dmc.disaster.shared.domain.Role;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;

@ExtendWith(MockitoExtension.class)
class ReliefSuppliesServiceImplTest {

  @Mock private ReliefStockRepository stocks;
  @Mock private ResourceAllocationRepository allocations;
  @Mock private ReliefDistributionRepository distributions;
  @Mock private ActingUserContext actingUser;
  @Mock private ReliefSuppliesScreenValidator validator;
  @Mock private JdbcClient jdbc;
  @Mock private JdbcClient.StatementSpec statementSpec;
  @Mock private JdbcClient.MappedQuerySpec<ReliefStockDto> mappedQuerySpec;

  private ReliefSuppliesServiceImpl service;

  @BeforeEach
  void setUp() {
    service =
        new ReliefSuppliesServiceImpl(
            stocks, allocations, distributions, actingUser, validator, jdbc);
  }

  @Test
  void allocate_valid_validatesAllocatesDecreasesStockAndSaves() {
    UUID stockId = UUID.randomUUID();
    UUID shelterId = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID districtId = UUID.randomUUID();

    ActingUser user = new ActingUser(userId, Role.DISTRICT_OFFICER, districtId, null, null);
    when(actingUser.require()).thenReturn(user);

    ReliefStock stock = ReliefStock.create(UUID.randomUUID(), UUID.randomUUID(), districtId, 100);
    when(validator.validateAllocation(any(), eq(districtId))).thenReturn(stock);
    when(allocations.save(any(ResourceAllocation.class))).thenAnswer(inv -> inv.getArgument(0));

    ReliefSuppliesService.CreateAllocationCommand command =
        new ReliefSuppliesService.CreateAllocationCommand(stockId, shelterId, eventId, 30, userId);

    AllocationDto dto = service.allocate(command);

    assertThat(dto).isNotNull();
    assertThat(dto.quantity()).isEqualTo(30);
    assertThat(stock.getQuantityAvailable()).isEqualTo(70);

    verify(validator).validateAllocation(any(), eq(districtId));
    verify(stocks).save(stock);
    verify(allocations).save(any(ResourceAllocation.class));
  }

  @Test
  void recordDistribution_allocationNotFound_throwsNotFound() {
    UUID allocationId = UUID.randomUUID();
    when(allocations.findById(allocationId)).thenReturn(Optional.empty());

    ReliefSuppliesService.CreateDistributionCommand command =
        new ReliefSuppliesService.CreateDistributionCommand(
            10, Instant.now(), null, false, UUID.randomUUID());

    assertThatThrownBy(() -> service.recordDistribution(allocationId, command))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.NOT_FOUND);
  }

  @Test
  void recordDistribution_valid_recordsMarksDistributedAndSaves() {
    UUID allocationId = UUID.randomUUID();
    UUID distributedBy = UUID.randomUUID();
    UUID clientRef = UUID.randomUUID();
    Instant distributedAt = Instant.now().minusSeconds(60);

    ResourceAllocation allocation =
        ResourceAllocation.create(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 50, distributedBy);
    when(allocations.findById(allocationId)).thenReturn(Optional.of(allocation));
    when(allocations.save(any(ResourceAllocation.class))).thenAnswer(inv -> inv.getArgument(0));

    ReliefSuppliesService.CreateDistributionCommand command =
        new ReliefSuppliesService.CreateDistributionCommand(
            50, distributedAt, clientRef, true, distributedBy);

    DistributionDto dto = service.recordDistribution(allocationId, command);

    assertThat(dto).isNotNull();
    assertThat(dto.quantityDistributed()).isEqualTo(50);
    assertThat(dto.recordedOffline()).isTrue();
    assertThat(dto.clientRef()).isEqualTo(clientRef);
    assertThat(allocation.getStatus()).isEqualTo(AllocationStatus.DISTRIBUTED);

    verify(validator).validateDistribution(eq(allocation), any());
    verify(distributions).save(any(ReliefDistribution.class));
    verify(allocations).save(allocation);

    // Also test with null distributedAt
    ReliefSuppliesService.CreateDistributionCommand command2 =
        new ReliefSuppliesService.CreateDistributionCommand(10, null, null, false, distributedBy);
    DistributionDto dto2 = service.recordDistribution(allocationId, command2);
    assertThat(dto2).isNotNull();
  }

  @Test
  void listAllocations_filtersByShelterEventOrAll() {
    UUID shelterId = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();

    ResourceAllocation a1 =
        ResourceAllocation.create(UUID.randomUUID(), shelterId, eventId, 10, UUID.randomUUID());
    ResourceAllocation a2 =
        ResourceAllocation.create(
            UUID.randomUUID(), shelterId, UUID.randomUUID(), 20, UUID.randomUUID());

    when(allocations.findByShelterId(shelterId)).thenReturn(List.of(a1, a2));
    when(allocations.findByEventId(eventId)).thenReturn(List.of(a1));
    when(allocations.findAll()).thenReturn(List.of(a1, a2));

    List<AllocationDto> byShelter = service.listAllocations(shelterId, null);
    assertThat(byShelter).hasSize(2);

    List<AllocationDto> byEvent = service.listAllocations(null, eventId);
    assertThat(byEvent).hasSize(1);

    List<AllocationDto> all = service.listAllocations(null, null);
    assertThat(all).hasSize(2);
  }

  @Test
  void getAllocations_pageableQuery() {
    UUID shelterId = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();
    ResourceAllocation a =
        ResourceAllocation.create(UUID.randomUUID(), shelterId, eventId, 15, UUID.randomUUID());

    when(allocations.findByShelterIdOrEventId(eq(shelterId), eq(eventId), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(a)));

    Page<AllocationDto> result = service.getAllocations(shelterId, eventId, 0, 10);
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).quantity()).isEqualTo(15);
  }

  @Test
  void getStocks_queriesViaJdbcClient() {
    UUID districtId = UUID.randomUUID();
    UUID itemId = UUID.randomUUID();

    ReliefStockDto dto =
        new ReliefStockDto(
            UUID.randomUUID(),
            itemId,
            "Blankets",
            "PACK",
            UUID.randomUUID(),
            "Red Cross",
            districtId,
            100,
            0L,
            Instant.now(),
            Instant.now());

    when(jdbc.sql(anyString())).thenReturn(statementSpec);
    when(statementSpec.param(eq("districtId"), eq(districtId))).thenReturn(statementSpec);
    when(statementSpec.param(eq("itemId"), eq(itemId))).thenReturn(statementSpec);
    when(statementSpec.query(any(RowMapper.class))).thenReturn(mappedQuerySpec);
    when(mappedQuerySpec.list()).thenReturn(List.of(dto));

    List<ReliefStockDto> result = service.getStocks(districtId, itemId);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).itemName()).isEqualTo("Blankets");

    // Test getStocks with null parameters
    when(jdbc.sql(anyString())).thenReturn(statementSpec);
    when(statementSpec.query(any(RowMapper.class))).thenReturn(mappedQuerySpec);
    List<ReliefStockDto> allStocks = service.getStocks(null, null);
    assertThat(allStocks).hasSize(1);
  }
}
