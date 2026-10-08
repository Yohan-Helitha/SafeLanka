package lk.dmc.disaster.response.service;

import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.response.dto.response.*;
import lk.dmc.disaster.response.entity.ActivityLog;
import lk.dmc.disaster.response.entity.AssignmentStatus;
import lk.dmc.disaster.response.entity.RescueAssignment;
import lk.dmc.disaster.response.entity.RescueTeam;
import lk.dmc.disaster.response.entity.RescueTeamStatus;
import lk.dmc.disaster.response.entity.Shelter;
import lk.dmc.disaster.response.entity.ShelterStatus;
import lk.dmc.disaster.response.repository.ActivityLogRepository;
import lk.dmc.disaster.response.repository.ReliefStockRepository;
import lk.dmc.disaster.response.repository.RescueAssignmentRepository;
import lk.dmc.disaster.response.repository.RescueTeamRepository;
import lk.dmc.disaster.response.repository.ResourceAllocationRepository;
import lk.dmc.disaster.response.repository.ShelterRepository;
import lk.dmc.disaster.shared.actor.ActingUserContext;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import lk.dmc.disaster.warnings.ActiveWarningQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
public class DistrictDashboardServiceImpl implements DistrictDashboardService {

  private final JdbcClient jdbc;
  private final RescueTeamRepository teams;
  private final RescueAssignmentRepository assignments;
  private final ShelterRepository shelters;
  private final ActivityLogRepository activityLogs;
  private final ReliefStockRepository stocks;
  private final ResourceAllocationRepository allocations;
  private final ActingUserContext actingUser;
  private final ActiveWarningQuery activeWarningQuery;

  DistrictDashboardServiceImpl(
      JdbcClient jdbc,
      RescueTeamRepository teams,
      RescueAssignmentRepository assignments,
      ShelterRepository shelters,
      ActivityLogRepository activityLogs,
      ReliefStockRepository stocks,
      ResourceAllocationRepository allocations,
      ActingUserContext actingUser,
      ActiveWarningQuery activeWarningQuery) {
    this.jdbc = jdbc;
    this.teams = teams;
    this.assignments = assignments;
    this.shelters = shelters;
    this.activityLogs = activityLogs;
    this.stocks = stocks;
    this.allocations = allocations;
    this.actingUser = actingUser;
    this.activeWarningQuery = activeWarningQuery;
  }

  @Override
  public DistrictDashboard getDashboard(UUID districtId) {
    List<RescueTeam> allDistrictTeams = teams.findByDistrictId(districtId);
    java.util.Map<String, Long> teamsByStatus =
        allDistrictTeams.stream()
            .collect(
                java.util.stream.Collectors.groupingBy(
                    t -> t.getStatus().name(), java.util.stream.Collectors.counting()));

    List<RescueAssignment> allAssignments = assignments.findByDistrictId(districtId);
    long openAssignments =
        allAssignments.stream()
            .filter(
                a ->
                    a.getStatus() != AssignmentStatus.CANCELLED
                        && a.getStatus() != AssignmentStatus.COMPLETED)
            .count();
    long pendingAcknowledgement =
        allAssignments.stream().filter(a -> a.getStatus() == AssignmentStatus.PENDING_ACK).count();

    List<Shelter> shelterList = shelters.findByDistrictId(districtId);
    long sheltersOccupied = shelterList.stream().mapToLong(Shelter::getCurrentOccupancy).sum();
    long totalCapacity = shelterList.stream().mapToLong(Shelter::getCapacity).sum();
    long nearlyFullShelters =
        shelterList.stream()
            .filter(
                s ->
                    s.getStatus() != ShelterStatus.CLOSED
                        && s.getCapacity() > 0
                        && ((double) s.getCurrentOccupancy() / s.getCapacity()) >= 0.85)
            .count();

    long stockLines = stocks.findByDistrictId(districtId).size();

    long activeWarnings =
        activeWarningQuery != null
            ? activeWarningQuery.findActiveForDistrict(districtId).size()
            : 0L;

    List<ActivityLog> recent = activityLogs.findByDistrictIdOrderByOccurredAtDesc(districtId);
    List<DistrictDashboard.ActivityEntry> summaries =
        recent.stream()
            .limit(20)
            .map(
                a ->
                    new DistrictDashboard.ActivityEntry(
                        a.getId(), a.getType().name(), a.getMessage(), a.getOccurredAt()))
            .toList();

    return new DistrictDashboard(
        districtId,
        null, // activeEventId populated or null
        teamsByStatus,
        openAssignments,
        pendingAcknowledgement,
        sheltersOccupied,
        totalCapacity,
        nearlyFullShelters,
        stockLines,
        activeWarnings,
        summaries);
  }

  @Override
  public List<RescueTeamDto> getTeams(UUID districtId, Boolean available) {
    if (available != null && available) {
      return teams.findByDistrictIdAndStatus(districtId, RescueTeamStatus.AVAILABLE).stream()
          .map(RescueTeamDto::from)
          .toList();
    }
    return teams.findByDistrictId(districtId).stream().map(RescueTeamDto::from).toList();
  }

  @Override
  public Page<AssignmentDto> getAssignments(UUID districtId, String status, int page, int size) {
    Pageable pageable = Pageable.ofSize(size).withPage(page);
    if (status != null && !status.isBlank()) {
      return assignments
          .findByDistrictIdAndStatus(districtId, AssignmentStatus.valueOf(status), pageable)
          .map(AssignmentDto::from);
    }
    return assignments.findByDistrictId(districtId, pageable).map(AssignmentDto::from);
  }

  @Override
  public AssignmentDto getAssignment(UUID id) {
    return AssignmentDto.from(
        assignments
            .findById(id)
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Assignment not found")));
  }

  @Override
  public AssignmentDto getMyAssignment() {
    var user = actingUser.require();
    List<AssignmentStatus> liveStatuses =
        List.of(
            AssignmentStatus.PENDING_ACK, AssignmentStatus.ACCEPTED,
            AssignmentStatus.EN_ROUTE, AssignmentStatus.ACTIVE);
    List<RescueAssignment> list =
        assignments.findByTeamIdAndStatusIn(user.rescueTeamId(), liveStatuses);
    if (list.isEmpty()) {
      return null;
    }
    return AssignmentDto.from(list.get(0));
  }

  @Override
  public List<ShelterDto> getShelters(UUID districtId, Boolean availableOnly) {
    if (availableOnly != null && availableOnly) {
      return shelters.findByDistrictIdAndStatus(districtId, ShelterStatus.OPEN).stream()
          .map(ShelterDto::from)
          .toList();
    }
    return shelters.findByDistrictId(districtId).stream().map(ShelterDto::from).toList();
  }

  @Override
  public List<ShelterSuggestionDto> getShelterSuggestions(UUID districtId, int people) {
    return shelters
        .findByDistrictIdAndStatusAndCurrentOccupancyLessThan(
            districtId, ShelterStatus.OPEN, Integer.MAX_VALUE)
        .stream()
        .filter(s -> s.getAvailableCapacity() >= people)
        .sorted((a, b) -> Integer.compare(b.getAvailableCapacity(), a.getAvailableCapacity()))
        .map(s -> ShelterSuggestionDto.from(s, 0))
        .toList();
  }

  @Override
  public List<ReliefStockDto> getStocks(UUID districtId, UUID itemId) {
    StringBuilder sql =
        new StringBuilder(
            """
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
        .query(
            (rs, rowNum) -> {
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
  public Page<AllocationDto> getAllocations(UUID shelterId, UUID eventId, int page, int size) {
    Pageable pageable = Pageable.ofSize(size).withPage(page);
    return allocations
        .findByShelterIdOrEventId(shelterId, eventId, pageable)
        .map(AllocationDto::from);
  }
}
