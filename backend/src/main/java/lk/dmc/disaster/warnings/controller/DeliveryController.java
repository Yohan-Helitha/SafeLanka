package lk.dmc.disaster.warnings.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lk.dmc.disaster.shared.actor.RequiresRole;
import lk.dmc.disaster.shared.api.ApiResponse;
import lk.dmc.disaster.shared.domain.Role;
import lk.dmc.disaster.warnings.dto.DeliveriesResponse;
import lk.dmc.disaster.warnings.entity.Channel;
import lk.dmc.disaster.warnings.entity.DeliveryStatus;
import lk.dmc.disaster.warnings.mapper.WarningMapper;
import lk.dmc.disaster.warnings.service.DeliveryQueryService;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Delivery results of a warning, so an officer can see which channel failed. */
@Tag(name = "Warnings")
@RestController
@RequestMapping("/api/warnings/{id}/deliveries")
@RequiresRole(Role.DMC_OFFICER)
class DeliveryController {

  private final DeliveryQueryService deliveries;
  private final WarningMapper mapper;

  DeliveryController(DeliveryQueryService deliveries, WarningMapper mapper) {
    this.deliveries = deliveries;
    this.mapper = mapper;
  }

  @Operation(summary = "Delivery totals by channel and a filterable list of deliveries")
  @GetMapping
  ApiResponse<DeliveriesResponse> deliveries(
      @PathVariable UUID id,
      @RequestParam(required = false) DeliveryStatus status,
      @RequestParam(required = false) Channel channel,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    Sort oldestFirst = Sort.by("attemptedAt", "id");
    return ApiResponse.of(
        mapper.toDeliveries(
            deliveries.summary(id),
            deliveries.list(id, status, channel, Paging.of(page, size, oldestFirst))));
  }
}
