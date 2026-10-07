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
import lk.dmc.disaster.warnings.integration.CitizenDirectory;
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
  private final CitizenDirectory citizens;

  public DeliveryQueryService(
      WarningRepository warnings, DeliveryRepository deliveries, CitizenDirectory citizens) {
    this.warnings = warnings;
    this.deliveries = deliveries;
    this.citizens = citizens;
  }

  /**
   * Totals for the warning, overall and per channel.
   *
   * @throws AppException NOT_FOUND for an unknown warning
   */
  @Transactional(readOnly = true)
  public DeliveryOutcome summary(UUID warningId) {
    requireWarning(warningId);
    return outcomeOf(warningId);
  }

  /** Totals for a warning the caller has already loaded, so no existence check is repeated. */
  @Transactional(readOnly = true)
  DeliveryOutcome outcomeOf(UUID warningId) {
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
   * One page of the warning's deliveries, each with the person's district.
   *
   * @param status only this status, or null for all
   * @param channel only this channel, or null for all
   * @throws AppException NOT_FOUND for an unknown warning
   */
  @Transactional(readOnly = true)
  public Page<DeliveryItem> list(
      UUID warningId, DeliveryStatus status, Channel channel, Pageable pageable) {
    requireWarning(warningId);
    Page<NotificationDelivery> page =
        deliveries.findAll(
            DeliverySpecifications.forWarning(warningId)
                .and(DeliverySpecifications.withStatus(status))
                .and(DeliverySpecifications.onChannel(channel)),
            pageable);
    Map<UUID, UUID> districts =
        citizens.districtsOf(
            page.getContent().stream().map(NotificationDelivery::getCitizenId).distinct().toList());
    return page.map(d -> new DeliveryItem(d, districts.get(d.getCitizenId())));
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
