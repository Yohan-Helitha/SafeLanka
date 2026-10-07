package lk.dmc.disaster.warnings.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.shared.actor.ActingUser;
import lk.dmc.disaster.shared.actor.ActingUserContext;
import lk.dmc.disaster.shared.actor.RoleInterceptor;
import lk.dmc.disaster.shared.domain.Role;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import lk.dmc.disaster.shared.error.GlobalExceptionHandler;
import lk.dmc.disaster.warnings.entity.Channel;
import lk.dmc.disaster.warnings.entity.ChannelSetting;
import lk.dmc.disaster.warnings.entity.Sensor;
import lk.dmc.disaster.warnings.entity.SensorReading;
import lk.dmc.disaster.warnings.service.ChannelSettingChange;
import lk.dmc.disaster.warnings.service.ChannelSettingsService;
import lk.dmc.disaster.warnings.service.SensorFeedSimulator;
import lk.dmc.disaster.warnings.service.SensorSnapshot;
import lk.dmc.disaster.warnings.service.TickResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Runs the controller with the real error envelope and role check, but no Spring context. */
@ExtendWith(MockitoExtension.class)
class SimulationControllerTest {

  private static final Instant NOW = Instant.parse("2026-10-06T10:00:00Z");

  @Mock private SensorFeedSimulator simulator;
  @Mock private ChannelSettingsService channelSettings;
  @Mock private ActingUserContext actingUser;

  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    mvc =
        MockMvcBuilders.standaloneSetup(new SimulationController(simulator, channelSettings))
            .setControllerAdvice(new GlobalExceptionHandler())
            .addInterceptors(new RoleInterceptor(actingUser))
            .build();
  }

  private void signedInAs(Role role) {
    when(actingUser.require())
        .thenReturn(new ActingUser(UUID.randomUUID(), role, UUID.randomUUID(), null, null));
  }

  private static Sensor sensor() {
    Sensor sensor = BeanUtils.instantiateClass(Sensor.class);
    ReflectionTestUtils.setField(sensor, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(sensor, "code", "KELANI-HANWELLA");
    ReflectionTestUtils.setField(sensor, "name", "Kelani at Hanwella");
    ReflectionTestUtils.setField(sensor, "districtId", UUID.randomUUID());
    ReflectionTestUtils.setField(sensor, "riverBasinId", UUID.randomUUID());
    ReflectionTestUtils.setField(sensor, "alertLevel", new BigDecimal("1.20"));
    ReflectionTestUtils.setField(sensor, "majorFloodLevel", new BigDecimal("2.50"));
    ReflectionTestUtils.setField(sensor, "unit", "m");
    return sensor;
  }

  private static ChannelSetting setting(Channel channel, boolean enabled, boolean failing) {
    ChannelSetting setting = BeanUtils.instantiateClass(ChannelSetting.class);
    ReflectionTestUtils.setField(setting, "channel", channel);
    setting.switchEnabled(enabled);
    setting.switchSimulatedFailure(failing);
    return setting;
  }

  @Test
  void onlyExistsInTheDevProfile() {
    Profile profile =
        AnnotatedElementUtils.findMergedAnnotation(SimulationController.class, Profile.class);

    assertThat(profile).isNotNull();
    assertThat(profile.value()).containsExactly("dev");
  }

  @Test
  void sensors_listsGaugesWithLatestReadingOrNull() throws Exception {
    signedInAs(Role.DMC_OFFICER);
    Sensor withReading = sensor();
    Sensor without = sensor();
    when(simulator.listSensors())
        .thenReturn(
            List.of(
                new SensorSnapshot(
                    withReading,
                    SensorReading.record(withReading.getId(), new BigDecimal("1.74"), NOW)),
                new SensorSnapshot(without, null)));

    mvc.perform(get("/api/simulation/sensors"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].code").value("KELANI-HANWELLA"))
        .andExpect(jsonPath("$.data[0].alertLevel").value(1.20))
        .andExpect(jsonPath("$.data[0].latest.value").value(1.74))
        .andExpect(jsonPath("$.data[1].latest").doesNotExist());
  }

  @Test
  void tick_returnsTheReadingAndWhetherTheThresholdWasCrossed() throws Exception {
    signedInAs(Role.DMC_OFFICER);
    UUID sensorId = UUID.randomUUID();
    UUID hazardId = UUID.randomUUID();
    when(simulator.tick(sensorId))
        .thenReturn(
            new TickResult(
                SensorReading.record(sensorId, new BigDecimal("1.26"), NOW), true, hazardId));

    mvc.perform(post("/api/simulation/sensors/{id}/tick", sensorId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.reading.value").value(1.26))
        .andExpect(jsonPath("$.data.thresholdCrossed").value(true))
        .andExpect(jsonPath("$.data.hazardId").value(hazardId.toString()));
  }

  @Test
  void setReading_passesTheValueToTheSimulator() throws Exception {
    signedInAs(Role.DMC_OFFICER);
    UUID sensorId = UUID.randomUUID();
    when(simulator.setReading(sensorId, new BigDecimal("2.60")))
        .thenReturn(
            new TickResult(
                SensorReading.record(sensorId, new BigDecimal("2.60"), NOW), true, null));

    mvc.perform(
            post("/api/simulation/sensors/{id}/readings", sensorId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"value\": 2.60}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.reading.value").value(2.60))
        .andExpect(jsonPath("$.data.hazardId").doesNotExist());
  }

  @Test
  void setReading_missingValue_is400WithTheErrorEnvelope() throws Exception {
    signedInAs(Role.DMC_OFFICER);

    mvc.perform(
            post("/api/simulation/sensors/{id}/readings", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
  }

  @Test
  void tick_unknownSensor_is404WithTheErrorEnvelope() throws Exception {
    signedInAs(Role.DMC_OFFICER);
    UUID sensorId = UUID.randomUUID();
    when(simulator.tick(sensorId))
        .thenThrow(new AppException(ErrorCode.NOT_FOUND, "Sensor not found."));

    mvc.perform(post("/api/simulation/sensors/{id}/tick", sensorId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
  }

  @Test
  void channels_listsTheDemoSwitches() throws Exception {
    signedInAs(Role.DMC_OFFICER);
    when(channelSettings.findAll())
        .thenReturn(List.of(setting(Channel.PUSH, true, false), setting(Channel.SMS, true, true)));

    mvc.perform(get("/api/simulation/channels"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[1].channel").value("SMS"))
        .andExpect(jsonPath("$.data[1].simulateFailure").value(true));
  }

  @Test
  void patchChannel_changesOnlyTheGivenFlag_andAcceptsLowerCaseNames() throws Exception {
    signedInAs(Role.DMC_OFFICER);
    when(channelSettings.change(eq(Channel.SMS), any(ChannelSettingChange.class)))
        .thenReturn(setting(Channel.SMS, true, true));

    mvc.perform(
            patch("/api/simulation/channels/sms")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"simulateFailure\": true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.channel").value("SMS"))
        .andExpect(jsonPath("$.data.simulateFailure").value(true));
  }

  @Test
  void patchChannel_unknownChannel_is404() throws Exception {
    signedInAs(Role.DMC_OFFICER);

    mvc.perform(
            patch("/api/simulation/channels/FAX")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\": false}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
  }

  @Test
  void patchChannel_emptyBody_is400() throws Exception {
    signedInAs(Role.DMC_OFFICER);

    mvc.perform(
            patch("/api/simulation/channels/SMS")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
  }

  @Test
  void anyEndpoint_asACitizen_is403() throws Exception {
    signedInAs(Role.CITIZEN);

    mvc.perform(get("/api/simulation/sensors"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("FORBIDDEN_ROLE"));
    mvc.perform(post("/api/simulation/sensors/{id}/tick", UUID.randomUUID()))
        .andExpect(status().isForbidden());
  }
}
