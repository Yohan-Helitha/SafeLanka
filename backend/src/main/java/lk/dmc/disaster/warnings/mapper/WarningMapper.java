package lk.dmc.disaster.warnings.mapper;

import lk.dmc.disaster.warnings.dto.ChannelOutcomeResponse;
import lk.dmc.disaster.warnings.dto.DeliveriesResponse;
import lk.dmc.disaster.warnings.dto.DeliveryResponse;
import lk.dmc.disaster.warnings.dto.DeliverySummaryResponse;
import lk.dmc.disaster.warnings.dto.PageResponse;
import lk.dmc.disaster.warnings.dto.WarningResponse;
import lk.dmc.disaster.warnings.entity.NotificationDelivery;
import lk.dmc.disaster.warnings.entity.Warning;
import lk.dmc.disaster.warnings.service.DeliveryItem;
import lk.dmc.disaster.warnings.service.DeliveryOutcome;
import lk.dmc.disaster.warnings.service.WarningView;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

/** Turns warning and delivery service results into response shapes. Holds no business rules. */
@Component
public class WarningMapper {

  /** Maps one warning with its areas, evidence and delivery totals. */
  public WarningResponse toResponse(WarningView view) {
    Warning warning = view.warning();
    return new WarningResponse(
        warning.getId(),
        warning.getHazardId(),
        warning.getEventId(),
        warning.getLevel(),
        warning.getStatus(),
        warning.getTargetType(),
        view.target().districtIds(),
        view.target().riverBasinIds(),
        view.resolvedDistrictIds(),
        warning.getTitle(),
        warning.getMessage(),
        warning.getSmsText(),
        warning.getInstructions(),
        warning.getIssuedAt(),
        warning.getIssuedBy(),
        warning.getSupersedesId(),
        warning.getCancelledAt(),
        warning.getCancelReason(),
        view.evidenceReportIds(),
        toSummary(view.deliveries()));
  }

  /** Maps a page of warnings. */
  public PageResponse<WarningResponse> toPage(Page<WarningView> page) {
    return PageResponse.of(page, this::toResponse);
  }

  /** Maps the delivery totals and one page of deliveries. */
  public DeliveriesResponse toDeliveries(DeliveryOutcome outcome, Page<DeliveryItem> page) {
    return new DeliveriesResponse(toSummary(outcome), PageResponse.of(page, this::toDelivery));
  }

  /** Maps the delivery totals, overall and per channel. */
  public DeliverySummaryResponse toSummary(DeliveryOutcome outcome) {
    return new DeliverySummaryResponse(
        outcome.targeted(),
        outcome.delivered(),
        outcome.failed(),
        outcome.byChannel().stream()
            .map(c -> new ChannelOutcomeResponse(c.channel(), c.delivered(), c.failed()))
            .toList());
  }

  private DeliveryResponse toDelivery(DeliveryItem item) {
    NotificationDelivery delivery = item.delivery();
    return new DeliveryResponse(
        delivery.getCitizenId(),
        item.districtId(),
        delivery.getChannel(),
        delivery.getStatus(),
        delivery.getAttemptedAt(),
        delivery.getDeliveredAt(),
        delivery.getFailureReason());
  }
}
