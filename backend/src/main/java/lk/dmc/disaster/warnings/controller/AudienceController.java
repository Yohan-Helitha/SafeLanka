package lk.dmc.disaster.warnings.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.shared.actor.RequiresRole;
import lk.dmc.disaster.shared.api.ApiResponse;
import lk.dmc.disaster.shared.domain.Role;
import lk.dmc.disaster.warnings.dto.AudienceResponse;
import lk.dmc.disaster.warnings.dto.ChannelSettingResponse;
import lk.dmc.disaster.warnings.service.AudiencePreview;
import lk.dmc.disaster.warnings.service.AudienceSelection;
import lk.dmc.disaster.warnings.service.AudienceService;
import lk.dmc.disaster.warnings.service.ChannelSettingsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The audience preview shown while an officer composes a warning. */
@Tag(name = "Warnings")
@RestController
@RequestMapping("/api/warnings/audience")
@RequiresRole(Role.DMC_OFFICER)
class AudienceController {

  private final AudienceService audience;
  private final ChannelSettingsService channelSettings;

  AudienceController(AudienceService audience, ChannelSettingsService channelSettings) {
    this.audience = audience;
    this.channelSettings = channelSettings;
  }

  @Operation(
      summary = "How many people a warning would reach",
      description = "Give districtIds, riverBasinIds, or both. 400 when both are empty.")
  @GetMapping
  ApiResponse<AudienceResponse> preview(
      @RequestParam(required = false) Set<UUID> districtIds,
      @RequestParam(required = false) Set<UUID> riverBasinIds) {
    AudiencePreview preview = audience.preview(new AudienceSelection(districtIds, riverBasinIds));
    return ApiResponse.of(
        new AudienceResponse(
            preview.citizenCount(),
            preview.resolvedDistrictIds(),
            channelSettings.findAll().stream().map(ChannelSettingResponse::from).toList()));
  }
}
