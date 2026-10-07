package lk.dmc.disaster.response.api;

import java.util.UUID;
import lk.dmc.disaster.response.dto.response.AllocationDto;
import lk.dmc.disaster.response.dto.response.AssignmentDto;
import lk.dmc.disaster.response.dto.response.DistributionDto;
import lk.dmc.disaster.response.dto.response.ReliefStockDto;
import lk.dmc.disaster.response.dto.response.RescueTeamDto;
import lk.dmc.disaster.response.dto.response.DistrictDashboard;
import lk.dmc.disaster.response.dto.response.ShelterDto;
import lk.dmc.disaster.response.dto.response.ShelterSuggestionDto;
import org.springframework.http.ResponseEntity;

public interface ResponseApi {

  ResponseEntity<DistrictDashboard> dashboard(UUID districtId);

  ResponseEntity<RescueTeamDto[]> teams(UUID districtId, Boolean available);

  ResponseEntity<AssignmentDto> createAssignment(AssignmentInput input);

  ResponseEntity<AssignmentDto> assign(UUID id, String teamId);

  ResponseEntity<AssignmentDto> cancelAssignment(UUID id);

  ResponseEntity<AssignmentDto[]> assignments(UUID districtId, String status);

  ResponseEntity<AssignmentDto> assignment(UUID id);

  ResponseEntity<AssignmentDto> myAssignment();

  ResponseEntity<AssignmentDto> respond(UUID id, boolean accept, String declineReason);

  ResponseEntity<RescueTeamDto> updateTeamStatus(UUID teamId, TeamStatusUpdate update);

  ResponseEntity<ShelterDto[]> shelters(UUID districtId, Boolean availableOnly);

  ResponseEntity<ShelterSuggestionDto[]> shelterSuggestions(UUID districtId, int people);

  ResponseEntity<ShelterDto> updateOccupancy(UUID shelterId, int occupancy);

  ResponseEntity<ReliefStockDto[]> stocks(UUID districtId, UUID itemId);

  ResponseEntity<AllocationDto> allocate(AllocationInput input);

  ResponseEntity<AllocationDto> recordDistribution(UUID allocationId, DistributionInput input);

  ResponseEntity<AllocationDto[]> allocations(UUID shelterId, UUID eventId);

  record AssignmentInput(
      UUID eventId,
      UUID warningId,
      double latitude,
      double longitude,
      String locationText,
      String task,
      int priority,
      int peopleEstimated,
      UUID destinationShelterId,
      UUID teamId) {}

  record TeamStatusUpdate(
      String toStatus,
      UUID clientRef,
      java.time.Instant changedAt,
      boolean recordedOffline) {}

  record AllocationInput(UUID stockId, UUID shelterId, UUID eventId, int quantity) {}

  record DistributionInput(int quantityDistributed, java.time.Instant distributedAt, UUID clientRef, boolean recordedOffline) {}
}
