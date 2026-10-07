package lk.dmc.disaster.warnings.dto;

/** The delivery results page: totals plus a page of individual deliveries. */
public record DeliveriesResponse(
    DeliverySummaryResponse summary, PageResponse<DeliveryResponse> items) {}
