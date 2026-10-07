package lk.dmc.disaster.warnings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.shared.domain.WarningLevel;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import lk.dmc.disaster.warnings.ActiveWarningSummary;
import lk.dmc.disaster.warnings.entity.Channel;
import lk.dmc.disaster.warnings.entity.DeliveryStatus;
import lk.dmc.disaster.warnings.entity.NotificationDelivery;
import lk.dmc.disaster.warnings.entity.Warning;
import lk.dmc.disaster.warnings.integration.AreaReference;
import lk.dmc.disaster.warnings.repository.DeliveryCount;
import lk.dmc.disaster.warnings.repository.DeliveryRepository;
import lk.dmc.disaster.warnings.repository.WarningRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

/** The three read-only services over warnings: the module contract, citizen alerts, deliveries. */
@ExtendWith(MockitoExtension.class)
class WarningReadServicesTest {

  private static final Instant NOW = ServiceFixtures.NOW;
  private static final UUID COLOMBO = UUID.randomUUID();
  private static final UUID KELANI = UUID.randomUUID();
  private static final UUID OWN_BASIN = UUID.randomUUID();

  @Mock private WarningRepository warnings;
  @Mock private DeliveryRepository deliveries;
  @Mock private AreaReference areas;

  // ---- ActiveWarningQueryImpl --------------------------------------------------------------

  @Test
  void findActiveForDistrict_directAndViaBasin_areBothReturnedHighestLevelFirst() {
    Warning direct =
        ServiceFixtures.warning(WarningLevel.WATCH, ServiceFixtures.districtTarget(COLOMBO), NOW);
    Warning viaBasin =
        ServiceFixtures.warning(
            WarningLevel.EVACUATE, ServiceFixtures.basinTarget(KELANI), NOW.minusSeconds(60));
    when(areas.basinsOfDistrict(COLOMBO)).thenReturn(Set.of(KELANI));
    when(warnings.findActiveCovering(Set.of(COLOMBO), Set.of(KELANI)))
        .thenReturn(List.of(direct, viaBasin));
    ActiveWarningQueryImpl query = new ActiveWarningQueryImpl(warnings, areas);

    List<ActiveWarningSummary> result = query.findActiveForDistrict(COLOMBO);

    assertThat(result)
        .extracting(ActiveWarningSummary::warningId)
        .containsExactly(viaBasin.getId(), direct.getId());
    assertThat(result.get(0).riverBasinIds()).containsExactly(KELANI);
    assertThat(result.get(1).districtIds()).containsExactly(COLOMBO);
    assertThat(result.get(1).title()).isEqualTo("Kelani flood");
    assertThat(result.get(1).instructions()).isEqualTo("Leave homes.");
  }

  @Test
  void findActiveForDistrict_sameLevel_newestFirst() {
    Warning older =
        ServiceFixtures.warning(
            WarningLevel.WARNING, ServiceFixtures.districtTarget(COLOMBO), NOW.minusSeconds(600));
    Warning newer =
        ServiceFixtures.warning(WarningLevel.WARNING, ServiceFixtures.districtTarget(COLOMBO), NOW);
    when(areas.basinsOfDistrict(COLOMBO)).thenReturn(Set.of());
    when(warnings.findActiveCovering(Set.of(COLOMBO), Set.of())).thenReturn(List.of(older, newer));

    assertThat(new ActiveWarningQueryImpl(warnings, areas).findActiveForDistrict(COLOMBO))
        .extracting(ActiveWarningSummary::warningId)
        .containsExactly(newer.getId(), older.getId());
  }

  @Test
  void findActiveForDistrict_notTargeted_isEmpty() {
    when(areas.basinsOfDistrict(COLOMBO)).thenReturn(Set.of());
    when(warnings.findActiveCovering(Set.of(COLOMBO), Set.of())).thenReturn(List.of());

    assertThat(new ActiveWarningQueryImpl(warnings, areas).findActiveForDistrict(COLOMBO))
        .isEmpty();
  }

  @Test
  void hasActiveWarning_trueOnlyForTheEventOfAnActiveWarning() {
    UUID event = UUID.randomUUID();
    Warning forEvent =
        ServiceFixtures.warningForEvent(
            WarningLevel.WATCH, ServiceFixtures.districtTarget(COLOMBO), event);
    Warning noEvent =
        ServiceFixtures.warning(WarningLevel.WATCH, ServiceFixtures.districtTarget(COLOMBO), NOW);
    when(areas.basinsOfDistrict(COLOMBO)).thenReturn(Set.of());
    when(warnings.findActiveCovering(Set.of(COLOMBO), Set.of()))
        .thenReturn(List.of(forEvent, noEvent));
    ActiveWarningQueryImpl query = new ActiveWarningQueryImpl(warnings, areas);

    assertThat(query.hasActiveWarning(event, COLOMBO)).isTrue();
    assertThat(query.hasActiveWarning(UUID.randomUUID(), COLOMBO)).isFalse();
  }

  // ---- CitizenAlertService -----------------------------------------------------------------

  @Test
  void alertsFor_usesTheDistrictItsBasinsAndTheCitizensOwnBasin() {
    Warning warning =
        ServiceFixtures.warning(WarningLevel.WARNING, ServiceFixtures.basinTarget(OWN_BASIN), NOW);
    when(areas.basinsOfDistrict(COLOMBO)).thenReturn(Set.of(KELANI));
    when(warnings.findActiveCovering(Set.of(COLOMBO), Set.of(KELANI, OWN_BASIN)))
        .thenReturn(List.of(warning));

    List<CitizenAlert> alerts =
        new CitizenAlertService(warnings, areas).alertsFor(COLOMBO, OWN_BASIN);

    assertThat(alerts).extracting(CitizenAlert::warningId).containsExactly(warning.getId());
    assertThat(alerts.get(0).message()).isEqualTo("Water is rising fast.");
  }

  @Test
  void alertsFor_citizenWithoutABasin_stillUsesTheDistrictBasins() {
    when(areas.basinsOfDistrict(COLOMBO)).thenReturn(Set.of(KELANI));
    when(warnings.findActiveCovering(Set.of(COLOMBO), Set.of(KELANI))).thenReturn(List.of());

    assertThat(new CitizenAlertService(warnings, areas).alertsFor(COLOMBO, null)).isEmpty();
  }

  @Test
  void alertsFor_highestLevelFirst_andAudibleOnlyFromWarning() {
    Warning watch =
        ServiceFixtures.warning(WarningLevel.WATCH, ServiceFixtures.districtTarget(COLOMBO), NOW);
    Warning evacuate =
        ServiceFixtures.warning(
            WarningLevel.EVACUATE, ServiceFixtures.districtTarget(COLOMBO), NOW);
    when(areas.basinsOfDistrict(COLOMBO)).thenReturn(Set.of());
    when(warnings.findActiveCovering(Set.of(COLOMBO), Set.of()))
        .thenReturn(List.of(watch, evacuate));

    List<CitizenAlert> alerts = new CitizenAlertService(warnings, areas).alertsFor(COLOMBO, null);

    assertThat(alerts)
        .extracting(CitizenAlert::level)
        .containsExactly(WarningLevel.EVACUATE, WarningLevel.WATCH);
    assertThat(alerts).extracting(CitizenAlert::audible).containsExactly(true, false);
  }

  // ---- DeliveryQueryService ----------------------------------------------------------------

  @Test
  void summary_totalsOverallAndPerChannel() {
    UUID warningId = UUID.randomUUID();
    when(warnings.existsById(warningId)).thenReturn(true);
    when(deliveries.countByChannelAndStatus(warningId))
        .thenReturn(
            List.of(
                new DeliveryCount(Channel.SMS, DeliveryStatus.DELIVERED, 22),
                new DeliveryCount(Channel.SMS, DeliveryStatus.FAILED, 2),
                new DeliveryCount(Channel.PUSH, DeliveryStatus.DELIVERED, 24),
                new DeliveryCount(Channel.AUDIBLE, DeliveryStatus.DELIVERED, 24)));
    when(deliveries.countTargetedCitizens(warningId)).thenReturn(24L);

    DeliveryOutcome outcome = new DeliveryQueryService(warnings, deliveries).summary(warningId);

    assertThat(outcome.targeted()).isEqualTo(24);
    assertThat(outcome.delivered()).isEqualTo(70);
    assertThat(outcome.failed()).isEqualTo(2);
    assertThat(outcome.byChannel())
        .containsExactly(
            new ChannelOutcome(Channel.PUSH, 24, 0),
            new ChannelOutcome(Channel.SMS, 22, 2),
            new ChannelOutcome(Channel.AUDIBLE, 24, 0));
  }

  @Test
  void summary_noDeliveries_isAllZero() {
    UUID warningId = UUID.randomUUID();
    when(warnings.existsById(warningId)).thenReturn(true);
    when(deliveries.countByChannelAndStatus(warningId)).thenReturn(List.of());
    when(deliveries.countTargetedCitizens(warningId)).thenReturn(0L);

    DeliveryOutcome outcome = new DeliveryQueryService(warnings, deliveries).summary(warningId);

    assertThat(outcome).isEqualTo(new DeliveryOutcome(0, 0, 0, List.of()));
  }

  @Test
  @SuppressWarnings("unchecked")
  void list_existingWarning_returnsTheRepositoryPage() {
    UUID warningId = UUID.randomUUID();
    Pageable page = PageRequest.of(0, 20);
    NotificationDelivery delivery =
        NotificationDelivery.queue(warningId, UUID.randomUUID(), Channel.SMS, NOW);
    Page<NotificationDelivery> found = new PageImpl<>(List.of(delivery), page, 1);
    when(warnings.existsById(warningId)).thenReturn(true);
    when(deliveries.findAll(any(Specification.class), any(Pageable.class))).thenReturn(found);

    Page<NotificationDelivery> result =
        new DeliveryQueryService(warnings, deliveries)
            .list(warningId, DeliveryStatus.QUEUED, Channel.SMS, page);

    assertThat(result.getContent()).containsExactly(delivery);
  }

  @Test
  void summaryAndList_unknownWarning_areNotFound() {
    UUID warningId = UUID.randomUUID();
    when(warnings.existsById(warningId)).thenReturn(false);
    DeliveryQueryService service = new DeliveryQueryService(warnings, deliveries);

    assertThatThrownBy(() -> service.summary(warningId))
        .isInstanceOfSatisfying(
            AppException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.NOT_FOUND));
    assertThatThrownBy(() -> service.list(warningId, null, null, PageRequest.of(0, 20)))
        .isInstanceOf(AppException.class);
  }
}
