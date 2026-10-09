package lk.dmc.disaster.warnings.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lk.dmc.disaster.shared.actor.ActingUser;
import lk.dmc.disaster.shared.actor.ActingUserContext;
import lk.dmc.disaster.shared.actor.RequiresRole;
import lk.dmc.disaster.shared.api.ApiResponse;
import lk.dmc.disaster.shared.domain.Role;
import lk.dmc.disaster.warnings.dto.CitizenAlertResponse;
import lk.dmc.disaster.warnings.service.CitizenAlertService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The active alerts for the signed-in person's own district and river basin: the public alert
 * banner every citizen, volunteer and officer sees (the URL rule in AccessRules allows the same
 * four roles).
 */
@Tag(name = "Citizen alerts")
@RestController
@RequestMapping("/api/warnings/active/mine")
@RequiresRole({Role.CITIZEN, Role.VOLUNTEER, Role.DISTRICT_OFFICER, Role.DMC_OFFICER})
class CitizenAlertController {

  private final CitizenAlertService alerts;
  private final ActingUserContext actingUser;

  CitizenAlertController(CitizenAlertService alerts, ActingUserContext actingUser) {
    this.alerts = alerts;
    this.actingUser = actingUser;
  }

  @Operation(summary = "Active alerts for me, most serious first")
  @GetMapping
  ApiResponse<List<CitizenAlertResponse>> mine() {
    ActingUser user = actingUser.require();
    return ApiResponse.of(
        alerts.alertsFor(user.districtId(), user.riverBasinId()).stream()
            .map(CitizenAlertResponse::from)
            .toList());
  }
}
