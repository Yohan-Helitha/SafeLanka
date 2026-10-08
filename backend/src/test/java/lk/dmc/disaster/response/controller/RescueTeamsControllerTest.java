package lk.dmc.disaster.response.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.response.dto.response.RescueTeamDto;
import lk.dmc.disaster.response.entity.RescueTeamStatus;
import lk.dmc.disaster.response.entity.TeamType;
import lk.dmc.disaster.response.service.RescueTeamsService;
import lk.dmc.disaster.shared.domain.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(MockitoExtension.class)
class RescueTeamsControllerTest extends ResponseControllerTestSupport {

  @Mock private RescueTeamsService rescueTeamsService;

  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    mvc = mvcFor(new RescueTeamsController(rescueTeamsService));
  }

  private RescueTeamDto dummyDto(UUID id) {
    return new RescueTeamDto(
        id,
        "Galle Team 1",
        UUID.randomUUID(),
        DISTRICT,
        TeamType.BOAT,
        6,
        RescueTeamStatus.AVAILABLE,
        Instant.now(),
        0L);
  }

  @Test
  void list_authorizedRoles_returns200() throws Exception {
    signedInAs(Role.DISTRICT_OFFICER);
    UUID teamId = UUID.randomUUID();
    when(rescueTeamsService.getTeams(eq(DISTRICT), eq(true))).thenReturn(List.of(dummyDto(teamId)));

    mvc.perform(
            get("/api/rescue-teams")
                .param("districtId", DISTRICT.toString())
                .param("available", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].id").value(teamId.toString()))
        .andExpect(jsonPath("$.data[0].name").value("Galle Team 1"));

    // DMC_OFFICER authorized
    signedInAs(Role.DMC_OFFICER);
    mvc.perform(get("/api/rescue-teams")).andExpect(status().isOk());
  }

  @Test
  void list_unauthorizedRole_returns403() throws Exception {
    signedInAs(Role.CITIZEN);
    mvc.perform(get("/api/rescue-teams")).andExpect(status().isForbidden());
  }

  @Test
  void updateStatus_authorizedRoles_validRequest_returns200() throws Exception {
    signedInAs(Role.RESCUE_MEMBER);
    UUID teamId = UUID.randomUUID();
    RescueTeamDto updated =
        new RescueTeamDto(
            teamId,
            "Galle Team 1",
            UUID.randomUUID(),
            DISTRICT,
            TeamType.BOAT,
            6,
            RescueTeamStatus.ACTIVE,
            Instant.now(),
            1L);

    when(rescueTeamsService.updateTeamStatus(eq(teamId), eq("ACTIVE"), any(), eq(false)))
        .thenReturn(updated);

    String body =
        """
        {
          "toStatus": "ACTIVE",
          "clientRef": null,
          "changedAt": null,
          "recordedOffline": false
        }
        """;

    mvc.perform(
            patch("/api/rescue-teams/{id}/status", teamId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("ACTIVE"));
  }

  @Test
  void updateStatus_invalidStatus_returns400() throws Exception {
    signedInAs(Role.DISTRICT_OFFICER);
    UUID teamId = UUID.randomUUID();

    String body = "{\"toStatus\": \"NOT_A_VALID_STATUS\"}";

    mvc.perform(
            patch("/api/rescue-teams/{id}/status", teamId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
  }

  @Test
  void updateStatus_unauthorizedRole_returns403() throws Exception {
    signedInAs(Role.CITIZEN);
    UUID teamId = UUID.randomUUID();

    String body = "{\"toStatus\": \"AVAILABLE\"}";

    mvc.perform(
            patch("/api/rescue-teams/{id}/status", teamId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isForbidden());
  }
}
