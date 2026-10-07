package lk.dmc.disaster.response.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.response.dto.response.AssignmentDto;
import lk.dmc.disaster.response.dto.response.DistrictDashboard;
import lk.dmc.disaster.response.dto.response.ReliefStockDto;
import lk.dmc.disaster.response.entity.ActivityLog;
import lk.dmc.disaster.response.entity.ActivityType;
import lk.dmc.disaster.response.entity.AssignmentStatus;
import lk.dmc.disaster.response.entity.ReliefStock;
import lk.dmc.disaster.response.entity.RescueAssignment;
import lk.dmc.disaster.response.entity.RescueTeam;
import lk.dmc.disaster.response.entity.RescueTeamStatus;
import lk.dmc.disaster.response.entity.ResourceAllocation;
import lk.dmc.disaster.response.entity.Shelter;
import lk.dmc.disaster.response.entity.ShelterStatus;
import lk.dmc.disaster.response.entity.TeamType;
import lk.dmc.disaster.response.repository.ActivityLogRepository;
import lk.dmc.disaster.response.repository.ReliefStockRepository;
import lk.dmc.disaster.response.repository.RescueAssignmentRepository;
import lk.dmc.disaster.response.repository.RescueTeamRepository;
import lk.dmc.disaster.response.repository.ResourceAllocationRepository;
import lk.dmc.disaster.response.repository.ShelterRepository;
import lk.dmc.disaster.shared.actor.ActingUser;
import lk.dmc.disaster.shared.actor.ActingUserContext;
import lk.dmc.disaster.shared.domain.Role;
import lk.dmc.disaster.shared.domain.WarningLevel;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import lk.dmc.disaster.warnings.ActiveWarningQuery;
import lk.dmc.disaster.warnings.ActiveWarningSummary;
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
class DistrictDashboardServiceImplTest {

  @Mock private JdbcClient jdbc;
  @Mock private JdbcClient.StatementSpec statementSpec;
  @Mock private JdbcClient.MappedQuerySpec<ReliefStockDto> mappedQuerySpec;
  @Mock private RescueTeamRepository teams;
  @Mock private RescueAssignmentRepository assignments;
  @Mock private ShelterRepository shelters;
  @Mock private ActivityLogRepository activityLogs;
  @Mock private ReliefStockRepository stocks;
  @Mock private ResourceAllocationRepository allocations;
  @Mock private ActingUserContext actingUser;
  @Mock private ActiveWarningQuery activeWarningQuery;

  private DistrictDashboardServiceImpl service;

  @BeforeEach
  void setUp() {
    service =
        new DistrictDashboardServiceImpl(
            jdbc,
            teams,
            assignments,
            shelters,
            activityLogs,
            stocks,
            allocations,
            actingUser,
            activeWarningQuery);
  }

  @Test
  void getDashboard_aggregatesMetricsAccurately() {
    UUID districtId = UUID.randomUUID();

    RescueTeam t1 = RescueTeam.create("Team 1", UUID.randomUUID(), districtId, TeamType.BOAT, 5);
    RescueTeam t2 = RescueTeam.create("Team 2", UUID.randomUUID(), districtId, TeamType.MEDICAL, 5);
    t2.markDispatched();
    when(teams.findByDistrictId(districtId)).thenReturn(List.of(t1, t2));

    RescueAssignment a1 =
        RescueAssignment.create(
            UUID.randomUUID(),
            null,
            UUID.randomUUID(),
            6.9,
            79.8,
            "Loc 1",
            "Task 1",
            (short) 1,
            5,
            null);
    a1.assignTeam(t2.getId()); // PENDING_ACK
    RescueAssignment a2 =
        RescueAssignment.create(
            UUID.randomUUID(),
            null,
            UUID.randomUUID(),
            6.9,
            79.8,
            "Loc 2",
            "Task 2",
            (short) 2,
            8,
            null);
    a2.cancel("Cancelled");
    RescueAssignment a3 =
        RescueAssignment.create(
            UUID.randomUUID(),
            null,
            UUID.randomUUID(),
            6.9,
            79.8,
            "Loc 3",
            "Task 3",
            (short) 3,
            2,
            null);
    a3.complete();
    when(assignments.findByDistrictId(districtId)).thenReturn(List.of(a1, a2, a3));

    Shelter s1 = Shelter.create("Shelter 1", districtId, "Addr 1", 6.9, 79.8, 100, null);
    s1.updateOccupancy(90); // 90% full >= 85%
    Shelter s2 = Shelter.create("Shelter 2", districtId, "Addr 2", 6.9, 79.8, 50, null);
    s2.updateOccupancy(20); // 40% full < 85%
    Shelter s3 = Shelter.create("Shelter 3", districtId, "Addr 3", 6.9, 79.8, 100, null);
    s3.close(); // CLOSED shelter branch
    Shelter s4 =
        Shelter.create("Shelter 4", districtId, "Addr 4", 6.9, 79.8, 0, null); // 0 capacity branch
    when(shelters.findByDistrictId(districtId)).thenReturn(List.of(s1, s2, s3, s4));

    ReliefStock rs1 = ReliefStock.create(UUID.randomUUID(), UUID.randomUUID(), districtId, 200);
    when(stocks.findByDistrictId(districtId)).thenReturn(List.of(rs1));

    ActiveWarningSummary summary =
        new ActiveWarningSummary(
            UUID.randomUUID(),
            WarningLevel.WARNING,
            "Warning 1",
            "Evacuate",
            UUID.randomUUID(),
            Instant.now(),
            Set.of(districtId),
            Set.of());
    when(activeWarningQuery.findActiveForDistrict(districtId)).thenReturn(List.of(summary));

    ActivityLog act =
        ActivityLog.create(
            districtId, null, ActivityType.RELIEF, "Blankets delivered", Instant.now());
    when(activityLogs.findByDistrictIdOrderByOccurredAtDesc(districtId)).thenReturn(List.of(act));

    DistrictDashboard dashboard = service.getDashboard(districtId);

    assertThat(dashboard.districtId()).isEqualTo(districtId);
    assertThat(dashboard.teamsByStatus())
        .containsEntry("AVAILABLE", 1L)
        .containsEntry("DISPATCHED", 1L);
    assertThat(dashboard.openAssignments()).isEqualTo(1L); // only a1
    assertThat(dashboard.pendingAcknowledgement()).isEqualTo(1L); // only a1
    assertThat(dashboard.sheltersOccupied()).isEqualTo(110L); // 90 + 20
    assertThat(dashboard.shelterCapacity()).isEqualTo(250L); // 100 + 50 + 100 + 0
    assertThat(dashboard.nearlyFullShelters()).isEqualTo(1L); // s1
    assertThat(dashboard.stockLines()).isEqualTo(1L);
    assertThat(dashboard.activeWarnings()).isEqualTo(1L);
    assertThat(dashboard.activity()).hasSize(1);
    assertThat(dashboard.activity().get(0).type()).isEqualTo("RELIEF");
  }

  @Test
  void getTeams_filtersAvailable() {
    UUID districtId = UUID.randomUUID();
    RescueTeam t = RescueTeam.create("Team", UUID.randomUUID(), districtId, TeamType.BOAT, 5);

    when(teams.findByDistrictIdAndStatus(districtId, RescueTeamStatus.AVAILABLE))
        .thenReturn(List.of(t));
    when(teams.findByDistrictId(districtId)).thenReturn(List.of(t));

    assertThat(service.getTeams(districtId, true)).hasSize(1);
    assertThat(service.getTeams(districtId, false)).hasSize(1);
    assertThat(service.getTeams(districtId, null)).hasSize(1);
  }

  @Test
  void getAssignments_withAndWithoutStatus() {
    UUID districtId = UUID.randomUUID();
    RescueAssignment a =
        RescueAssignment.create(
            UUID.randomUUID(),
            null,
            UUID.randomUUID(),
            6.9,
            79.8,
            "Loc",
            "Task",
            (short) 1,
            5,
            null);

    when(assignments.findByDistrictIdAndStatus(
            eq(districtId), eq(AssignmentStatus.PENDING_ACK), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(a)));
    when(assignments.findByDistrictId(eq(districtId), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(a)));

    Page<AssignmentDto> p1 = service.getAssignments(districtId, "PENDING_ACK", 0, 10);
    assertThat(p1.getContent()).hasSize(1);

    Page<AssignmentDto> p2 = service.getAssignments(districtId, null, 0, 10);
    assertThat(p2.getContent()).hasSize(1);

    Page<AssignmentDto> p3 = service.getAssignments(districtId, "   ", 0, 10);
    assertThat(p3.getContent()).hasSize(1);
  }

  @Test
  void getAssignment_foundAndNotFound() {
    UUID id = UUID.randomUUID();
    RescueAssignment a =
        RescueAssignment.create(
            UUID.randomUUID(),
            null,
            UUID.randomUUID(),
            6.9,
            79.8,
            "Loc",
            "Task",
            (short) 1,
            5,
            null);
    when(assignments.findById(id)).thenReturn(Optional.of(a));

    assertThat(service.getAssignment(id)).isNotNull();

    UUID missing = UUID.randomUUID();
    when(assignments.findById(missing)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.getAssignment(missing))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.NOT_FOUND);
  }

  @Test
  void getMyAssignment_returnsLiveOrNull() {
    UUID teamId = UUID.randomUUID();
    ActingUser user = new ActingUser(UUID.randomUUID(), Role.RESCUE_MEMBER, null, null, teamId);
    when(actingUser.require()).thenReturn(user);

    RescueAssignment a =
        RescueAssignment.create(
            UUID.randomUUID(),
            null,
            UUID.randomUUID(),
            6.9,
            79.8,
            "Loc",
            "Task",
            (short) 1,
            5,
            null);
    a.assignTeam(teamId);

    when(assignments.findByTeamIdAndStatusIn(eq(teamId), any())).thenReturn(List.of(a));
    assertThat(service.getMyAssignment()).isNotNull();

    when(assignments.findByTeamIdAndStatusIn(eq(teamId), any())).thenReturn(List.of());
    assertThat(service.getMyAssignment()).isNull();
  }

  @Test
  void getShelters_andSuggestions() {
    UUID districtId = UUID.randomUUID();
    Shelter s = Shelter.create("Shelter", districtId, "Address", 6.9, 79.8, 100, null);

    when(shelters.findByDistrictIdAndStatus(districtId, ShelterStatus.OPEN)).thenReturn(List.of(s));
    when(shelters.findByDistrictId(districtId)).thenReturn(List.of(s));
    when(shelters.findByDistrictIdAndStatusAndCurrentOccupancyLessThan(
            districtId, ShelterStatus.OPEN, Integer.MAX_VALUE))
        .thenReturn(List.of(s));

    assertThat(service.getShelters(districtId, true)).hasSize(1);
    assertThat(service.getShelters(districtId, false)).hasSize(1);
    assertThat(service.getShelters(districtId, null)).hasSize(1);
    assertThat(service.getShelterSuggestions(districtId, 50)).hasSize(1);
  }

  @Test
  void getStocks_andAllocations() {
    UUID districtId = UUID.randomUUID();
    UUID itemId = UUID.randomUUID();
    ReliefStockDto stockDto =
        new ReliefStockDto(
            UUID.randomUUID(),
            itemId,
            "Item",
            "kg",
            UUID.randomUUID(),
            "Org",
            districtId,
            10,
            0L,
            Instant.now(),
            Instant.now());

    when(jdbc.sql(anyString())).thenReturn(statementSpec);
    when(statementSpec.param(eq("districtId"), eq(districtId))).thenReturn(statementSpec);
    when(statementSpec.param(eq("itemId"), eq(itemId))).thenReturn(statementSpec);
    when(statementSpec.query(any(RowMapper.class))).thenReturn(mappedQuerySpec);
    when(mappedQuerySpec.list()).thenReturn(List.of(stockDto));

    assertThat(service.getStocks(districtId, itemId)).hasSize(1);

    // Test getStocks with null filters
    when(jdbc.sql(anyString())).thenReturn(statementSpec);
    when(statementSpec.query(any(RowMapper.class))).thenReturn(mappedQuerySpec);
    assertThat(service.getStocks(null, null)).hasSize(1);

    UUID shelterId = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();
    ResourceAllocation alloc =
        ResourceAllocation.create(UUID.randomUUID(), shelterId, eventId, 20, UUID.randomUUID());
    when(allocations.findByShelterIdOrEventId(eq(shelterId), eq(eventId), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(alloc)));

    assertThat(service.getAllocations(shelterId, eventId, 0, 10).getContent()).hasSize(1);

    // Test getDashboard when activeWarningQuery is null
    DistrictDashboardServiceImpl serviceNoWarn =
        new DistrictDashboardServiceImpl(
            jdbc,
            teams,
            assignments,
            shelters,
            activityLogs,
            stocks,
            allocations,
            actingUser,
            null);
    DistrictDashboard db = serviceNoWarn.getDashboard(districtId);
    assertThat(db.activeWarnings()).isZero();
  }
}
