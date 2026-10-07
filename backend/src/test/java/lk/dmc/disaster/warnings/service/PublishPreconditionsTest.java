package lk.dmc.disaster.warnings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.shared.domain.WarningLevel;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import lk.dmc.disaster.warnings.entity.Hazard;
import lk.dmc.disaster.warnings.entity.HazardArea;
import lk.dmc.disaster.warnings.entity.HazardStatus;
import lk.dmc.disaster.warnings.entity.TargetType;
import lk.dmc.disaster.warnings.entity.Warning;
import lk.dmc.disaster.warnings.entity.WarningContent;
import lk.dmc.disaster.warnings.entity.WarningDraft;
import lk.dmc.disaster.warnings.entity.WarningStatus;
import lk.dmc.disaster.warnings.entity.WarningTarget;
import lk.dmc.disaster.warnings.integration.VerifiedReportSummary;
import lk.dmc.disaster.warnings.integration.VerifiedReports;
import lk.dmc.disaster.warnings.repository.HazardRepository;
import lk.dmc.disaster.warnings.repository.WarningRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PublishPreconditionsTest {

  private static final Instant NOW = Instant.parse("2026-10-06T10:00:00Z");
  private static final UUID COLOMBO = UUID.randomUUID();
  private static final UUID GAMPAHA = UUID.randomUUID();
  private static final UUID KELANI = UUID.randomUUID();

  @Mock private HazardRepository hazards;
  @Mock private WarningRepository warnings;
  @Mock private VerifiedReports verifiedReports;
  @Mock private AudienceService audience;

  private PublishPreconditions preconditions;
  private Hazard hazard;

  @BeforeEach
  void setUp() {
    preconditions = new PublishPreconditions(hazards, warnings, verifiedReports, audience);
    hazard =
        Hazard.manual(
            UUID.randomUUID(),
            3,
            new HazardArea(COLOMBO, null),
            "Kelani river is rising near Hanwella.",
            null,
            NOW);
    lenient().when(hazards.findById(hazard.getId())).thenReturn(java.util.Optional.of(hazard));
    lenient()
        .when(warnings.findByHazardIdAndStatus(hazard.getId(), WarningStatus.ACTIVE))
        .thenReturn(List.of());
  }

  private static VerifiedReportSummary summary(UUID id) {
    return new VerifiedReportSummary(id, "RPT-1", "FLOOD", "Water over the road.", COLOMBO, NOW);
  }

  private WarningDraft draft(Set<UUID> evidence) {
    WarningContent content =
        new WarningContent(
            "Kelani flood", "Water is rising fast.", "Kelani flood: move now.", "Leave homes.");
    WarningTarget target = new WarningTarget(TargetType.DISTRICT, Set.of(COLOMBO), Set.of());
    return new WarningDraft(hazard.getId(), null, WarningLevel.WARNING, target, content, evidence);
  }

  private static void assertCode(Throwable thrown, ErrorCode expected) {
    assertThat(thrown)
        .isInstanceOfSatisfying(AppException.class, e -> assertThat(e.code()).isEqualTo(expected));
  }

  @Test
  void check_openHazardNoEvidenceNoOverlap_returnsTheHazard() {
    assertThat(preconditions.check(draft(Set.of()), Set.of(COLOMBO))).isSameAs(hazard);
  }

  @Test
  void check_allEvidenceVerified_passes() {
    UUID report = UUID.randomUUID();
    when(verifiedReports.findVerified(Set.of(report))).thenReturn(List.of(summary(report)));

    assertThat(preconditions.check(draft(Set.of(report)), Set.of(COLOMBO))).isSameAs(hazard);
  }

  @Test
  void check_unknownHazard_isNotFound() {
    UUID missing = UUID.randomUUID();
    when(hazards.findById(missing)).thenReturn(java.util.Optional.empty());
    WarningDraft draft =
        new WarningDraft(
            missing,
            null,
            WarningLevel.WATCH,
            draft(Set.of()).target(),
            draft(Set.of()).content(),
            Set.of());

    assertThatThrownBy(() -> preconditions.check(draft, Set.of(COLOMBO)))
        .satisfies(e -> assertCode(e, ErrorCode.NOT_FOUND));
  }

  @Test
  void check_resolvedHazard_isBusinessRule() {
    hazard.assessAs(HazardStatus.RESOLVED);

    assertThatThrownBy(() -> preconditions.check(draft(Set.of()), Set.of(COLOMBO)))
        .satisfies(e -> assertCode(e, ErrorCode.BUSINESS_RULE));
  }

  @Test
  void check_unverifiedEvidence_isBusinessRuleNamingTheReports() {
    UUID verified = UUID.randomUUID();
    UUID pending = UUID.randomUUID();
    when(verifiedReports.findVerified(Set.of(verified, pending)))
        .thenReturn(List.of(summary(verified)));

    assertThatThrownBy(() -> preconditions.check(draft(Set.of(verified, pending)), Set.of(COLOMBO)))
        .isInstanceOfSatisfying(
            AppException.class,
            e -> {
              assertThat(e.code()).isEqualTo(ErrorCode.BUSINESS_RULE);
              assertThat(e.details()).containsEntry("unverifiedReportIds", Set.of(pending));
            });
  }

  @Test
  void check_activeWarningCoveringTheSameDistrict_isConflictWithItsId() {
    Warning existing =
        existingWarning(new WarningTarget(TargetType.DISTRICT, Set.of(COLOMBO), Set.of()));
    when(warnings.findByHazardIdAndStatus(hazard.getId(), WarningStatus.ACTIVE))
        .thenReturn(List.of(existing));
    when(audience.resolveDistricts(AudienceSelection.of(existing.target())))
        .thenReturn(Set.of(COLOMBO));

    assertThatThrownBy(() -> preconditions.check(draft(Set.of()), Set.of(COLOMBO)))
        .isInstanceOfSatisfying(
            AppException.class,
            e -> {
              assertThat(e.code()).isEqualTo(ErrorCode.CONFLICT);
              assertThat(e.details()).containsEntry("warningId", existing.getId());
            });
  }

  @Test
  void check_basinWarningResolvingToAnOverlappingDistrict_isConflict() {
    Warning existing =
        existingWarning(new WarningTarget(TargetType.RIVER_BASIN, Set.of(), Set.of(KELANI)));
    when(warnings.findByHazardIdAndStatus(hazard.getId(), WarningStatus.ACTIVE))
        .thenReturn(List.of(existing));
    when(audience.resolveDistricts(AudienceSelection.of(existing.target())))
        .thenReturn(Set.of(COLOMBO, GAMPAHA));

    assertThatThrownBy(() -> preconditions.check(draft(Set.of()), Set.of(GAMPAHA)))
        .satisfies(e -> assertCode(e, ErrorCode.CONFLICT));
  }

  @Test
  void check_activeWarningForOtherDistrictsOnly_isAllowed() {
    Warning existing =
        existingWarning(new WarningTarget(TargetType.DISTRICT, Set.of(GAMPAHA), Set.of()));
    when(warnings.findByHazardIdAndStatus(hazard.getId(), WarningStatus.ACTIVE))
        .thenReturn(List.of(existing));
    when(audience.resolveDistricts(AudienceSelection.of(existing.target())))
        .thenReturn(Set.of(GAMPAHA));

    assertThat(preconditions.check(draft(Set.of()), Set.of(COLOMBO))).isSameAs(hazard);
  }

  private Warning existingWarning(WarningTarget target) {
    WarningDraft existing =
        new WarningDraft(
            hazard.getId(), null, WarningLevel.WATCH, target, draft(Set.of()).content(), Set.of());
    return Warning.publish(existing, UUID.randomUUID(), NOW);
  }
}
