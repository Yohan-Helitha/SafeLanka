package lk.dmc.disaster.response.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.response.dto.response.AssignmentDto;
import lk.dmc.disaster.response.entity.AssignmentStatus;
import lk.dmc.disaster.response.service.TeamAssignmentService;
import lk.dmc.disaster.shared.domain.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(MockitoExtension.class)
class TeamAssignmentControllerTest extends ResponseControllerTestSupport {

  @Mock private TeamAssignmentService assignmentService;

  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    mvc = mvcFor(new TeamAssignmentController(assignmentService, actingUser));
  }

  private AssignmentDto dummyDto(UUID id) {
    return new AssignmentDto(
        id,
        UUID.randomUUID(),
        null,
        UUID.randomUUID(),
        UUID.randomUUID(),
        6.9271,
        79.8612,
        "Wellampitiya",
        "Evacuate trapped families",
        (short) 1,
        15,
        null,
        AssignmentStatus.PENDING_ACK,
        null,
        Instant.now(),
        null,
        null,
        0L);
  }

  @Test
  void create_districtOfficer_validRequest_returns201() throws Exception {
    signedInAs(Role.DISTRICT_OFFICER);
    UUID assignmentId = UUID.randomUUID();
    when(assignmentService.createAssignment(any())).thenReturn(dummyDto(assignmentId));

    String body =
        """
        {
          "eventId": "%s",
          "latitude": 6.9271,
          "longitude": 79.8612,
          "locationText": "Wellampitiya junction",
          "task": "Evacuate trapped families near canal",
          "priority": 1,
          "peopleEstimated": 15
        }
        """
            .formatted(UUID.randomUUID());

    mvc.perform(post("/api/assignments").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.id").value(assignmentId.toString()))
        .andExpect(jsonPath("$.data.task").value("Evacuate trapped families"));
  }

  @Test
  void create_invalidBody_returns400() throws Exception {
    signedInAs(Role.DISTRICT_OFFICER);

    // Missing task and location
    String body =
        """
        {
          "eventId": "%s",
          "latitude": 6.9271,
          "longitude": 79.8612,
          "priority": 1,
          "peopleEstimated": 15
        }
        """
            .formatted(UUID.randomUUID());

    mvc.perform(post("/api/assignments").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
  }

  @Test
  void create_unauthorizedRole_returns403() throws Exception {
    signedInAs(Role.RESCUE_MEMBER);
    String body =
        """
        {
          "eventId": "%s",
          "latitude": 6.9271,
          "longitude": 79.8612,
          "locationText": "Wellampitiya",
          "task": "Evacuate",
          "priority": 1,
          "peopleEstimated": 5
        }
        """
            .formatted(UUID.randomUUID());

    mvc.perform(post("/api/assignments").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isForbidden());
  }

  @Test
  void assign_validRequest_returns200() throws Exception {
    signedInAs(Role.DISTRICT_OFFICER);
    UUID id = UUID.randomUUID();
    UUID teamId = UUID.randomUUID();
    when(assignmentService.assignTeam(eq(id), eq(teamId))).thenReturn(dummyDto(id));

    mvc.perform(
            post("/api/assignments/{id}/assign", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"teamId\": \"%s\"}".formatted(teamId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(id.toString()));
  }

  @Test
  void assign_missingTeamId_returns400() throws Exception {
    signedInAs(Role.DISTRICT_OFFICER);
    UUID id = UUID.randomUUID();

    mvc.perform(
            post("/api/assignments/{id}/assign", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void cancel_validRequest_returns200() throws Exception {
    signedInAs(Role.DISTRICT_OFFICER);
    UUID id = UUID.randomUUID();
    when(assignmentService.cancelAssignment(eq(id), eq("No longer needed")))
        .thenReturn(dummyDto(id));

    mvc.perform(
            post("/api/assignments/{id}/cancel", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\": \"No longer needed\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(id.toString()));
  }

  @Test
  void cancel_blankReason_returns400() throws Exception {
    signedInAs(Role.DISTRICT_OFFICER);
    UUID id = UUID.randomUUID();

    mvc.perform(
            post("/api/assignments/{id}/cancel", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\": \"\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void list_unpagedAndPaged_returns200() throws Exception {
    signedInAs(Role.DISTRICT_OFFICER);
    UUID id = UUID.randomUUID();
    when(assignmentService.listAssignments(any(), any())).thenReturn(List.of(dummyDto(id)));
    when(assignmentService.getAssignments(any(), any(), eq(0), eq(10)))
        .thenReturn(new PageImpl<>(List.of(dummyDto(id))));

    mvc.perform(get("/api/assignments")).andExpect(status().isOk());

    mvc.perform(get("/api/assignments").param("page", "0").param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].id").value(id.toString()))
        .andExpect(jsonPath("$.meta.page").value(0))
        .andExpect(jsonPath("$.meta.size").value(1))
        .andExpect(jsonPath("$.meta.totalElements").value(1));

    // Only page param (unpaged fallback)
    mvc.perform(get("/api/assignments").param("page", "0")).andExpect(status().isOk());
    // Only size param (unpaged fallback)
    mvc.perform(get("/api/assignments").param("size", "10")).andExpect(status().isOk());
  }

  @Test
  void get_authorizedRoles_returns200() throws Exception {
    signedInAs(Role.RESCUE_MEMBER);
    UUID id = UUID.randomUUID();
    when(assignmentService.getAssignment(id)).thenReturn(dummyDto(id));

    mvc.perform(get("/api/assignments/{id}", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(id.toString()));

    signedInAs(Role.CITIZEN);
    mvc.perform(get("/api/assignments/{id}", id)).andExpect(status().isForbidden());
  }

  @Test
  void mine_rescueMember_returns200Or204() throws Exception {
    signedInAs(Role.RESCUE_MEMBER);
    UUID id = UUID.randomUUID();
    when(assignmentService.getMyAssignment()).thenReturn(dummyDto(id));

    mvc.perform(get("/api/assignments/mine"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(id.toString()));

    when(assignmentService.getMyAssignment()).thenReturn(null);
    mvc.perform(get("/api/assignments/mine")).andExpect(status().isNoContent());

    signedInAs(Role.DISTRICT_OFFICER);
    mvc.perform(get("/api/assignments/mine")).andExpect(status().isForbidden());
  }

  @Test
  void respond_rescueMember_returns200() throws Exception {
    signedInAs(Role.RESCUE_MEMBER);
    UUID id = UUID.randomUUID();
    when(assignmentService.respond(eq(id), eq(true), any())).thenReturn(dummyDto(id));

    mvc.perform(
            post("/api/assignments/{id}/respond", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accept\": true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(id.toString()));

    signedInAs(Role.DISTRICT_OFFICER);
    mvc.perform(
            post("/api/assignments/{id}/respond", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accept\": true}"))
        .andExpect(status().isForbidden());
  }
}
