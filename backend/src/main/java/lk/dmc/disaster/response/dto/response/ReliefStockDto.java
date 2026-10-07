package lk.dmc.disaster.response.dto.response;

import java.time.Instant;
import java.util.UUID;
import lk.dmc.disaster.response.entity.ReliefStock;

public record ReliefStockDto(
    UUID id,
    UUID itemId,
    String itemName,
    String unit,
    UUID organisationId,
    String organisationName,
    UUID districtId,
    int quantityAvailable,
    long version,
    Instant createdAt,
    Instant updatedAt) {

  public static ReliefStockDto from(ReliefStock stock) {
    return new ReliefStockDto(
        stock.getId(),
        stock.getItemId(),
        null,
        null,
        stock.getOrganisationId(),
        null,
        stock.getDistrictId(),
        stock.getQuantityAvailable(),
        stock.getVersion(),
        stock.getCreatedAt(),
        stock.getUpdatedAt());
  }

  public static ReliefStockDto from(
      ReliefStock stock, String itemName, String unit, String organisationName) {
    return new ReliefStockDto(
        stock.getId(),
        stock.getItemId(),
        itemName,
        unit,
        stock.getOrganisationId(),
        organisationName,
        stock.getDistrictId(),
        stock.getQuantityAvailable(),
        stock.getVersion(),
        stock.getCreatedAt(),
        stock.getUpdatedAt());
  }
}
