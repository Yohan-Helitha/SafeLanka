package lk.dmc.disaster.warnings.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import lk.dmc.disaster.shared.domain.Role;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import lk.dmc.disaster.warnings.entity.Hazard;
import lk.dmc.disaster.warnings.entity.HazardStatus;
import lk.dmc.disaster.warnings.integration.HazardTypeDirectory;
import lk.dmc.disaster.warnings.mapper.HazardMapper;
import lk.dmc.disaster.warnings.service.CreateHazardCommand;
import lk.dmc.disaster.warnings.service.HazardAssessmentService;
import lk.dmc.disaster.warnings.service.HazardListEntry;
import lk.dmc.disaster.warnings.service.HazardQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(MockitoExtension.class)
class HazardControllerTest extends ControllerTestSupport {

  @Mock private HazardQueryService query;
  @Mock private HazardAssessmentService assessment;
  @Mock private HazardTypeDirectory hazardTypes;

  private MockMvc mvc;
  private Hazard hazard;

  @BeforeEach
  void setUp() {
    mvc = mvcFor(new HazardController(query, assessment, new HazardMapper(hazardTypes)));
    hazard = ControllerFixtures.hazard();
  }

  private void hazardTypesKnown() {
    when(hazardTypes.codesById()).thenReturn(Map.of(ControllerFixtures.HAZARD_TYPE, "FLOOD"));
  }

  private static String createBody(String severity, String description) {
    return """
        {"hazardTypeId": "%s", "severity": %s, "districtId": "%s", "description": "%s"}
        """
        .formatted(ControllerFixtures.HAZARD_TYPE, severity, DISTRICT, description);
  }

  @Test
  void list_returnsItemsWithTheTypeCode_andPassesTheFilters() throws Exception {
    signedInAs(Role.DMC_OFFICER);
    hazardTypesKnown();
    UUID districtId = UUID.randomUUID();
    when(query.list(
            List.of(HazardStatus.UNDER_ASSESSMENT, HazardStatus.WARNED),
            ControllerFixtures.HAZARD_TYPE,
            districtId))
        .thenReturn(List.of(new HazardListEntry(hazard, 2, null)));

    mvc.perform(
            get("/api/hazards")
                .param("status", "UNDER_ASSESSMENT,WARNED")
                .param("hazardTypeId", ControllerFixtures.HAZARD_TYPE.toString())
                .param("districtId", districtId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].hazardTypeCode").value("FLOOD"))
        .andExpect(jsonPath("$.data[0].severity").value(4))
        .andExpect(jsonPath("$.data[0].verifiedReportCount").value(2))
        .andExpect(jsonPath("$.data[0].status").value("UNDER_ASSESSMENT"))
        .andExpect(jsonPath("$.data[0].latestReading").doesNotExist());
  }

  @Test
  void list_withoutFilters_asksForTheDefaults() throws Exception {
    signedInAs(Role.DMC_OFFICER);
    hazardTypesKnown();
    when(query.list(isNull(), isNull(), isNull())).thenReturn(List.of());

    mvc.perform(get("/api/hazards"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isEmpty());
  }

  @Test
  void detail_returnsTheHazardWithEvidenceGaugeAndWarnings() throws Exception {
    signedInAs(Role.DMC_OFFICER);
    hazardTypesKnown();
    when(query.detail(hazard.getId())).thenReturn(ControllerFixtures.detailOf(hazard));

    mvc.perform(get("/api/hazards/{id}", hazard.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(hazard.getId().toString()))
        .andExpect(
            jsonPath("$.data.description").value("Kelani river is rising fast near Hanwella."))
        .andExpect(jsonPath("$.data.evidence").isEmpty())
        .andExpect(jsonPath("$.data.sensor").doesNotExist())
        .andExpect(jsonPath("$.data.warnings").isEmpty());
  }

  @Test
  void detail_unknownHazard_is404WithTheErrorEnvelope() throws Exception {
    signedInAs(Role.DMC_OFFICER);
    UUID id = UUID.randomUUID();
    when(query.detail(id)).thenThrow(new AppException(ErrorCode.NOT_FOUND, "Hazard not found."));

    mvc.perform(get("/api/hazards/{id}", id))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
  }

  @Test
  void create_validBody_is201AndReturnsTheNewHazard() throws Exception {
    signedInAs(Role.DMC_OFFICER);
    hazardTypesKnown();
    when(assessment.create(any(CreateHazardCommand.class))).thenReturn(hazard);
    when(query.detail(hazard.getId())).thenReturn(ControllerFixtures.detailOf(hazard));

    mvc.perform(
            post("/api/hazards")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody("4", "Kelani river is rising fast near Hanwella.")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.source").value("MANUAL"))
        .andExpect(jsonPath("$.data.status").value("UNDER_ASSESSMENT"));

    ArgumentCaptor<CreateHazardCommand> command =
        ArgumentCaptor.forClass(CreateHazardCommand.class);
    verify(assessment).create(command.capture());
    assertThat(command.getValue().severity()).isEqualTo(4);
    assertThat(command.getValue().area().districtId()).isEqualTo(DISTRICT);
  }

  @Test
  void create_badSeverityAndShortDescription_is400WithFieldErrors() throws Exception {
    signedInAs(Role.DMC_OFFICER);

    mvc.perform(
            post("/api/hazards")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody("9", "short")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.error.details.fields.severity").exists())
        .andExpect(jsonPath("$.error.details.fields.description").exists());
    verify(assessment, never()).create(any());
  }

  @Test
  void create_noDistrictAndNoBasin_is400() throws Exception {
    signedInAs(Role.DMC_OFFICER);
    String body =
        """
        {"hazardTypeId": "%s", "severity": 3, "description": "Kelani river is rising fast."}
        """
            .formatted(ControllerFixtures.HAZARD_TYPE);

    mvc.perform(post("/api/hazards").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
  }

  @Test
  void setStatus_resolved_returnsTheUpdatedHazard() throws Exception {
    signedInAs(Role.DMC_OFFICER);
    hazardTypesKnown();
    when(query.detail(hazard.getId())).thenReturn(ControllerFixtures.detailOf(hazard));

    mvc.perform(
            patch("/api/hazards/{id}/status", hazard.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"RESOLVED\", \"note\": \"Water has receded.\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(hazard.getId().toString()));

    verify(assessment).setStatus(hazard.getId(), HazardStatus.RESOLVED);
  }

  @Test
  void setStatus_notAllowedChange_is409() throws Exception {
    signedInAs(Role.DMC_OFFICER);
    UUID id = UUID.randomUUID();
    when(assessment.setStatus(id, HazardStatus.MONITORING))
        .thenThrow(new AppException(ErrorCode.INVALID_STATE_TRANSITION, "Hazard is resolved."));

    mvc.perform(
            patch("/api/hazards/{id}/status", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"MONITORING\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("INVALID_STATE_TRANSITION"));
  }

  @Test
  void setStatus_missingStatus_is400() throws Exception {
    signedInAs(Role.DMC_OFFICER);

    mvc.perform(
            patch("/api/hazards/{id}/status", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.details.fields.status").exists());
  }

  @Test
  void setSeverity_validValue_returnsTheUpdatedHazard() throws Exception {
    signedInAs(Role.DMC_OFFICER);
    hazardTypesKnown();
    when(query.detail(hazard.getId())).thenReturn(ControllerFixtures.detailOf(hazard));

    mvc.perform(
            patch("/api/hazards/{id}/severity", hazard.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"severity\": 4}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(hazard.getId().toString()));

    verify(assessment).setSeverity(hazard.getId(), 4);
  }

  @Test
  void setSeverity_outOfRange_is400() throws Exception {
    signedInAs(Role.DMC_OFFICER);

    mvc.perform(
            patch("/api/hazards/{id}/severity", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"severity\": 6}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.details.fields.severity").exists());
    verify(assessment, never()).setSeverity(any(), org.mockito.ArgumentMatchers.anyInt());
  }

  @Test
  void setSeverity_resolvedHazard_is409() throws Exception {
    signedInAs(Role.DMC_OFFICER);
    UUID id = UUID.randomUUID();
    when(assessment.setSeverity(id, 2))
        .thenThrow(new AppException(ErrorCode.INVALID_STATE_TRANSITION, "Hazard is resolved."));

    mvc.perform(
            patch("/api/hazards/{id}/severity", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"severity\": 2}"))
        .andExpect(status().isConflict());
  }

  @Test
  void everyEndpoint_forOtherRoles_is403() throws Exception {
    signedInAs(Role.DISTRICT_OFFICER);

    mvc.perform(get("/api/hazards"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("FORBIDDEN_ROLE"));
    mvc.perform(
            post("/api/hazards")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody("3", "Kelani river is rising fast.")))
        .andExpect(status().isForbidden());
    verifyNoInteractions(query, assessment);
  }
}
