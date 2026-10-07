package lk.dmc.disaster.warnings.service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import lk.dmc.disaster.warnings.entity.Channel;
import lk.dmc.disaster.warnings.entity.DeliveryStatus;
import lk.dmc.disaster.warnings.entity.NotificationDelivery;
import lk.dmc.disaster.warnings.repository.DeliveryCount;
import lk.dmc.disaster.warnings.repository.DeliveryRepository;
import lk.dmc.disaster.warnings.repository.DeliverySpecifications;
import lk.dmc.disaster.warnings.repository.WarningRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Delivery results of a warning: totals by channel and a filterable list. */
@Service
public class DeliveryQueryService {

  private final WarningRepository warnings;
  private final DeliveryRepository deliveries;

  public DeliveryQueryService(WarningRepository warnings, DeliveryRepository deliveries) {
    this.warnings = warnings;
    this.deliveries = deliveries;
  }

  /**
   * Totals for the warning, overall and per channel.
   *
   * @throws AppException NOT_FOUND for an unknown warning
   */
  @Transactional(readOnly = true)
  public DeliveryOutcome summary(UUID warningId) {
    requireWarning(warningId);
    List<DeliveryCount> counts = deliveries.countByChannelAndStatus(warningId);
    Map<Channel, List<DeliveryCount>> byChannel =
        counts.stream()
            .collect(
                Collectors.groupingBy(
                    DeliveryCount::channel,
                    () -> new EnumMap<>(Channel.class),
                    Collectors.toList()));
    List<ChannelOutcome> channels =
        byChannel.entrySet().stream()
            .map(
                e ->
                    new ChannelOutcome(
                        e.getKey(),
                        total(e.getValue(), DeliveryStatus.DELIVERED),
                        total(e.getValue(), DeliveryStatus.FAILED)))
            .toList();
    return new DeliveryOutcome(
        deliveries.countTargetedCitizens(warningId),
        total(counts, DeliveryStatus.DELIVERED),
        total(counts, DeliveryStatus.FAILED),
        channels);
  }

  /**
   * One page of the warning's deliveries.
   *
   * @param status only this status, or null for all
   * @param channel only this channel, or null for all
   * @throws AppException NOT_FOUND for an unknown warning
   */
  @Transactional(readOnly = true)
  public Page<NotificationDelivery> list(
      UUID warningId, DeliveryStatus status, Channel channel, Pageable pageable) {
    requireWarning(warningId);
    return deliveries.findAll(
        DeliverySpecifications.forWarning(warningId)
            .and(DeliverySpecifications.withStatus(status))
            .and(DeliverySpecifications.onChannel(channel)),
        pageable);
  }

  private void requireWarning(UUID warningId) {
    if (!warnings.existsById(warningId)) {
      throw new AppException(ErrorCode.NOT_FOUND, "Warning not found.");
    }
  }

  private static long total(List<DeliveryCount> counts, DeliveryStatus status) {
    return counts.stream().filter(c -> c.status() == status).mapToLong(DeliveryCount::count).sum();
  }
}
