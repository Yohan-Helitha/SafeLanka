package lk.dmc.disaster.warnings.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.shared.domain.WarningLevel;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import lk.dmc.disaster.warnings.entity.TargetType;
import lk.dmc.disaster.warnings.service.CitizenAlert;
import lk.dmc.disaster.warnings.service.EscalateCommand;
import lk.dmc.disaster.warnings.service.PublishCommand;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class RequestDtoTest {

  private static final UUID OFFICER = UUID.randomUUID();
  private static final UUID DISTRICT = UUID.randomUUID();
  private static final UUID BASIN = UUID.randomUUID();

  private static PublishWarningRequest publishRequest(
      TargetType type, Set<UUID> districts, Set<UUID> basins) {
    return new PublishWarningRequest(
        UUID.randomUUID(),
        null,
        WarningLevel.WARNING,
        type,
        districts,
        basins,
        "Kelani river flood warning",
        "The Kelani river is above its major flood level.",
        "DMC: Kelani flood. Move to higher ground now.",
        "Leave low-lying homes.",
        null,
        true);
  }

  @Test
  void publishRequest_toCommand_buildsTheDraftAndKeepsTheIssuerAndConfirmation() {
    PublishCommand command =
        publishRequest(TargetType.RIVER_BASIN, null, Set.of(BASIN)).toCommand(OFFICER);

    assertThat(command.issuedBy()).isEqualTo(OFFICER);
    assertThat(command.confirmed()).isTrue();
    assertThat(command.draft().target().riverBasinIds()).containsExactly(BASIN);
    assertThat(command.draft().evidenceReportIds()).isEmpty();
    assertThat(command.draft().content().title()).isEqualTo("Kelani river flood warning");
  }

  @Test
  void publishRequest_targetWithoutAreas_isValidationError() {
    assertThatThrownBy(
            () -> publishRequest(TargetType.DISTRICT, Set.of(), Set.of()).toCommand(OFFICER))
        .isInstanceOfSatisfying(
            AppException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    assertThatThrownBy(
            () ->
                publishRequest(TargetType.DISTRICT, Set.of(DISTRICT), Set.of(BASIN))
                    .toCommand(OFFICER))
        .isInstanceOf(AppException.class);
  }

  @Test
  void escalateRequest_noText_keepsTheCurrentTexts() {
    EscalateCommand command =
        new EscalateWarningRequest(WarningLevel.EVACUATE, null, null, null, null, true)
            .toCommand(UUID.randomUUID(), OFFICER);

    assertThat(command.newContent()).isNull();
    assertThat(command.level()).isEqualTo(WarningLevel.EVACUATE);
    assertThat(command.confirmed()).isTrue();
  }

  @Test
  void escalateRequest_allFourTexts_replacesTheTexts() {
    EscalateCommand command =
        new EscalateWarningRequest(
                WarningLevel.EVACUATE,
                "Evacuate Kelani banks",
                "Leave the Kelani river banks immediately.",
                "DMC: Evacuate Kelani banks now.",
                "Go to the nearest safe centre.",
                true)
            .toCommand(UUID.randomUUID(), OFFICER);

    assertThat(command.newContent().title()).isEqualTo("Evacuate Kelani banks");
  }

  @Test
  void escalateRequest_someTextMissing_isValidationErrorNamingTheField() {
    assertThatThrownBy(
            () ->
                new EscalateWarningRequest(
                        WarningLevel.EVACUATE, "Evacuate now", null, null, null, true)
                    .toCommand(UUID.randomUUID(), OFFICER))
        .isInstanceOfSatisfying(
            AppException.class, e -> assertThat(e.details()).containsEntry("field", "message"));
  }

  @Test
  void channelSettingPatch_toChange_keepsMissingFlagsNull() {
    var change = new ChannelSettingPatch(null, true).toChange();

    assertThat(change.enabled()).isNull();
    assertThat(change.simulateFailure()).isTrue();
  }

  @Test
  void pageResponse_copiesThePagingFieldsAndMapsTheContent() {
    PageImpl<Integer> page = new PageImpl<>(List.of(1, 2), PageRequest.of(1, 2), 5);

    PageResponse<String> response = PageResponse.of(page, n -> "n" + n);

    assertThat(response.content()).containsExactly("n1", "n2");
    assertThat(response.page()).isEqualTo(1);
    assertThat(response.size()).isEqualTo(2);
    assertThat(response.totalElements()).isEqualTo(5);
    assertThat(response.totalPages()).isEqualTo(3);
  }

  @Test
  void citizenAlertResponse_from_copiesEveryField() {
    CitizenAlert alert =
        new CitizenAlert(
            UUID.randomUUID(),
            WarningLevel.WATCH,
            "Kelani watch",
            "Water is high.",
            "Stay alert.",
            Instant.parse("2026-10-06T10:00:00Z"),
            UUID.randomUUID(),
            null,
            false);

    CitizenAlertResponse response = CitizenAlertResponse.from(alert);

    assertThat(response.warningId()).isEqualTo(alert.warningId());
    assertThat(response.audible()).isFalse();
    assertThat(response.title()).isEqualTo("Kelani watch");
  }
}
