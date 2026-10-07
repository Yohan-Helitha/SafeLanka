package lk.dmc.disaster.warnings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.shared.domain.WarningLevel;
import lk.dmc.disaster.warnings.entity.Channel;
import lk.dmc.disaster.warnings.entity.DeliveryStatus;
import lk.dmc.disaster.warnings.entity.NotificationDelivery;
import lk.dmc.disaster.warnings.entity.TargetType;
import lk.dmc.disaster.warnings.entity.Warning;
import lk.dmc.disaster.warnings.entity.WarningContent;
import lk.dmc.disaster.warnings.entity.WarningDraft;
import lk.dmc.disaster.warnings.entity.WarningTarget;
import lk.dmc.disaster.warnings.integration.AudibleAlertSimulator;
import lk.dmc.disaster.warnings.integration.DeliveryRequest;
import lk.dmc.disaster.warnings.integration.DeliveryResult;
import lk.dmc.disaster.warnings.integration.GatewayFailureSwitch;
import lk.dmc.disaster.warnings.integration.NotificationChannel;
import lk.dmc.disaster.warnings.integration.PushChannelSimulator;
import lk.dmc.disaster.warnings.integration.SmsGatewaySimulator;
import lk.dmc.disaster.warnings.repository.DeliveryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationDispatchServiceTest {

  private static final Instant NOW = Instant.parse("2026-10-06T10:00:00Z");
  private static final UUID DISTRICT = UUID.randomUUID();

  @Mock private ChannelSettingsService settings;
  @Mock private AudienceService audience;
  @Mock private DeliveryRepository deliveries;

  private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
  private final List<UUID> citizens =
      List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

  private static final GatewayFailureSwitch NO_FAILURES = channel -> false;

  private NotificationDispatchService serviceWith(NotificationChannel... channels) {
    return new NotificationDispatchService(
        List.of(channels), settings, audience, deliveries, clock);
  }

  private NotificationDispatchService serviceWithAllSimulators(GatewayFailureSwitch failures) {
    return serviceWith(
        new AudibleAlertSimulator(failures),
        new SmsGatewaySimulator(failures),
        new PushChannelSimulator(failures));
  }

  private void allChannelsEnabled() {
    when(settings.isEnabled(any(Channel.class))).thenReturn(true);
  }

  private Warning warning(WarningLevel level) {
    WarningContent content =
        new WarningContent(
            "Kelani flood", "Water is rising fast.", "Kelani flood: move now.", "Leave homes.");
    WarningTarget target = new WarningTarget(TargetType.DISTRICT, Set.of(DISTRICT), Set.of());
    return Warning.publish(
        new WarningDraft(UUID.randomUUID(), null, level, target, content, Set.of()),
        UUID.randomUUID(),
        NOW);
  }

  private void audienceIs(List<UUID> people) {
    when(audience.findCitizenIds(any(AudienceSelection.class))).thenReturn(people);
  }

  @SuppressWarnings("unchecked")
  private List<NotificationDelivery> savedDeliveries() {
    ArgumentCaptor<List<NotificationDelivery>> captor = ArgumentCaptor.forClass(List.class);
    verify(deliveries).saveAll(captor.capture());
    return captor.getValue();
  }

  @Test
  void dispatch_evacuateWithThreeCitizens_createsNineDeliveries() {
    audienceIs(citizens);
    allChannelsEnabled();

    DispatchSummary summary =
        serviceWithAllSimulators(NO_FAILURES).dispatch(warning(WarningLevel.EVACUATE));

    assertThat(savedDeliveries()).hasSize(9);
    assertThat(summary).isEqualTo(new DispatchSummary(3, 9, 0));
  }

  @Test
  void dispatch_recordsTimesAndTheRightWarningAndCitizens() {
    audienceIs(citizens);
    allChannelsEnabled();
    Warning warning = warning(WarningLevel.WARNING);

    serviceWithAllSimulators(NO_FAILURES).dispatch(warning);

    assertThat(savedDeliveries())
        .allSatisfy(
            d -> {
              assertThat(d.getWarningId()).isEqualTo(warning.getId());
              assertThat(d.getAttemptedAt()).isEqualTo(NOW);
              assertThat(d.getDeliveredAt()).isEqualTo(NOW);
              assertThat(d.getStatus()).isEqualTo(DeliveryStatus.DELIVERED);
            })
        .extracting(NotificationDelivery::getCitizenId)
        .containsOnly(citizens.toArray(UUID[]::new));
  }

  @Test
  void dispatch_disabledChannel_isSkipped() {
    audienceIs(citizens);
    when(settings.isEnabled(Channel.PUSH)).thenReturn(true);
    when(settings.isEnabled(Channel.SMS)).thenReturn(false);
    when(settings.isEnabled(Channel.AUDIBLE)).thenReturn(true);

    serviceWithAllSimulators(NO_FAILURES).dispatch(warning(WarningLevel.EVACUATE));

    assertThat(savedDeliveries())
        .extracting(NotificationDelivery::getChannel)
        .doesNotContain(Channel.SMS)
        .hasSize(6);
  }

  @Test
  void dispatch_watchLevel_skipsTheAudibleChannel() {
    audienceIs(citizens);
    when(settings.isEnabled(Channel.PUSH)).thenReturn(true);
    when(settings.isEnabled(Channel.SMS)).thenReturn(true);

    serviceWithAllSimulators(NO_FAILURES).dispatch(warning(WarningLevel.WATCH));

    assertThat(savedDeliveries())
        .extracting(NotificationDelivery::getChannel)
        .doesNotContain(Channel.AUDIBLE)
        .hasSize(6);
  }

  @Test
  void dispatch_nobodyInTheAudience_sendsNothing() {
    audienceIs(List.of());
    allChannelsEnabled();
    NotificationChannel neverCalled = new NeverCalledChannel();

    DispatchSummary summary = serviceWith(neverCalled).dispatch(warning(WarningLevel.EVACUATE));

    assertThat(summary).isEqualTo(new DispatchSummary(0, 0, 0));
    assertThat(savedDeliveries()).isEmpty();
  }

  @Test
  void dispatch_smsGatewayFailing_recordsSmsFailedWhilePushAndAudibleStillDeliver() {
    audienceIs(citizens);
    allChannelsEnabled();
    GatewayFailureSwitch smsFails = channel -> channel == Channel.SMS;

    DispatchSummary summary =
        serviceWithAllSimulators(smsFails).dispatch(warning(WarningLevel.EVACUATE));

    List<NotificationDelivery> saved = savedDeliveries();
    assertThat(summary).isEqualTo(new DispatchSummary(3, 6, 3));
    assertThat(saved)
        .filteredOn(d -> d.getChannel() == Channel.SMS)
        .allSatisfy(
            d -> {
              assertThat(d.getStatus()).isEqualTo(DeliveryStatus.FAILED);
              assertThat(d.getFailureReason()).isEqualTo("Simulated gateway timeout");
            });
    assertThat(saved)
        .filteredOn(d -> d.getChannel() != Channel.SMS)
        .allMatch(d -> d.getStatus() == DeliveryStatus.DELIVERED);
  }

  @Test
  void dispatch_channelThrowsInsteadOfFailing_isRecordedFailedAndOthersContinue() {
    audienceIs(citizens);
    allChannelsEnabled();
    NotificationChannel crashing = new CrashingChannel(Channel.SMS);

    DispatchSummary summary =
        serviceWith(new PushChannelSimulator(NO_FAILURES), crashing)
            .dispatch(warning(WarningLevel.WATCH));

    assertThat(summary).isEqualTo(new DispatchSummary(3, 3, 3));
    assertThat(savedDeliveries())
        .filteredOn(d -> d.getStatus() == DeliveryStatus.FAILED)
        .allSatisfy(d -> assertThat(d.getFailureReason()).startsWith("Gateway error"));
  }

  @Test
  void dispatch_deliveriesAreListedInChannelOrder() {
    audienceIs(List.of(citizens.get(0)));
    allChannelsEnabled();

    serviceWithAllSimulators(NO_FAILURES).dispatch(warning(WarningLevel.EVACUATE));

    assertThat(savedDeliveries())
        .extracting(NotificationDelivery::getChannel)
        .containsExactly(Channel.PUSH, Channel.SMS, Channel.AUDIBLE);
  }

  /** A channel that must not be used. */
  private static final class NeverCalledChannel implements NotificationChannel {
    @Override
    public Channel channel() {
      return Channel.PUSH;
    }

    @Override
    public DeliveryResult send(DeliveryRequest request) {
      throw new AssertionError("must not be called");
    }
  }

  /** A broken gateway that throws instead of returning a failure. */
  private static final class CrashingChannel implements NotificationChannel {
    private final Channel channel;

    CrashingChannel(Channel channel) {
      this.channel = channel;
    }

    @Override
    public Channel channel() {
      return channel;
    }

    @Override
    public DeliveryResult send(DeliveryRequest request) {
      throw new IllegalStateException("socket closed");
    }
  }
}
