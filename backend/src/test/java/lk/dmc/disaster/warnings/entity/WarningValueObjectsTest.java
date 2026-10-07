package lk.dmc.disaster.warnings.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.shared.domain.WarningLevel;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import org.junit.jupiter.api.Test;

class WarningValueObjectsTest {

  private static final String OK = "valid text here";

  private static void assertValidationOn(Throwable thrown, String field) {
    assertThat(thrown)
        .isInstanceOfSatisfying(
            AppException.class,
            e -> {
              assertThat(e.code()).isEqualTo(ErrorCode.VALIDATION_ERROR);
              assertThat(e.details()).containsEntry("field", field);
            });
  }

  @Test
  void content_surroundingSpaces_areTrimmed() {
    WarningContent content = new WarningContent("  Flood alert  ", OK, OK, OK);

    assertThat(content.title()).isEqualTo("Flood alert");
  }

  @Test
  void content_smsOver160Characters_isRejected() {
    assertThatThrownBy(() -> new WarningContent("Flood", OK, "x".repeat(161), OK))
        .satisfies(e -> assertValidationOn(e, "smsText"));
  }

  @Test
  void content_eachTextTooShort_namesTheField() {
    assertThatThrownBy(() -> new WarningContent("abc", OK, OK, OK))
        .satisfies(e -> assertValidationOn(e, "title"));
    assertThatThrownBy(() -> new WarningContent("Flood", "short", OK, OK))
        .satisfies(e -> assertValidationOn(e, "message"));
    assertThatThrownBy(() -> new WarningContent("Flood", OK, "short", OK))
        .satisfies(e -> assertValidationOn(e, "smsText"));
    assertThatThrownBy(() -> new WarningContent("Flood", OK, OK, "abc"))
        .satisfies(e -> assertValidationOn(e, "instructions"));
  }

  @Test
  void content_nullText_isRejected() {
    assertThatThrownBy(() -> new WarningContent(null, OK, OK, OK))
        .satisfies(e -> assertValidationOn(e, "title"));
  }

  @Test
  void target_districtTypeWithDistricts_isAccepted() {
    UUID district = UUID.randomUUID();

    WarningTarget target = new WarningTarget(TargetType.DISTRICT, Set.of(district), null);

    assertThat(target.districtIds()).containsExactly(district);
    assertThat(target.riverBasinIds()).isEmpty();
  }

  @Test
  void target_noAreaForTheChosenType_isRejected() {
    assertThatThrownBy(() -> new WarningTarget(TargetType.RIVER_BASIN, Set.of(), Set.of()))
        .satisfies(e -> assertValidationOn(e, "riverBasinIds"));
    assertThatThrownBy(
            () -> new WarningTarget(TargetType.DISTRICT, Set.of(), Set.of(UUID.randomUUID())))
        .satisfies(e -> assertValidationOn(e, "districtIds"));
  }

  @Test
  void target_bothKindsOfArea_isRejected() {
    assertThatThrownBy(
            () ->
                new WarningTarget(
                    TargetType.DISTRICT, Set.of(UUID.randomUUID()), Set.of(UUID.randomUUID())))
        .satisfies(e -> assertValidationOn(e, "districtIds"));
  }

  @Test
  void hazardArea_needsAtLeastOneArea() {
    assertThatThrownBy(() -> new HazardArea(null, null))
        .satisfies(e -> assertValidationOn(e, "districtId"));
    assertThat(new HazardArea(null, UUID.randomUUID()).districtId()).isNull();
  }

  @Test
  void draft_nullEvidence_becomesEmpty() {
    WarningDraft draft =
        new WarningDraft(
            UUID.randomUUID(),
            null,
            WarningLevel.WATCH,
            EntityFixtures.districts(),
            EntityFixtures.content(),
            null);

    assertThat(draft.evidenceReportIds()).isEmpty();
  }

  @Test
  void audible_onlyFromWarningLevelUp() {
    assertThat(WarningRules.audibleAllowedAt(WarningLevel.ADVISORY)).isFalse();
    assertThat(WarningRules.audibleAllowedAt(WarningLevel.WATCH)).isFalse();
    assertThat(WarningRules.audibleAllowedAt(WarningLevel.WARNING)).isTrue();
    assertThat(WarningRules.audibleAllowedAt(WarningLevel.EVACUATE)).isTrue();
  }

  @Test
  void severity_outsideOneToFive_isRejected() {
    assertThatThrownBy(() -> WarningRules.requireSeverity(0))
        .satisfies(e -> assertValidationOn(e, "severity"));
    assertThatThrownBy(() -> WarningRules.requireSeverity(6))
        .satisfies(e -> assertValidationOn(e, "severity"));
    assertThat(WarningRules.requireSeverity(5)).isEqualTo(5);
  }
}
