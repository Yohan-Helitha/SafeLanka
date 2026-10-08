package lk.dmc.disaster.warnings.entity;

import static lk.dmc.disaster.warnings.entity.EntityFixtures.DISTRICT;
import static lk.dmc.disaster.warnings.entity.EntityFixtures.HAZARD_ID;
import static lk.dmc.disaster.warnings.entity.EntityFixtures.NOW;
import static lk.dmc.disaster.warnings.entity.EntityFixtures.OFFICER;
import static lk.dmc.disaster.warnings.entity.EntityFixtures.activeWarning;
import static lk.dmc.disaster.warnings.entity.EntityFixtures.content;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.shared.domain.WarningLevel;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import org.junit.jupiter.api.Test;

class WarningTest {

  private static void assertCode(Throwable thrown, ErrorCode expected) {
    assertThat(thrown)
        .isInstanceOfSatisfying(AppException.class, e -> assertThat(e.code()).isEqualTo(expected));
  }

  @Test
  void publish_validDraft_createsActiveWarningWithTargetsAndEvidence() {
    UUID report = UUID.randomUUID();
    WarningDraft draft =
        new WarningDraft(
            HAZARD_ID,
            null,
            WarningLevel.WARNING,
            EntityFixtures.districts(),
            content(),
            Set.of(report));

    Warning warning = Warning.publish(draft, OFFICER, NOW);

    assertThat(warning.getStatus()).isEqualTo(WarningStatus.ACTIVE);
    assertThat(warning.getTargetType()).isEqualTo(TargetType.DISTRICT);
    assertThat(warning.getIssuedBy()).isEqualTo(OFFICER);
    assertThat(warning.getIssuedAt()).isEqualTo(NOW);
    assertThat(warning.levelHistory())
        .singleElement()
        .satisfies(
            first -> {
              assertThat(first.getFromLevel()).isNull();
              assertThat(first.getToLevel()).isEqualTo(WarningLevel.WARNING);
              assertThat(first.getChangedBy()).isEqualTo(OFFICER);
              assertThat(first.getChangedAt()).isEqualTo(NOW);
            });
    assertThat(warning.levelChangedAt()).isEqualTo(NOW);
    assertThat(warning.targetAreas())
        .extracting(WarningTargetArea::getDistrictId)
        .containsExactly(DISTRICT);
    assertThat(warning.evidenceReportIds()).containsExactly(report);
    assertThat(warning.content()).isEqualTo(content());
  }

  @Test
  void publish_riverBasinTarget_storesBasinAreas() {
    UUID basin = UUID.randomUUID();
    WarningTarget target = new WarningTarget(TargetType.RIVER_BASIN, Set.of(), Set.of(basin));

    Warning warning =
        Warning.publish(
            new WarningDraft(HAZARD_ID, null, WarningLevel.WATCH, target, content(), Set.of()),
            OFFICER,
            NOW);

    assertThat(warning.target().riverBasinIds()).containsExactly(basin);
    assertThat(warning.target().districtIds()).isEmpty();
    assertThat(warning.evidenceReportIds()).isEmpty();
  }

  @Test
  void publish_contentExactlyAtLimits_isAccepted() {
    WarningContent atLimits =
        new WarningContent(
            "x".repeat(WarningRules.TITLE_MAX),
            "x".repeat(WarningRules.MESSAGE_MAX),
            "x".repeat(WarningRules.SMS_MAX),
            "x".repeat(WarningRules.INSTRUCTIONS_MAX));

    assertThat(atLimits.smsText()).hasSize(160);
  }

  @Test
  void updateContent_activeWarning_replacesTextsAndKeepsLevel() {
    Warning warning = activeWarning(WarningLevel.WATCH);
    WarningContent edited =
        new WarningContent(
            "Updated title", "Updated message body.", "Updated SMS text here.", "Updated steps.");

    warning.updateContent(edited);

    assertThat(warning.content()).isEqualTo(edited);
    assertThat(warning.getLevel()).isEqualTo(WarningLevel.WATCH);
  }

  @Test
  void updateContent_cancelledWarning_isConflict() {
    Warning warning = activeWarning(WarningLevel.WATCH);
    warning.cancel("Water level receded", NOW);

    assertThatThrownBy(() -> warning.updateContent(content()))
        .satisfies(e -> assertCode(e, ErrorCode.INVALID_STATE_TRANSITION));
  }

  @Test
  void escalateTo_higherLevel_raisesTheSameWarningAndRecordsTheChange() {
    Warning warning = activeWarning(WarningLevel.WATCH);
    UUID id = warning.getId();
    WarningTarget target = warning.target();
    Instant later = NOW.plusSeconds(60);

    warning.escalateTo(WarningLevel.EVACUATE, warning.content(), OFFICER, later);

    assertThat(warning.getId()).isEqualTo(id);
    assertThat(warning.getStatus()).isEqualTo(WarningStatus.ACTIVE);
    assertThat(warning.getLevel()).isEqualTo(WarningLevel.EVACUATE);
    assertThat(warning.target()).isEqualTo(target);
    assertThat(warning.getIssuedAt()).isEqualTo(NOW);
    assertThat(warning.levelChangedAt()).isEqualTo(later);
    assertThat(warning.levelHistory())
        .extracting(
            WarningLevelChange::getFromLevel,
            WarningLevelChange::getToLevel,
            WarningLevelChange::getChangedAt)
        .containsExactly(
            tuple(null, WarningLevel.WATCH, NOW),
            tuple(WarningLevel.WATCH, WarningLevel.EVACUATE, later));
  }

  @Test
  void escalateTo_twice_keepsTheWholePathInOrder() {
    Warning warning = activeWarning(WarningLevel.ADVISORY);

    warning.escalateTo(WarningLevel.WARNING, content(), OFFICER, NOW.plusSeconds(60));
    warning.escalateTo(WarningLevel.EVACUATE, content(), OFFICER, NOW.plusSeconds(120));

    assertThat(warning.levelHistory())
        .extracting(WarningLevelChange::getToLevel)
        .containsExactly(WarningLevel.ADVISORY, WarningLevel.WARNING, WarningLevel.EVACUATE);
  }

  @Test
  void escalateTo_newContent_replacesTheTexts() {
    Warning warning = activeWarning(WarningLevel.WATCH);
    WarningContent evacuate =
        new WarningContent(
            "Evacuate Kelani banks",
            "Leave the Kelani river banks immediately.",
            "DMC: Evacuate Kelani banks now.",
            "Go to the nearest safe centre.");

    warning.escalateTo(WarningLevel.EVACUATE, evacuate, OFFICER, NOW.plusSeconds(60));

    assertThat(warning.content()).isEqualTo(evacuate);
  }

  @Test
  void levelHistory_isACopy() {
    Warning warning = activeWarning(WarningLevel.WATCH);

    assertThatThrownBy(() -> warning.levelHistory().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void escalateTo_sameLevel_isBusinessRuleAndLeavesWarningActive() {
    Warning warning = activeWarning(WarningLevel.WARNING);

    assertThatThrownBy(() -> warning.escalateTo(WarningLevel.WARNING, content(), OFFICER, NOW))
        .satisfies(e -> assertCode(e, ErrorCode.BUSINESS_RULE));
    assertThat(warning.isActive()).isTrue();
    assertThat(warning.levelHistory()).hasSize(1);
  }

  @Test
  void escalateTo_lowerLevel_isBusinessRule() {
    Warning warning = activeWarning(WarningLevel.WARNING);

    assertThatThrownBy(() -> warning.escalateTo(WarningLevel.ADVISORY, content(), OFFICER, NOW))
        .satisfies(e -> assertCode(e, ErrorCode.BUSINESS_RULE));
  }

  @Test
  void escalateTo_nonActiveWarning_isConflict() {
    Warning warning = activeWarning(WarningLevel.WATCH);
    warning.cancel("Issued by mistake", NOW);

    assertThatThrownBy(() -> warning.escalateTo(WarningLevel.EVACUATE, content(), OFFICER, NOW))
        .satisfies(e -> assertCode(e, ErrorCode.INVALID_STATE_TRANSITION));
  }

  @Test
  void cancel_withReason_marksCancelled() {
    Warning warning = activeWarning(WarningLevel.WATCH);

    warning.cancel("  Water level receded  ", NOW.plusSeconds(5));

    assertThat(warning.getStatus()).isEqualTo(WarningStatus.CANCELLED);
    assertThat(warning.getCancelReason()).isEqualTo("Water level receded");
    assertThat(warning.getCancelledAt()).isEqualTo(NOW.plusSeconds(5));
    assertThat(warning.isActive()).isFalse();
  }

  @Test
  void cancel_blankOrTooShortReason_isValidationError() {
    Warning warning = activeWarning(WarningLevel.WATCH);

    assertThatThrownBy(() -> warning.cancel("  ", NOW))
        .satisfies(e -> assertCode(e, ErrorCode.VALIDATION_ERROR));
    assertThatThrownBy(() -> warning.cancel("no", NOW))
        .satisfies(e -> assertCode(e, ErrorCode.VALIDATION_ERROR));
    assertThat(warning.isActive()).isTrue();
  }

  @Test
  void cancel_alreadyCancelled_isConflict() {
    Warning warning = activeWarning(WarningLevel.WATCH);
    warning.cancel("Water level receded", NOW);

    assertThatThrownBy(() -> warning.cancel("Cancelling again", NOW))
        .satisfies(e -> assertCode(e, ErrorCode.INVALID_STATE_TRANSITION));
  }

  @Test
  void returnedCollections_cannotBeUsedToChangeTheWarning() {
    Warning warning = activeWarning(WarningLevel.WATCH);

    assertThatThrownBy(() -> warning.targetAreas().clear())
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> warning.evidenceReportIds().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
