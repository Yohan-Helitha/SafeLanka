package lk.dmc.disaster.response.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.response.dto.request.TeamStatusUpdateRequest;
import lk.dmc.disaster.response.dto.response.RescueTeamDto;
import lk.dmc.disaster.response.service.RescueTeamsService;
import lk.dmc.disaster.shared.actor.RequiresRole;
import lk.dmc.disaster.shared.api.ApiResponse;
import lk.dmc.disaster.shared.domain.Role;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rescue-teams")
public class RescueTeamsController {

  private final RescueTeamsService rescueTeamService;

  RescueTeamsController(RescueTeamsService rescueTeamService) {
    this.rescueTeamService = rescueTeamService;
  }

  @GetMapping
  @RequiresRole({Role.DISTRICT_OFFICER, Role.DMC_OFFICER})
  ApiResponse<List<RescueTeamDto>> list(
      @RequestParam(required = false) UUID districtId,
      @RequestParam(name = "available", required = false) Boolean available) {
    return ApiResponse.of(rescueTeamService.getTeams(districtId, available));
  }

  @PatchMapping("/{id}/status")
  @RequiresRole({Role.DISTRICT_OFFICER, Role.RESCUE_MEMBER})
  ApiResponse<RescueTeamDto> updateStatus(
      @PathVariable UUID id, @Valid @RequestBody TeamStatusUpdateRequest request) {
    return ApiResponse.of(
        rescueTeamService.updateTeamStatus(
            id, request.toStatus(), request.changedAt(), request.recordedOffline()));
  }
}
