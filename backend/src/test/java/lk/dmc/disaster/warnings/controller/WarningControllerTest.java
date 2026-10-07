package lk.dmc.disaster.warnings.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import lk.dmc.disaster.shared.actor.ActingUser;
import lk.dmc.disaster.shared.domain.Role;
import lk.dmc.disaster.shared.domain.WarningLevel;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import lk.dmc.disaster.warnings.entity.Warning;
import lk.dmc.disaster.warnings.entity.WarningStatus;
import lk.dmc.disaster.warnings.mapper.WarningMapper;
import lk.dmc.disaster.warnings.service.EscalateCommand;
import lk.dmc.disaster.warnings.service.PublishCommand;
import lk.dmc.disaster.warnings.service.WarningPublicationService;
import lk.dmc.disaster.warnings.service.WarningQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(MockitoExtension.class)
class WarningControllerTest extends ControllerTestSupport {

  @Mock private WarningPublicationService publication;
  @Mock private WarningQueryService query;

  private MockMvc mvc;
  private Warning warning;

  @BeforeEach
  void setUp() {
    mvc = mvcFor(new WarningController(publication, query, new WarningMapper(), actingUser));
    warning = ControllerFixtures.warning(WarningLevel.WARNING);
    queryReturns(warning);
  }

  /** The query service is asked for the finished warning after every change. */
  private void queryReturns(Warning forWarning) {
    org.mockito.Mockito.lenient()
        .when(query.get(forWarning.getId()))
        .thenReturn(ControllerFixtures.view(forWarning));
  }

  private static String publishBody(String title, boolean confirm) {
    return """
        {"hazardId": "%s", "level": "WARNING", "targetType": "DISTRICT",
         "districtIds": ["%s"], "riverBasinIds": [],
         "title": "%s", "message": "The Kelani river is above its major flood level.",
         "smsText": "DMC: Kelani flood. Move to higher ground now.",
         "instructions": "Leave low-lying homes.", "evidenceReportIds": [], "confirm": %s}
        """
        .formatted(UUID.randomUUID(), DISTRICT, title, confirm);
  }

  // ---- publish -----------------------------------------------------------------------------

  @Test
  void publish_validBody_is201WithTheWarningAndDeliveryTotals() throws Exception {
    ActingUser officer = signedInAs(Role.DMC_OFFICER);
    when(publication.publish(any(PublishCommand.class))).thenReturn(warning);

    mvc.perform(
            post("/api/warnings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(publishBody("Kelani river flood warning", true)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.id").value(warning.getId().toString()))
        .andExpect(jsonPath("$.data.level").value("WARNING"))
        .andExpect(jsonPath("$.data.status").value("ACTIVE"))
        .andExpect(jsonPath("$.data.districtIds[0]").value(DISTRICT.toString()))
        .andExpect(jsonPath("$.data.resolvedDistrictIds[0]").value(DISTRICT.toString()))
        .andExpect(jsonPath("$.data.deliverySummary.targeted").value(24))
        .andExpect(jsonPath("$.data.deliverySummary.delivered").value(70))
        .andExpect(jsonPath("$.data.deliverySummary.failed").value(2))
        .andExpect(jsonPath("$.data.deliverySummary.byChannel[0].channel").value("SMS"))
        .andExpect(jsonPath("$.data.deliverySummary.byChannel[0].failed").value(2));

    ArgumentCaptor<PublishCommand> command = ArgumentCaptor.forClass(PublishCommand.class);
    verify(publication).publish(command.capture());
    assertThat(command.getValue().issuedBy()).isEqualTo(officer.id());
    assertThat(command.getValue().confirmed()).isTrue();
    assertThat(command.getValue().draft().level()).isEqualTo(WarningLevel.WARNING);
  }

  @Test
  void publish_shortTitle_is400WithAFieldError() throws Exception {
    signedInAs(Role.DMC_OFFICER);

    mvc.perform(
            post("/api/warnings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(publishBody("Hi", true)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.error.details.fields.title").exists());
    verify(publication, never()).publish(any());
  }

  @Test
  void publish_notConfirmed_is422() throws Exception {
    signedInAs(Role.DMC_OFFICER);
    when(publication.publish(any(PublishCommand.class)))
        .thenThrow(new AppException(ErrorCode.BUSINESS_RULE, "Confirm the warning."));

    mvc.perform(
            post("/api/warnings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(publishBody("Kelani river flood warning", false)))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.error.code").value("BUSINESS_RULE"));
  }

  @Test
  void publish_overlappingActiveWarning_is409WithTheExistingWarningId() throws Exception {
    signedInAs(Role.DMC_OFFICER);
    UUID existing = UUID.randomUUID();
    when(publication.publish(any(PublishCommand.class)))
        .thenThrow(
            new AppException(
                ErrorCode.CONFLICT, "Already covered.", Map.of("warningId", existing)));

    mvc.perform(
            post("/api/warnings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(publishBody("Kelani river flood warning", true)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("CONFLICT"))
        .andExpect(jsonPath("$.error.details.warningId").value(existing.toString()));
  }

  @Test
  void publish_basinTargetWithNoBasin_is400() throws Exception {
    signedInAs(Role.DMC_OFFICER);
    String body =
        publishBody("Kelani river flood warning", true).replace("DISTRICT", "RIVER_BASIN");

    mvc.perform(post("/api/warnings").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
  }

  // ---- update, escalate, cancel ------------------------------------------------------------

  @Test
  void update_validBody_is200() throws Exception {
    signedInAs(Role.DMC_OFFICER);

    mvc.perform(
            put("/api/warnings/{id}", warning.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title": "Kelani flood update", "message": "Water is still rising slowly.",
                     "smsText": "DMC: Kelani flood update. Stay on high ground.",
                     "instructions": "Do not return home yet."}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(warning.getId().toString()));

    verify(publication).updateContent(eq(warning.getId()), any());
  }

  @Test
  void update_warningNotActive_is409() throws Exception {
    signedInAs(Role.DMC_OFFICER);
    when(publication.updateContent(eq(warning.getId()), any()))
        .thenThrow(new AppException(ErrorCode.INVALID_STATE_TRANSITION, "Only ACTIVE."));

    mvc.perform(
            put("/api/warnings/{id}", warning.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title": "Kelani flood update", "message": "Water is still rising slowly.",
                     "smsText": "DMC: Kelani flood update. Stay on high ground.",
                     "instructions": "Do not return home yet."}
                    """))
        .andExpect(status().isConflict());
  }

  @Test
  void escalate_higherLevel_is201WithTheNewWarning() throws Exception {
    ActingUser officer = signedInAs(Role.DMC_OFFICER);
    Warning next = ControllerFixtures.warning(WarningLevel.EVACUATE);
    queryReturns(next);
    when(publication.escalate(any(EscalateCommand.class))).thenReturn(next);

    mvc.perform(
            post("/api/warnings/{id}/escalate", warning.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"level\": \"EVACUATE\", \"confirm\": true}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.id").value(next.getId().toString()))
        .andExpect(jsonPath("$.data.level").value("EVACUATE"));

    ArgumentCaptor<EscalateCommand> command = ArgumentCaptor.forClass(EscalateCommand.class);
    verify(publication).escalate(command.capture());
    assertThat(command.getValue().warningId()).isEqualTo(warning.getId());
    assertThat(command.getValue().level()).isEqualTo(WarningLevel.EVACUATE);
    assertThat(command.getValue().newContent()).isNull();
    assertThat(command.getValue().issuedBy()).isEqualTo(officer.id());
  }

  @Test
  void escalate_sameLevel_is422() throws Exception {
    signedInAs(Role.DMC_OFFICER);
    when(publication.escalate(any(EscalateCommand.class)))
        .thenThrow(new AppException(ErrorCode.BUSINESS_RULE, "Level must be higher."));

    mvc.perform(
            post("/api/warnings/{id}/escalate", warning.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"level\": \"WARNING\", \"confirm\": true}"))
        .andExpect(status().isUnprocessableContent());
  }

  @Test
  void escalate_onlySomeTextGiven_is400() throws Exception {
    signedInAs(Role.DMC_OFFICER);

    mvc.perform(
            post("/api/warnings/{id}/escalate", warning.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"level\": \"EVACUATE\", \"title\": \"Evacuate now\", \"confirm\": true}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    verify(publication, never()).escalate(any());
  }

  @Test
  void cancel_withReason_is200() throws Exception {
    signedInAs(Role.DMC_OFFICER);

    mvc.perform(
            post("/api/warnings/{id}/cancel", warning.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\": \"Kelani water level has fallen.\"}"))
        .andExpect(status().isOk());

    verify(publication).cancel(warning.getId(), "Kelani water level has fallen.");
  }

  @Test
  void cancel_shortReason_is400() throws Exception {
    signedInAs(Role.DMC_OFFICER);

    mvc.perform(
            post("/api/warnings/{id}/cancel", warning.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\": \"no\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.details.fields.reason").exists());
  }

  // ---- list and get ------------------------------------------------------------------------

  @Test
  void list_districtOfficer_isAllowedAndGetsAPage() throws Exception {
    signedInAs(Role.DISTRICT_OFFICER);
    Page<lk.dmc.disaster.warnings.service.WarningView> page =
        new PageImpl<>(List.of(ControllerFixtures.view(warning)), PageRequest.of(0, 20), 1);
    when(query.list(eq(WarningStatus.ACTIVE), eq(null), any(Pageable.class))).thenReturn(page);

    mvc.perform(get("/api/warnings").param("status", "ACTIVE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].id").value(warning.getId().toString()))
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.size").value(20));
  }

  @Test
  void list_hugeSize_isLimitedToOneHundredAndNewestFirst() throws Exception {
    signedInAs(Role.DMC_OFFICER);
    when(query.list(eq(null), eq(null), any(Pageable.class))).thenReturn(Page.empty());

    mvc.perform(get("/api/warnings").param("size", "5000").param("page", "-3"))
        .andExpect(status().isOk());

    ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
    verify(query).list(eq(null), eq(null), pageable.capture());
    assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
    assertThat(pageable.getValue().getPageNumber()).isZero();
    assertThat(pageable.getValue().getSort().getOrderFor("issuedAt").isDescending()).isTrue();
  }

  @Test
  void get_districtOfficer_isAllowed() throws Exception {
    signedInAs(Role.DISTRICT_OFFICER);

    mvc.perform(get("/api/warnings/{id}", warning.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.title").value("Kelani river flood warning"));
  }

  @Test
  void get_unknownWarning_is404() throws Exception {
    signedInAs(Role.DMC_OFFICER);
    UUID id = UUID.randomUUID();
    when(query.get(id)).thenThrow(new AppException(ErrorCode.NOT_FOUND, "Warning not found."));

    mvc.perform(get("/api/warnings/{id}", id))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
  }

  // ---- roles -------------------------------------------------------------------------------

  @Test
  void changingWarnings_asADistrictOfficerOrCitizen_is403() throws Exception {
    signedInAs(Role.DISTRICT_OFFICER);

    mvc.perform(
            post("/api/warnings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(publishBody("Kelani river flood warning", true)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("FORBIDDEN_ROLE"));
    mvc.perform(
            post("/api/warnings/{id}/cancel", warning.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\": \"Kelani water level has fallen.\"}"))
        .andExpect(status().isForbidden());
    verifyNoInteractions(publication);
  }

  @Test
  void listing_asACitizen_is403() throws Exception {
    signedInAs(Role.CITIZEN);

    mvc.perform(get("/api/warnings")).andExpect(status().isForbidden());
  }
}
