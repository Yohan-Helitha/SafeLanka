package lk.dmc.disaster.warnings.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lk.dmc.disaster.shared.actor.RequiresRole;
import lk.dmc.disaster.shared.api.ApiResponse;
import lk.dmc.disaster.shared.domain.Role;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import lk.dmc.disaster.warnings.dto.ChannelSettingPatch;
import lk.dmc.disaster.warnings.dto.ChannelSettingResponse;
import lk.dmc.disaster.warnings.dto.SensorResponse;
import lk.dmc.disaster.warnings.dto.SetReadingRequest;
import lk.dmc.disaster.warnings.dto.TickResponse;
import lk.dmc.disaster.warnings.entity.Channel;
import lk.dmc.disaster.warnings.service.ChannelSettingsService;
import lk.dmc.disaster.warnings.service.SensorFeedSimulator;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Demo controls for the simulated gauges and gateways. Only exists in the dev profile, so it can
 * never be reached in a real deployment.
 */
@Profile("dev")
@RestController
@RequestMapping("/api/simulation")
@RequiresRole(Role.DMC_OFFICER)
class SimulationController {

  private final SensorFeedSimulator simulator;
  private final ChannelSettingsService channelSettings;

  SimulationController(SensorFeedSimulator simulator, ChannelSettingsService channelSettings) {
    this.simulator = simulator;
    this.channelSettings = channelSettings;
  }

  @GetMapping("/sensors")
  ApiResponse<List<SensorResponse>> sensors() {
    return ApiResponse.of(simulator.listSensors().stream().map(SensorResponse::from).toList());
  }

  @PostMapping("/sensors/{id}/tick")
  ApiResponse<TickResponse> tick(@PathVariable UUID id) {
    return ApiResponse.of(TickResponse.from(simulator.tick(id)));
  }

  @PostMapping("/sensors/{id}/readings")
  ApiResponse<TickResponse> setReading(
      @PathVariable UUID id, @Valid @RequestBody SetReadingRequest request) {
    return ApiResponse.of(TickResponse.from(simulator.setReading(id, request.value())));
  }

  @GetMapping("/channels")
  ApiResponse<List<ChannelSettingResponse>> channels() {
    return ApiResponse.of(
        channelSettings.findAll().stream().map(ChannelSettingResponse::from).toList());
  }

  @PatchMapping("/channels/{channel}")
  ApiResponse<ChannelSettingResponse> changeChannel(
      @PathVariable String channel, @RequestBody ChannelSettingPatch patch) {
    return ApiResponse.of(
        ChannelSettingResponse.from(channelSettings.change(parse(channel), patch.toChange())));
  }

  /** An unknown channel name is a 404, like an unknown id, not a malformed request. */
  private static Channel parse(String name) {
    try {
      return Channel.valueOf(name.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new AppException(ErrorCode.NOT_FOUND, "Unknown channel " + name + ".");
    }
  }
}
