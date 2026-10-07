package lk.dmc.disaster.response.controller;

import java.util.UUID;
import lk.dmc.disaster.shared.actor.RequiresRole;
import lk.dmc.disaster.response.dto.response.DistrictDashboard;
import lk.dmc.disaster.response.service.DistrictDashboardService;
import lk.dmc.disaster.shared.api.ApiResponse;
import lk.dmc.disaster.shared.domain.Role;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/response")
@RequiresRole({Role.DISTRICT_OFFICER, Role.DMC_OFFICER})
public class DistrictDashboardController {

  private final DistrictDashboardService responseService;

  DistrictDashboardController(DistrictDashboardService responseService) {
    this.responseService = responseService;
  }

  @GetMapping("/dashboard")
  ApiResponse<DistrictDashboard> dashboard(@RequestParam UUID districtId) {
    return ApiResponse.of(responseService.getDashboard(districtId));
  }
}
