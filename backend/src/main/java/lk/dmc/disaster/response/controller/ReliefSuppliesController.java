package lk.dmc.disaster.response.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.response.dto.request.AllocationRequest;
import lk.dmc.disaster.response.dto.request.DistributionRequest;
import lk.dmc.disaster.response.dto.response.AllocationDto;
import lk.dmc.disaster.response.dto.response.DistributionDto;
import lk.dmc.disaster.response.dto.response.ReliefStockDto;
import lk.dmc.disaster.response.service.ReliefSuppliesService;
import lk.dmc.disaster.shared.actor.ActingUserContext;
import lk.dmc.disaster.shared.actor.RequiresRole;
import lk.dmc.disaster.shared.api.ApiResponse;
import lk.dmc.disaster.shared.domain.Role;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReliefSuppliesController {

  private final ReliefSuppliesService reliefSuppliesService;
  private final ActingUserContext actingUser;

  ReliefSuppliesController(
      ReliefSuppliesService reliefSuppliesService, ActingUserContext actingUser) {
    this.reliefSuppliesService = reliefSuppliesService;
    this.actingUser = actingUser;
  }

  @GetMapping("/api/relief-stocks")
  @RequiresRole({Role.DISTRICT_OFFICER, Role.DMC_OFFICER})
  ApiResponse<List<ReliefStockDto>> listStocks(
      @RequestParam(required = false) UUID districtId,
      @RequestParam(required = false) UUID itemId) {
    return ApiResponse.of(reliefSuppliesService.getStocks(districtId, itemId));
  }

  @PostMapping("/api/allocations")
  @RequiresRole(Role.DISTRICT_OFFICER)
  ResponseEntity<ApiResponse<AllocationDto>> allocate(
      @Valid @RequestBody AllocationRequest request) {
    var user = actingUser.require();
    ReliefSuppliesService.CreateAllocationCommand command =
        new ReliefSuppliesService.CreateAllocationCommand(
            request.stockId(),
            request.shelterId(),
            request.eventId(),
            request.quantity(),
            user.id());
    AllocationDto created = reliefSuppliesService.allocate(command);
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(created));
  }

  @PostMapping("/api/allocations/{id}/distributions")
  @RequiresRole({Role.DISTRICT_OFFICER, Role.SHELTER_COORDINATOR})
  ResponseEntity<ApiResponse<DistributionDto>> distributions(
      @PathVariable UUID id, @Valid @RequestBody DistributionRequest request) {
    var user = actingUser.require();
    ReliefSuppliesService.CreateDistributionCommand command =
        new ReliefSuppliesService.CreateDistributionCommand(
            request.quantityDistributed(),
            request.distributedAt(),
            request.clientRef(),
            request.recordedOffline(),
            user.id());
    DistributionDto created = reliefSuppliesService.recordDistribution(id, command);
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(created));
  }

  @GetMapping("/api/allocations")
  @RequiresRole({Role.DISTRICT_OFFICER, Role.DMC_OFFICER, Role.SHELTER_COORDINATOR})
  ApiResponse<?> listAllocations(
      @RequestParam(required = false) UUID shelterId,
      @RequestParam(required = false) UUID eventId,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size) {
    if (page != null && size != null) {
      return ApiResponse.page(reliefSuppliesService.getAllocations(shelterId, eventId, page, size));
    }
    return ApiResponse.of(reliefSuppliesService.listAllocations(shelterId, eventId));
  }
}
