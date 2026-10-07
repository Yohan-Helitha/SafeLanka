package lk.dmc.disaster.warnings.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.shared.domain.WarningLevel;
import lk.dmc.disaster.warnings.entity.TargetType;
import lk.dmc.disaster.warnings.entity.Warning;
import lk.dmc.disaster.warnings.entity.WarningContent;
import lk.dmc.disaster.warnings.entity.WarningDraft;
import lk.dmc.disaster.warnings.entity.WarningTarget;
import org.junit.jupiter.api.Test;

class DeliveryRequestTest {

  @Test
  void of_copiesTheWarningTextsAndTheCitizen() {
    WarningContent content =
        new WarningContent(
            "Kelani flood", "Water is rising fast.", "Kelani flood: move now.", "Leave homes.");
    WarningTarget target = new WarningTarget(TargetType.DISTRICT, Set.of(UUID.randomUUID()), null);
    Warning warning =
        Warning.publish(
            new WarningDraft(
                UUID.randomUUID(), null, WarningLevel.EVACUATE, target, content, Set.of()),
            UUID.randomUUID(),
            Instant.parse("2026-10-06T10:00:00Z"));
    UUID citizen = UUID.randomUUID();

    DeliveryRequest request = DeliveryRequest.of(warning, citizen);

    assertThat(request.warningId()).isEqualTo(warning.getId());
    assertThat(request.citizenId()).isEqualTo(citizen);
    assertThat(request.level()).isEqualTo(WarningLevel.EVACUATE);
    assertThat(request.title()).isEqualTo("Kelani flood");
    assertThat(request.smsText()).isEqualTo("Kelani flood: move now.");
  }
}
