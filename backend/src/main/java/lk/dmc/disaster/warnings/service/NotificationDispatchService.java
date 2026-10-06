package lk.dmc.disaster.warnings.service;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.warnings.entity.DeliveryStatus;
import lk.dmc.disaster.warnings.entity.NotificationDelivery;
import lk.dmc.disaster.warnings.entity.Warning;
import lk.dmc.disaster.warnings.integration.DeliveryRequest;
import lk.dmc.disaster.warnings.integration.DeliveryResult;
import lk.dmc.disaster.warnings.integration.NotificationChannel;
import lk.dmc.disaster.warnings.repository.DeliveryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sends a warning to everyone in its audience over every enabled channel, one delivery record per
 * person per channel. It knows channels only through {@link NotificationChannel}, so a new channel
 * needs no change here, and a failing channel never stops the others.
 */
@Slf4j
@Service
public class NotificationDispatchService {

  private final List<NotificationChannel> channels;
  private final ChannelSettingsService channelSettings;
  private final AudienceService audience;
  private final DeliveryRepository deliveries;
  private final Clock clock;

  public NotificationDispatchService(
      List<NotificationChannel> channels,
      ChannelSettingsService channelSettings,
      AudienceService audience,
      DeliveryRepository deliveries,
      Clock clock) {
    this.channels =
        channels.stream().sorted(Comparator.comparing(NotificationChannel::channel)).toList();
    this.channelSettings = channelSettings;
    this.audience = audience;
    this.deliveries = deliveries;
    this.clock = clock;
  }

  /** Sends the warning and records every attempt. */
  @Transactional
  public DispatchSummary dispatch(Warning warning) {
    List<UUID> citizenIds = audience.findCitizenIds(AudienceSelection.of(warning.target()));
    List<NotificationDelivery> records = new ArrayList<>();
    for (NotificationChannel channel : activeChannels(warning)) {
      for (UUID citizenId : citizenIds) {
        records.add(sendOne(channel, warning, citizenId));
      }
    }
    deliveries.saveAll(records);

    DispatchSummary summary =
        new DispatchSummary(
            citizenIds.size(),
            count(records, DeliveryStatus.DELIVERED),
            count(records, DeliveryStatus.FAILED));
    log.info(
        "Warning {} sent to {} people: {} delivered, {} failed",
        warning.getId(),
        summary.citizens(),
        summary.delivered(),
        summary.failed());
    return summary;
  }

  private List<NotificationChannel> activeChannels(Warning warning) {
    return channels.stream()
        .filter(channel -> channel.supports(warning.getLevel()))
        .filter(channel -> channelSettings.isEnabled(channel.channel()))
        .toList();
  }

  private NotificationDelivery sendOne(
      NotificationChannel channel, Warning warning, UUID citizenId) {
    NotificationDelivery delivery =
        NotificationDelivery.queue(warning.getId(), citizenId, channel.channel(), clock.instant());
    DeliveryResult result = sendSafely(channel, DeliveryRequest.of(warning, citizenId));
    if (result.success()) {
      delivery.delivered(clock.instant());
    } else {
      delivery.failed(result.failureReason());
    }
    return delivery;
  }

  /** A channel that throws instead of returning a failure is treated as a failed delivery. */
  private DeliveryResult sendSafely(NotificationChannel channel, DeliveryRequest request) {
    try {
      return channel.send(request);
    } catch (RuntimeException e) {
      log.warn("{} threw while sending warning {}", channel.channel(), request.warningId(), e);
      return DeliveryResult.failed("Gateway error: " + e.getClass().getSimpleName());
    }
  }

  private static int count(List<NotificationDelivery> records, DeliveryStatus status) {
    return (int) records.stream().filter(d -> d.getStatus() == status).count();
  }
}
