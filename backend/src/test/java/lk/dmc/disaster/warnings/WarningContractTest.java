package lk.dmc.disaster.warnings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.shared.domain.WarningLevel;
import org.junit.jupiter.api.Test;

class WarningContractTest {

  private static final Instant NOW = Instant.parse("2026-10-06T10:00:00Z");

  @Test
  void summary_laterChangeToSourceSet_doesNotLeakIn() {
    Set<UUID> districts = new HashSet<>(Set.of(UUID.randomUUID()));

    ActiveWarningSummary summary =
        new ActiveWarningSummary(
            UUID.randomUUID(),
            WarningLevel.WARNING,
            "Kelani flood",
            "Move to higher ground",
            UUID.randomUUID(),
            NOW,
            districts,
            Set.of());
    districts.clear();

    assertThat(summary.districtIds()).hasSize(1);
  }

  @Test
  void summary_returnedSets_areImmutable() {
    ActiveWarningSummary summary =
        new ActiveWarningSummary(
            UUID.randomUUID(),
            WarningLevel.WATCH,
            "Landslide watch",
            "Stay alert",
            null,
            NOW,
            Set.of(),
            Set.of());

    assertThatThrownBy(() -> summary.riverBasinIds().add(UUID.randomUUID()))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void summary_nullSet_isRejected() {
    assertThatThrownBy(
            () ->
                new ActiveWarningSummary(
                    UUID.randomUUID(), WarningLevel.WATCH, "t", "i", null, NOW, null, Set.of()))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void publishedEvent_copiesAllThreeAreaSets() {
    UUID district = UUID.randomUUID();
    Set<UUID> resolved = new HashSet<>(Set.of(district));

    WarningPublishedEvent event =
        new WarningPublishedEvent(
            UUID.randomUUID(),
            WarningLevel.EVACUATE,
            null,
            Set.of(district),
            Set.of(),
            resolved,
            NOW);
    resolved.clear();

    assertThat(event.resolvedDistrictIds()).containsExactly(district);
    assertThat(event.eventId()).isNull();
  }

  @Test
  void escalatedEvent_keepsLevelsAndCopiesDistricts() {
    Set<UUID> resolved = new HashSet<>(Set.of(UUID.randomUUID()));

    WarningEscalatedEvent event =
        new WarningEscalatedEvent(
            UUID.randomUUID(),
            WarningLevel.WATCH,
            WarningLevel.EVACUATE,
            resolved,
            NOW);
    resolved.clear();

    assertThat(event.newLevel().isHigherThan(event.oldLevel())).isTrue();
    assertThat(event.resolvedDistrictIds()).hasSize(1);
  }

  @Test
  void cancelledEvent_copiesDistrictsAndKeepsReason() {
    Set<UUID> resolved = new HashSet<>(Set.of(UUID.randomUUID()));

    WarningCancelledEvent event =
        new WarningCancelledEvent(UUID.randomUUID(), "Water level receded", resolved, NOW);
    resolved.clear();

    assertThat(event.reason()).isEqualTo("Water level receded");
    assertThat(event.resolvedDistrictIds()).hasSize(1);
  }
}
