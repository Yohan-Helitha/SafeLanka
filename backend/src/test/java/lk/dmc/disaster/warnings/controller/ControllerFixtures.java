package lk.dmc.disaster.warnings.controller;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.shared.domain.WarningLevel;
import lk.dmc.disaster.warnings.entity.Channel;
import lk.dmc.disaster.warnings.entity.Hazard;
import lk.dmc.disaster.warnings.entity.HazardArea;
import lk.dmc.disaster.warnings.entity.TargetType;
import lk.dmc.disaster.warnings.entity.Warning;
import lk.dmc.disaster.warnings.entity.WarningContent;
import lk.dmc.disaster.warnings.entity.WarningDraft;
import lk.dmc.disaster.warnings.entity.WarningTarget;
import lk.dmc.disaster.warnings.service.ChannelOutcome;
import lk.dmc.disaster.warnings.service.DeliveryOutcome;
import lk.dmc.disaster.warnings.service.HazardDetailView;
import lk.dmc.disaster.warnings.service.HazardListEntry;
import lk.dmc.disaster.warnings.service.WarningView;

/** Shared test data for the controller and mapper tests. */
final class ControllerFixtures {

  static final Instant NOW = Instant.parse("2026-10-06T10:00:00Z");
  static final UUID OFFICER = UUID.randomUUID();
  static final UUID HAZARD_TYPE = UUID.randomUUID();

  private ControllerFixtures() {}

  static WarningContent content() {
    return new WarningContent(
        "Kelani river flood warning",
        "The Kelani river is above its major flood level at Hanwella.",
        "DMC: Kelani flood. Move to higher ground now.",
        "Leave low-lying homes.");
  }

  static Warning warning(WarningLevel level) {
    WarningTarget target =
        new WarningTarget(TargetType.DISTRICT, Set.of(ControllerTestSupport.DISTRICT), Set.of());
    WarningDraft draft =
        new WarningDraft(UUID.randomUUID(), null, level, target, content(), Set.of());
    return Warning.publish(draft, OFFICER, NOW);
  }

  static WarningView view(Warning warning) {
    DeliveryOutcome outcome =
        new DeliveryOutcome(24, 70, 2, List.of(new ChannelOutcome(Channel.SMS, 22, 2)));
    return new WarningView(
        warning,
        warning.target(),
        warning.evidenceReportIds(),
        Set.of(ControllerTestSupport.DISTRICT),
        outcome);
  }

  static Hazard hazard() {
    return Hazard.manual(
        HAZARD_TYPE,
        4,
        new HazardArea(ControllerTestSupport.DISTRICT, null),
        "Kelani river is rising fast near Hanwella.",
        null,
        NOW);
  }

  static HazardDetailView detailOf(Hazard hazard) {
    return new HazardDetailView(new HazardListEntry(hazard, 0, null), List.of(), null, List.of());
  }
}
