package lk.dmc.disaster.response.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.response.dto.response.ShelterDto;
import lk.dmc.disaster.response.dto.response.ShelterHeadcountUpdateDto;
import lk.dmc.disaster.response.dto.response.ShelterSuggestionDto;
import lk.dmc.disaster.response.entity.HeadcountUpdateStatus;
import lk.dmc.disaster.response.entity.ShelterStatus;
import lk.dmc.disaster.response.service.DistrictSheltersService;
import lk.dmc.disaster.shared.domain.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(MockitoExtension.class)
class DistrictSheltersControllerTest extends ResponseControllerTestSupport {

  @Mock private DistrictSheltersService shelterService;

  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    mvc = mvcFor(new DistrictSheltersController(shelterService, actingUser));
  }

  private ShelterDto dummyDto(UUID id) {
    return new ShelterDto(
        id,
        "Temple Shelter",
        DISTRICT,
        "Galle Road",
        6.91,
        79.85,
        100,
        40,
        ShelterStatus.OPEN,
        UUID.randomUUID(),
        0L,
        Instant.now(),
        Instant.now());
  }

  private ShelterHeadcountUpdateDto dummyUpdateDto(UUID id, UUID shelterId) {
    return new ShelterHeadcountUpdateDto(
        id,
        shelterId,
        "Temple Shelter",
        DISTRICT,
        60,
        40,
        40,
        100,
        "Dilani Gunasekara",
        "SHELTER_COORDINATOR",
        "20 new arrivals",
        HeadcountUpdateStatus.PENDING,
        Instant.now(),
        null);
  }

  @Test
  void list_authorizedRoles_returns200() throws Exception {
    signedInAs(Role.DISTRICT_OFFICER);
    UUID shelterId = UUID.randomUUID();
    when(shelterService.getShelters(eq(DISTRICT), eq(true)))
        .thenReturn(List.of(dummyDto(shelterId)));

    mvc.perform(
            get("/api/shelters")
                .param("districtId", DISTRICT.toString())
                .param("availableOnly", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].id").value(shelterId.toString()))
        .andExpect(jsonPath("$.data[0].name").value("Temple Shelter"));

    signedInAs(Role.SHELTER_COORDINATOR);
    mvc.perform(get("/api/shelters")).andExpect(status().isOk());
  }

  @Test
  void list_unauthorizedRole_returns403() throws Exception {
    signedInAs(Role.CITIZEN);
    mvc.perform(get("/api/shelters")).andExpect(status().isForbidden());
  }

  @Test
  void suggestions_districtOfficer_returns200() throws Exception {
    signedInAs(Role.DISTRICT_OFFICER);
    UUID shelterId = UUID.randomUUID();
    ShelterSuggestionDto suggestion =
        new ShelterSuggestionDto(
            shelterId, "Temple Shelter", "Galle Road", 6.91, 79.85, 100, 40, 60, 2.5);

    when(shelterService.getSuggestions(eq(DISTRICT), eq(30))).thenReturn(List.of(suggestion));

    mvc.perform(
            get("/api/shelters/suggestions")
                .param("districtId", DISTRICT.toString())
                .param("people", "30"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].id").value(shelterId.toString()))
        .andExpect(jsonPath("$.data[0].availableCapacity").value(60));
  }

  @Test
  void suggestions_otherRole_returns403() throws Exception {
    signedInAs(Role.SHELTER_COORDINATOR);
    mvc.perform(
            get("/api/shelters/suggestions")
                .param("districtId", DISTRICT.toString())
                .param("people", "30"))
        .andExpect(status().isForbidden());
  }

  @Test
  void updateOccupancy_validRequest_returns200() throws Exception {
    signedInAs(Role.DISTRICT_OFFICER);
    UUID shelterId = UUID.randomUUID();
    when(shelterService.updateOccupancy(eq(shelterId), eq(50))).thenReturn(dummyDto(shelterId));

    mvc.perform(
            patch("/api/shelters/{id}/occupancy", shelterId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"occupancy\": 50}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(shelterId.toString()));
  }

  @Test
  void updateOccupancy_negativeValue_returns400() throws Exception {
    signedInAs(Role.DISTRICT_OFFICER);
    UUID shelterId = UUID.randomUUID();

    mvc.perform(
            patch("/api/shelters/{id}/occupancy", shelterId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"occupancy\": -1}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
  }

  @Test
  void updateOccupancy_unauthorizedRole_returns403() throws Exception {
    signedInAs(Role.CITIZEN);
    UUID shelterId = UUID.randomUUID();

    mvc.perform(
            patch("/api/shelters/{id}/occupancy", shelterId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"occupancy\": 20}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void listHeadcountUpdates_districtOfficer_returns200() throws Exception {
    signedInAs(Role.DISTRICT_OFFICER);
    UUID updateId = UUID.randomUUID();
    UUID shelterId = UUID.randomUUID();
    when(shelterService.getHeadcountUpdates(eq(DISTRICT), eq(HeadcountUpdateStatus.PENDING)))
        .thenReturn(List.of(dummyUpdateDto(updateId, shelterId)));

    mvc.perform(
            get("/api/shelters/headcount-updates")
                .param("districtId", DISTRICT.toString())
                .param("status", "PENDING"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].id").value(updateId.toString()))
        .andExpect(jsonPath("$.data[0].reportedOccupancy").value(60));
  }

  @Test
  void applyHeadcountUpdate_districtOfficer_returns200() throws Exception {
    signedInAs(Role.DISTRICT_OFFICER);
    UUID updateId = UUID.randomUUID();
    UUID shelterId = UUID.randomUUID();
    when(shelterService.applyHeadcountUpdate(eq(updateId), eq(null)))
        .thenReturn(dummyDto(shelterId));

    mvc.perform(
            post("/api/shelters/headcount-updates/{id}/apply", updateId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(shelterId.toString()));
  }

  @Test
  void dismissHeadcountUpdate_districtOfficer_returns200() throws Exception {
    signedInAs(Role.DISTRICT_OFFICER);
    UUID updateId = UUID.randomUUID();
    UUID shelterId = UUID.randomUUID();
    when(shelterService.dismissHeadcountUpdate(eq(updateId)))
        .thenReturn(dummyUpdateDto(updateId, shelterId));

    mvc.perform(post("/api/shelters/headcount-updates/{id}/dismiss", updateId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(updateId.toString()));
  }

  @Test
  void createHeadcountUpdate_volunteer_returns200() throws Exception {
    signedInAs(Role.VOLUNTEER);
    UUID shelterId = UUID.randomUUID();
    UUID updateId = UUID.randomUUID();
    when(shelterService.createHeadcountUpdate(any()))
        .thenReturn(dummyUpdateDto(updateId, shelterId));

    mvc.perform(
            post("/api/shelters/headcount-updates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"shelterId\": \""
                        + shelterId
                        + "\", \"reportedOccupancy\": 60, \"message\": \"New evacuees\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(updateId.toString()));
  }
}
