package lk.dmc.disaster.response.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.response.api.ResponseApi;
import lk.dmc.disaster.response.config.ResponseRoutes;
import lk.dmc.disaster.response.dto.request.AllocationRequest;
import lk.dmc.disaster.response.dto.request.AssignTeamRequest;
import lk.dmc.disaster.response.dto.request.CancelAssignmentRequest;
import lk.dmc.disaster.response.dto.request.DistributionRequest;
import lk.dmc.disaster.response.dto.request.OccupancyUpdateRequest;
import lk.dmc.disaster.response.dto.request.RespondRequest;
import lk.dmc.disaster.response.dto.request.TeamStatusUpdateRequest;
import lk.dmc.disaster.response.dto.response.AllocationDto;
import lk.dmc.disaster.response.dto.response.AssignmentDto;
import lk.dmc.disaster.response.dto.response.DistributionDto;
import lk.dmc.disaster.response.dto.response.PageResponse;
import lk.dmc.disaster.response.dto.response.ReliefStockDto;
import lk.dmc.disaster.response.dto.response.RescueTeamDto;
import lk.dmc.disaster.response.dto.response.ShelterDto;
import lk.dmc.disaster.response.dto.response.ShelterSuggestionDto;
import lk.dmc.disaster.response.entity.AllocationStatus;
import lk.dmc.disaster.response.entity.AssignmentStatus;
import lk.dmc.disaster.response.entity.ReliefDistribution;
import lk.dmc.disaster.response.entity.ReliefStock;
import lk.dmc.disaster.response.entity.RescueAssignment;
import lk.dmc.disaster.response.entity.RescueTeam;
import lk.dmc.disaster.response.entity.RescueTeamStatus;
import lk.dmc.disaster.response.entity.ResourceAllocation;
import lk.dmc.disaster.response.entity.Shelter;
import lk.dmc.disaster.response.entity.ShelterStatus;
import lk.dmc.disaster.response.entity.TeamType;
import org.junit.jupiter.api.Test;

class ResponseDtoTest {

  @Test
  void assignmentDto_fromEntity_mapsAllFields() {
    UUID eventId = UUID.randomUUID();
    UUID creatorId = UUID.randomUUID();
    UUID teamId = UUID.randomUUID();
    RescueAssignment assignment =
        RescueAssignment.create(
            eventId, null, creatorId, 6.9, 79.8, "Location", "Task", (short) 1, 12, null);
    assignment.assignTeam(teamId);

    AssignmentDto dto = AssignmentDto.from(assignment);

    assertThat(dto.id()).isEqualTo(assignment.getId());
    assertThat(dto.eventId()).isEqualTo(eventId);
    assertThat(dto.teamId()).isEqualTo(teamId);
    assertThat(dto.latitude()).isEqualTo(6.9);
    assertThat(dto.longitude()).isEqualTo(79.8);
    assertThat(dto.locationText()).isEqualTo("Location");
    assertThat(dto.task()).isEqualTo("Task");
    assertThat(dto.priority()).isEqualTo((short) 1);
    assertThat(dto.peopleEstimated()).isEqualTo(12);
    assertThat(dto.status()).isEqualTo(AssignmentStatus.PENDING_ACK);
  }

  @Test
  void rescueTeamDto_fromEntity_mapsAllFields() {
    RescueTeam team =
        RescueTeam.create("Team Alpha", UUID.randomUUID(), UUID.randomUUID(), TeamType.BOAT, 6);

    RescueTeamDto dto = RescueTeamDto.from(team);

    assertThat(dto.id()).isEqualTo(team.getId());
    assertThat(dto.name()).isEqualTo("Team Alpha");
    assertThat(dto.teamType()).isEqualTo(TeamType.BOAT);
    assertThat(dto.capacity()).isEqualTo(6);
    assertThat(dto.status()).isEqualTo(RescueTeamStatus.AVAILABLE);
  }

  @Test
  void shelterDto_and_suggestionDto_mapsAllFields() {
    Shelter shelter =
        Shelter.create(
            "Shelter One", UUID.randomUUID(), "Address 1", 6.9, 79.8, 100, UUID.randomUUID());
    shelter.updateOccupancy(40);

    ShelterDto dto = ShelterDto.from(shelter);
    assertThat(dto.id()).isEqualTo(shelter.getId());
    assertThat(dto.name()).isEqualTo("Shelter One");
    assertThat(dto.capacity()).isEqualTo(100);
    assertThat(dto.currentOccupancy()).isEqualTo(40);
    assertThat(dto.status()).isEqualTo(ShelterStatus.OPEN);

    ShelterSuggestionDto suggestion = ShelterSuggestionDto.from(shelter, 5.5);
    assertThat(suggestion.id()).isEqualTo(shelter.getId());
    assertThat(suggestion.name()).isEqualTo("Shelter One");
    assertThat(suggestion.availableCapacity()).isEqualTo(60);
    assertThat(suggestion.distanceKm()).isEqualTo(5.5);
  }

  @Test
  void allocationDto_and_distributionDto_fromEntity() {
    UUID stockId = UUID.randomUUID();
    UUID shelterId = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();
    UUID user = UUID.randomUUID();
    ResourceAllocation alloc = ResourceAllocation.create(stockId, shelterId, eventId, 50, user);

    AllocationDto allocDto = AllocationDto.from(alloc);
    assertThat(allocDto.id()).isEqualTo(alloc.getId());
    assertThat(allocDto.stockId()).isEqualTo(stockId);
    assertThat(allocDto.quantity()).isEqualTo(50);
    assertThat(allocDto.status()).isEqualTo(AllocationStatus.ALLOCATED);

    UUID clientRef = UUID.randomUUID();
    Instant now = Instant.now();
    ReliefDistribution dist =
        ReliefDistribution.create(alloc.getId(), 25, user, now, true, clientRef);

    DistributionDto distDto = DistributionDto.from(dist);
    assertThat(distDto.id()).isEqualTo(dist.getId());
    assertThat(distDto.allocationId()).isEqualTo(alloc.getId());
    assertThat(distDto.quantityDistributed()).isEqualTo(25);
    assertThat(distDto.recordedOffline()).isTrue();
    assertThat(distDto.clientRef()).isEqualTo(clientRef);
  }

  @Test
  void reliefStockDto_fromEntity_mapsAllFields() {
    UUID itemId = UUID.randomUUID();
    UUID orgId = UUID.randomUUID();
    UUID districtId = UUID.randomUUID();
    ReliefStock stock = ReliefStock.create(itemId, orgId, districtId, 500);

    ReliefStockDto dto1 = ReliefStockDto.from(stock);
    assertThat(dto1.id()).isEqualTo(stock.getId());
    assertThat(dto1.itemId()).isEqualTo(itemId);
    assertThat(dto1.organisationId()).isEqualTo(orgId);
    assertThat(dto1.districtId()).isEqualTo(districtId);
    assertThat(dto1.quantityAvailable()).isEqualTo(500);
    assertThat(dto1.itemName()).isNull();

    ReliefStockDto dto2 = ReliefStockDto.from(stock, "Rice", "KG", "Red Cross");
    assertThat(dto2.itemName()).isEqualTo("Rice");
    assertThat(dto2.unit()).isEqualTo("KG");
    assertThat(dto2.organisationName()).isEqualTo("Red Cross");
    assertThat(dto2.quantityAvailable()).isEqualTo(500);
  }

  @Test
  void pageResponse_and_requestDtos_records() {
    PageResponse<String> page = new PageResponse<>(List.of("A", "B"), 0, 10, 2L, 1);
    assertThat(page.items()).containsExactly("A", "B");
    assertThat(page.page()).isZero();
    assertThat(page.size()).isEqualTo(10);
    assertThat(page.totalElements()).isEqualTo(2L);
    assertThat(page.totalPages()).isEqualTo(1);

    AllocationRequest ar =
        new AllocationRequest(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 10);
    assertThat(ar.quantity()).isEqualTo(10);

    AssignTeamRequest atr = new AssignTeamRequest(UUID.randomUUID());
    assertThat(atr.teamId()).isNotNull();

    CancelAssignmentRequest car = new CancelAssignmentRequest("Reason");
    assertThat(car.reason()).isEqualTo("Reason");

    DistributionRequest dr = new DistributionRequest(10, Instant.now(), UUID.randomUUID(), false);
    assertThat(dr.quantityDistributed()).isEqualTo(10);

    OccupancyUpdateRequest our = new OccupancyUpdateRequest(25);
    assertThat(our.occupancy()).isEqualTo(25);

    RespondRequest rr = new RespondRequest(true, null);
    assertThat(rr.accept()).isTrue();

    TeamStatusUpdateRequest tsur = new TeamStatusUpdateRequest("AVAILABLE", null, null, false);
    assertThat(tsur.toStatus()).isEqualTo("AVAILABLE");
  }

  @Test
  void responseRoutes_and_apiRecords() {
    assertThat(ResponseRoutes.API_RESPONSE).isEqualTo("/api/response");
    assertThat(ResponseRoutes.DASHBOARD).isEqualTo("/dashboard");
    assertThat(ResponseRoutes.RESCUE_TEAMS).isEqualTo("/rescue-teams");
    assertThat(ResponseRoutes.ASSIGNMENTS).isEqualTo("/assignments");
    assertThat(ResponseRoutes.SHELTERS).isEqualTo("/shelters");

    ResponseApi.AssignmentInput input =
        new ResponseApi.AssignmentInput(
            UUID.randomUUID(), null, 6.9, 79.8, "Loc", "Task", 1, 10, null, null);
    assertThat(input.task()).isEqualTo("Task");

    ResponseApi.TeamStatusUpdate update =
        new ResponseApi.TeamStatusUpdate("ACTIVE", null, null, false);
    assertThat(update.toStatus()).isEqualTo("ACTIVE");

    ResponseApi.AllocationInput allocIn =
        new ResponseApi.AllocationInput(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 15);
    assertThat(allocIn.quantity()).isEqualTo(15);

    ResponseApi.DistributionInput distIn =
        new ResponseApi.DistributionInput(10, Instant.now(), UUID.randomUUID(), true);
    assertThat(distIn.quantityDistributed()).isEqualTo(10);
  }

  @Test
  void shelterHeadcountUpdateDto_and_requests_mapCorrectly() {
    UUID shelterId = UUID.randomUUID();
    UUID districtId = UUID.randomUUID();
    Shelter shelter = Shelter.create("Shelter ABC", districtId, "Road", 6.9, 79.8, 200, null);

    lk.dmc.disaster.response.entity.ShelterHeadcountUpdate update =
        lk.dmc.disaster.response.entity.ShelterHeadcountUpdate.create(
            shelterId, districtId, 175, 120, "Volunteer Sam", "VOLUNTEER", "30 families arrived");

    lk.dmc.disaster.response.dto.response.ShelterHeadcountUpdateDto dtoWithShelter =
        lk.dmc.disaster.response.dto.response.ShelterHeadcountUpdateDto.from(update, shelter);
    assertThat(dtoWithShelter.id()).isEqualTo(update.getId());
    assertThat(dtoWithShelter.shelterId()).isEqualTo(shelterId);
    assertThat(dtoWithShelter.shelterName()).isEqualTo("Shelter ABC");
    assertThat(dtoWithShelter.shelterCapacity()).isEqualTo(200);
    assertThat(dtoWithShelter.currentShelterOccupancy()).isEqualTo(0);
    assertThat(dtoWithShelter.reportedOccupancy()).isEqualTo(175);
    assertThat(dtoWithShelter.previousOccupancy()).isEqualTo(120);
    assertThat(dtoWithShelter.reportedByName()).isEqualTo("Volunteer Sam");
    assertThat(dtoWithShelter.reportedByRole()).isEqualTo("VOLUNTEER");
    assertThat(dtoWithShelter.message()).isEqualTo("30 families arrived");
    assertThat(dtoWithShelter.status()).isEqualTo(lk.dmc.disaster.response.entity.HeadcountUpdateStatus.PENDING);

    lk.dmc.disaster.response.dto.response.ShelterHeadcountUpdateDto dtoWithoutShelter =
        lk.dmc.disaster.response.dto.response.ShelterHeadcountUpdateDto.from(update, null);
    assertThat(dtoWithoutShelter.shelterName()).isEqualTo("Unknown Shelter");
    assertThat(dtoWithoutShelter.shelterCapacity()).isEqualTo(0);

    lk.dmc.disaster.response.dto.request.CreateHeadcountUpdateRequest createReq =
        new lk.dmc.disaster.response.dto.request.CreateHeadcountUpdateRequest(
            shelterId, 150, "Lead Officer", "FIELD_OFFICER", "Headcount count");
    assertThat(createReq.shelterId()).isEqualTo(shelterId);
    assertThat(createReq.reportedOccupancy()).isEqualTo(150);
    assertThat(createReq.reportedByName()).isEqualTo("Lead Officer");
    assertThat(createReq.reportedByRole()).isEqualTo("FIELD_OFFICER");
    assertThat(createReq.message()).isEqualTo("Headcount count");

    lk.dmc.disaster.response.dto.request.ApplyHeadcountUpdateRequest applyReq =
        new lk.dmc.disaster.response.dto.request.ApplyHeadcountUpdateRequest(160);
    assertThat(applyReq.customOccupancy()).isEqualTo(160);
  }
}
