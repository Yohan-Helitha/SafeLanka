package lk.dmc.disaster.response.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import lk.dmc.disaster.response.dto.response.DistrictDashboard;
import lk.dmc.disaster.response.service.DistrictDashboardService;
import lk.dmc.disaster.shared.domain.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(MockitoExtension.class)
class DistrictDashboardControllerTest extends ResponseControllerTestSupport {

  @Mock private DistrictDashboardService dashboardService;

  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    mvc = mvcFor(new DistrictDashboardController(dashboardService));
  }

  @Test
  void dashboard_authorizedRoles_returns200WithData() throws Exception {
    signedInAs(Role.DISTRICT_OFFICER);
    UUID districtId = UUID.randomUUID();

    DistrictDashboard dashboard =
        new DistrictDashboard(
            districtId, null, Map.of("AVAILABLE", 3L), 5L, 2L, 120L, 200L, 1L, 4L, 2L, List.of());
    when(dashboardService.getDashboard(districtId)).thenReturn(dashboard);

    mvc.perform(get("/api/response/dashboard").param("districtId", districtId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.districtId").value(districtId.toString()))
        .andExpect(jsonPath("$.data.openAssignments").value(5))
        .andExpect(jsonPath("$.data.pendingAcknowledgement").value(2))
        .andExpect(jsonPath("$.data.sheltersOccupied").value(120));

    // DMC_OFFICER should also be authorized
    signedInAs(Role.DMC_OFFICER);
    mvc.perform(get("/api/response/dashboard").param("districtId", districtId.toString()))
        .andExpect(status().isOk());
  }

  @Test
  void dashboard_unauthorizedRole_returns403() throws Exception {
    signedInAs(Role.CITIZEN);
    UUID districtId = UUID.randomUUID();

    mvc.perform(get("/api/response/dashboard").param("districtId", districtId.toString()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("FORBIDDEN_ROLE"));
  }
}
