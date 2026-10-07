package lk.dmc.disaster.response.validation;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import lk.dmc.disaster.response.dto.request.AllocationRequest;
import lk.dmc.disaster.response.dto.request.DistributionRequest;
import lk.dmc.disaster.response.entity.ReliefStock;
import lk.dmc.disaster.response.entity.ResourceAllocation;
import lk.dmc.disaster.response.entity.Shelter;
import lk.dmc.disaster.response.entity.ShelterStatus;
import lk.dmc.disaster.response.repository.ReliefStockRepository;
import lk.dmc.disaster.response.repository.ShelterRepository;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import org.springframework.stereotype.Component;

/**
 * Validation logic corresponding to frontend screen:
 * ReliefSuppliesScreen (frontend/src/screens/response/ReliefSuppliesScreen.tsx)
 * and modal AllocateDialog (frontend/src/components/response/AllocateDialog.tsx).
 *
 * Handles:
 * - Resource allocation validation:
 *   - Stock existence and quantity availability check.
 *   - In case of insufficient stock, finds alternative stock suppliers for the same item in the district.
 *   - Shelter verification (cannot allocate to closed shelter).
 * - Distribution recording validation:
 *   - Distribution quantity check against allocated quantity.
 */
@Component
public class ReliefSuppliesScreenValidator {

  private final ReliefStockRepository stocks;
  private final ShelterRepository shelters;

  public ReliefSuppliesScreenValidator(ReliefStockRepository stocks, ShelterRepository shelters) {
    this.stocks = stocks;
    this.shelters = shelters;
  }

  public ReliefStock validateAllocation(AllocationRequest request, UUID userDistrictId) {
    if (request == null) {
      throw new AppException(ErrorCode.VALIDATION_ERROR, "Allocation request body is required");
    }

    if (request.stockId() == null) {
      throw new AppException(ErrorCode.VALIDATION_ERROR, "Stock ID is required", Map.of("stockId", "Must not be null"));
    }

    if (request.shelterId() == null) {
      throw new AppException(ErrorCode.VALIDATION_ERROR, "Shelter ID is required", Map.of("shelterId", "Must not be null"));
    }

    if (request.eventId() == null) {
      throw new AppException(ErrorCode.VALIDATION_ERROR, "Event ID is required", Map.of("eventId", "Must not be null"));
    }

    if (request.quantity() == null || request.quantity() <= 0) {
      throw new AppException(ErrorCode.VALIDATION_ERROR, "Quantity must be greater than zero", Map.of("quantity", "Must be at least 1"));
    }

    Shelter shelter = shelters.findById(request.shelterId())
        .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Target shelter not found"));

    if (shelter.getStatus() == ShelterStatus.CLOSED) {
      throw new AppException(ErrorCode.CONFLICT, "Cannot allocate supplies to a closed shelter", Map.of("shelterStatus", "CLOSED"));
    }

    ReliefStock stock = stocks.findById(request.stockId())
        .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Relief stock not found"));

    if (stock.getQuantityAvailable() < request.quantity()) {
      int shortfall = request.quantity() - stock.getQuantityAvailable();
      UUID districtToSearch = userDistrictId != null ? userDistrictId : stock.getDistrictId();

      List<ReliefStock> availableStocks = stocks.findByDistrictId(districtToSearch);
      List<Map<String, Object>> alternatives = availableStocks.stream()
          .filter(s -> !s.getId().equals(stock.getId()) && s.getItemId().equals(stock.getItemId()) && s.getQuantityAvailable() > 0)
          .map(s -> Map.<String, Object>of(
              "stockId", s.getId().toString(),
              "organisationName", s.getOrganisationId().toString(),
              "districtName", s.getDistrictId().toString(),
              "quantityAvailable", s.getQuantityAvailable()
          ))
          .toList();

      throw new AppException(
          ErrorCode.INSUFFICIENT_STOCK,
          "Insufficient stock available. Requested " + request.quantity() + " but only " + stock.getQuantityAvailable() + " available.",
          Map.of(
              "shortfall", shortfall,
              "available", stock.getQuantityAvailable(),
              "requested", request.quantity(),
              "alternatives", alternatives
          )
      );
    }

    return stock;
  }

  public void validateDistribution(ResourceAllocation allocation, DistributionRequest request) {
    if (request == null) {
      throw new AppException(ErrorCode.VALIDATION_ERROR, "Distribution request body is required");
    }

    if (request.quantityDistributed() == null || request.quantityDistributed() <= 0) {
      throw new AppException(ErrorCode.VALIDATION_ERROR, "Quantity distributed must be greater than zero", Map.of("quantityDistributed", "Must be at least 1"));
    }

    if (request.quantityDistributed() > allocation.getQuantity()) {
      throw new AppException(
          ErrorCode.CAPACITY_EXCEEDED,
          "Distribution quantity exceeds allocation quantity of " + allocation.getQuantity(),
          Map.of("allocatedQuantity", allocation.getQuantity(), "requestedDistribution", request.quantityDistributed())
      );
    }
  }
}

