package lk.dmc.disaster.warnings.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.shared.domain.Role;
import lk.dmc.disaster.shared.domain.WarningLevel;
import lk.dmc.disaster.warnings.entity.Channel;
import lk.dmc.disaster.warnings.entity.ChannelSetting;
import lk.dmc.disaster.warnings.entity.DeliveryStatus;
import lk.dmc.disaster.warnings.entity.NotificationDelivery;
import lk.dmc.disaster.warnings.mapper.WarningMapper;
import lk.dmc.disaster.warnings.service.AudiencePreview;
import lk.dmc.disaster.warnings.service.AudienceSelection;
import lk.dmc.disaster.warnings.service.AudienceService;
import lk.dmc.disaster.warnings.service.ChannelOutcome;
import lk.dmc.disaster.warnings.service.ChannelSettingsService;
import lk.dmc.disaster.warnings.service.CitizenAlert;
import lk.dmc.disaster.warnings.service.CitizenAlertService;
import lk.dmc.disaster.warnings.service.DeliveryItem;
import lk.dmc.disaster.warnings.service.DeliveryOutcome;
import lk.dmc.disaster.warnings.service.DeliveryQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

/** The audience preview, the delivery results and the citizen alert feed. */
@ExtendWith(MockitoExtension.class)
class WarningReadControllersTest extends ControllerTestSupport {

  @Mock private AudienceService audience;
  @Mock private ChannelSettingsService channelSettings;
  @Mock private DeliveryQueryService deliveries;
  @Mock private CitizenAlertService alerts;

  private static ChannelSetting setting(Channel channel, boolean failing) {
    ChannelSetting setting = BeanUtils.instantiateClass(ChannelSetting.class);
    ReflectionTestUtils.setField(setting, "channel", channel);
    setting.switchEnabled(true);
    setting.switchSimulatedFailure(failing);
    return setting;
  }

  // ---- audience ----------------------------------------------------------------------------

  @Test
  void audience_returnsTheCountResolvedDistrictsAndChannelState() throws Exception {
    signedInAs(Role.DMC_OFFICER);
    UUID gampaha = UUID.randomUUID();
    when(audience.preview(new AudienceSelection(Set.of(), Set.of(BASIN))))
        .thenReturn(new AudiencePreview(24, Set.of(DISTRICT, gampaha)));
    when(channelSettings.findAll())
        .thenReturn(List.of(setting(Channel.PUSH, false), setting(Channel.SMS, true)));

    mvcFor(new AudienceController(audience, channelSettings))
        .perform(get("/api/warnings/audience").param("riverBasinIds", BASIN.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.citizenCount").value(24))
        .andExpect(jsonPath("$.data.resolvedDistrictIds.length()").value(2))
        .andExpect(jsonPath("$.data.channels[1].channel").value("SMS"))
        .andExpect(jsonPath("$.data.channels[1].simulateFailure").value(true));
  }

  @Test
  void audience_bothSelectionsEmpty_is400() throws Exception {
    signedInAs(Role.DMC_OFFICER);

    mvcFor(new AudienceController(audience, channelSettings))
        .perform(get("/api/warnings/audience"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
  }

  @Test
  void audience_asACitizen_is403() throws Exception {
    signedInAs(Role.CITIZEN);

    mvcFor(new AudienceController(audience, channelSettings))
        .perform(get("/api/warnings/audience").param("districtIds", DISTRICT.toString()))
        .andExpect(status().isForbidden());
  }

  // ---- deliveries --------------------------------------------------------------------------

  @Test
  void deliveries_returnsTotalsAndAFilteredPage() throws Exception {
    signedInAs(Role.DMC_OFFICER);
    UUID warningId = UUID.randomUUID();
    NotificationDelivery failed =
        NotificationDelivery.queue(
            warningId, UUID.randomUUID(), Channel.SMS, WarningLevel.WARNING, ControllerFixtures.NOW);
    failed.failed("Simulated gateway timeout");
    when(deliveries.summary(warningId))
        .thenReturn(
            new DeliveryOutcome(24, 70, 2, List.of(new ChannelOutcome(Channel.SMS, 22, 2))));
    when(deliveries.list(
            eq(warningId), eq(DeliveryStatus.FAILED), eq(Channel.SMS), any(Pageable.class)))
        .thenReturn(
            new PageImpl<>(List.of(new DeliveryItem(failed, DISTRICT)), PageRequest.of(0, 20), 1));

    mvcFor(new DeliveryController(deliveries, new WarningMapper()))
        .perform(
            get("/api/warnings/{id}/deliveries", warningId)
                .param("status", "FAILED")
                .param("channel", "SMS"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.summary.failed").value(2))
        .andExpect(jsonPath("$.data.summary.byChannel[0].delivered").value(22))
        .andExpect(jsonPath("$.data.items.content[0].channel").value("SMS"))
        .andExpect(jsonPath("$.data.items.content[0].status").value("FAILED"))
        .andExpect(
            jsonPath("$.data.items.content[0].failureReason").value("Simulated gateway timeout"))
        .andExpect(jsonPath("$.data.items.content[0].districtId").value(DISTRICT.toString()))
        .andExpect(jsonPath("$.data.items.totalElements").value(1));
  }

  @Test
  void deliveries_usesTheDefaultPageAndSize() throws Exception {
    signedInAs(Role.DMC_OFFICER);
    UUID warningId = UUID.randomUUID();
    when(deliveries.summary(warningId)).thenReturn(new DeliveryOutcome(0, 0, 0, List.of()));
    when(deliveries.list(eq(warningId), eq(null), eq(null), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

    mvcFor(new DeliveryController(deliveries, new WarningMapper()))
        .perform(get("/api/warnings/{id}/deliveries", warningId))
        .andExpect(status().isOk());

    ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
    verify(deliveries).list(eq(warningId), eq(null), eq(null), pageable.capture());
    assertThat(pageable.getValue().getPageNumber()).isZero();
    assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
  }

  @Test
  void deliveries_asADistrictOfficer_is403() throws Exception {
    signedInAs(Role.DISTRICT_OFFICER);

    mvcFor(new DeliveryController(deliveries, new WarningMapper()))
        .perform(get("/api/warnings/{id}/deliveries", UUID.randomUUID()))
        .andExpect(status().isForbidden());
  }

  // ---- citizen alerts ----------------------------------------------------------------------

  @Test
  void myAlerts_usesTheCitizensOwnDistrictAndBasin() throws Exception {
    signedInAs(Role.CITIZEN);
    CitizenAlert alert =
        new CitizenAlert(
            UUID.randomUUID(),
            WarningLevel.EVACUATE,
            "Kelani river flood warning",
            "Water is rising fast.",
            "Leave low-lying homes.",
            ControllerFixtures.NOW,
            DISTRICT,
            BASIN,
            true);
    when(alerts.alertsFor(DISTRICT, BASIN)).thenReturn(List.of(alert));

    mvcFor(new CitizenAlertController(alerts, actingUser))
        .perform(get("/api/warnings/active/mine"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].warningId").value(alert.warningId().toString()))
        .andExpect(jsonPath("$.data[0].level").value("EVACUATE"))
        .andExpect(jsonPath("$.data[0].districtId").value(DISTRICT.toString()))
        .andExpect(jsonPath("$.data[0].riverBasinId").value(BASIN.toString()))
        .andExpect(jsonPath("$.data[0].audible").value(true));
  }

  @Test
  void myAlerts_volunteerWithNoAlerts_getsAnEmptyList() throws Exception {
    signedInAs(Role.VOLUNTEER);
    when(alerts.alertsFor(DISTRICT, BASIN)).thenReturn(List.of());

    mvcFor(new CitizenAlertController(alerts, actingUser))
        .perform(get("/api/warnings/active/mine"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isEmpty());
  }

  @Test
  void myAlerts_asDistrictOfficer_getsTheAlertsOfTheirDistrict() throws Exception {
    signedInAs(Role.DISTRICT_OFFICER);
    when(alerts.alertsFor(DISTRICT, BASIN)).thenReturn(List.of());

    mvcFor(new CitizenAlertController(alerts, actingUser))
        .perform(get("/api/warnings/active/mine"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isEmpty());
  }

  @Test
  void myAlerts_asDmcOfficer_isAllowedToo() throws Exception {
    signedInAs(Role.DMC_OFFICER);
    when(alerts.alertsFor(DISTRICT, BASIN)).thenReturn(List.of());

    mvcFor(new CitizenAlertController(alerts, actingUser))
        .perform(get("/api/warnings/active/mine"))
        .andExpect(status().isOk());
  }

  @Test
  void myAlerts_asARescueMember_is403() throws Exception {
    signedInAs(Role.RESCUE_MEMBER);

    mvcFor(new CitizenAlertController(alerts, actingUser))
        .perform(get("/api/warnings/active/mine"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("FORBIDDEN_ROLE"));
  }
}
