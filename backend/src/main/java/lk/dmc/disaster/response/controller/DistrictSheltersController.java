package lk.dmc.disaster.response.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.shared.actor.ActingUserContext;
import lk.dmc.disaster.shared.actor.RequiresRole;
import lk.dmc.disaster.response.dto.request.OccupancyUpdateRequest;
import lk.dmc.disaster.response.dto.response.ShelterDto;
import lk.dmc.disaster.response.dto.response.ShelterSuggestionDto;
import lk.dmc.disaster.response.service.DistrictSheltersService;
import lk.dmc.disaster.shared.api.ApiResponse;
import lk.dmc.disaster.shared.domain.Role;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shelters")
public class DistrictSheltersController {

  private final DistrictSheltersService shelterService;
  private final ActingUserContext actingUser;

  DistrictSheltersController(DistrictSheltersService shelterService, ActingUserContext actingUser) {
    this.shelterService = shelterService;
    this.actingUser = actingUser;
  }

  @GetMapping
  @RequiresRole({Role.DISTRICT_OFFICER, Role.DMC_OFFICER, Role.SHELTER_COORDINATOR})
  ApiResponse<List<ShelterDto>> list(
      @RequestParam(required = false) UUID districtId,
      @RequestParam(name = "availableOnly", required = false) Boolean availableOnly) {
    return ApiResponse.of(shelterService.getShelters(districtId, availableOnly));
  }

  @GetMapping("/suggestions")
  @RequiresRole(Role.DISTRICT_OFFICER)
  ApiResponse<List<ShelterSuggestionDto>> suggestions(
      @RequestParam UUID districtId,
      @RequestParam int people) {
    return ApiResponse.of(shelterService.getSuggestions(districtId, people));
  }

  @PatchMapping("/{id}/occupancy")
  @RequiresRole({Role.DISTRICT_OFFICER, Role.SHELTER_COORDINATOR})
  ApiResponse<ShelterDto> updateOccupancy(@PathVariable UUID id, @Valid @RequestBody OccupancyUpdateRequest request) {
    return ApiResponse.of(shelterService.updateOccupancy(id, request.occupancy()));
  }
}
