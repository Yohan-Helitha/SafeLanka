package lk.dmc.disaster.response.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lk.dmc.disaster.response.dto.request.AllocationRequest;
import lk.dmc.disaster.response.dto.request.DistributionRequest;
import lk.dmc.disaster.response.entity.ReliefStock;
import lk.dmc.disaster.response.entity.ResourceAllocation;
import lk.dmc.disaster.response.entity.Shelter;
import lk.dmc.disaster.response.repository.ReliefStockRepository;
import lk.dmc.disaster.response.repository.ShelterRepository;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReliefSuppliesScreenValidatorTest {

  @Mock private ReliefStockRepository stocks;
  @Mock private ShelterRepository shelters;

  private ReliefSuppliesScreenValidator validator;

  @BeforeEach
  void setUp() {
    validator = new ReliefSuppliesScreenValidator(stocks, shelters);
  }

  @Test
  void validateAllocation_nullOrMissingFields_throwsValidationError() {
    assertThatThrownBy(() -> validator.validateAllocation(null, null))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.VALIDATION_ERROR);

    AllocationRequest missingStock =
        new AllocationRequest(null, UUID.randomUUID(), UUID.randomUUID(), 10);
    assertThatThrownBy(() -> validator.validateAllocation(missingStock, null))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.VALIDATION_ERROR);

    AllocationRequest missingShelter =
        new AllocationRequest(UUID.randomUUID(), null, UUID.randomUUID(), 10);
    assertThatThrownBy(() -> validator.validateAllocation(missingShelter, null))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.VALIDATION_ERROR);

    AllocationRequest missingEvent =
        new AllocationRequest(UUID.randomUUID(), UUID.randomUUID(), null, 10);
    assertThatThrownBy(() -> validator.validateAllocation(missingEvent, null))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.VALIDATION_ERROR);

    AllocationRequest zeroQty =
        new AllocationRequest(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 0);
    assertThatThrownBy(() -> validator.validateAllocation(zeroQty, null))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.VALIDATION_ERROR);

    AllocationRequest negQty =
        new AllocationRequest(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), -5);
    assertThatThrownBy(() -> validator.validateAllocation(negQty, null))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.VALIDATION_ERROR);
  }

  @Test
  void validateAllocation_shelterNotFound_throwsNotFound() {
    UUID shelterId = UUID.randomUUID();
    AllocationRequest req =
        new AllocationRequest(UUID.randomUUID(), shelterId, UUID.randomUUID(), 10);
    when(shelters.findById(shelterId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> validator.validateAllocation(req, null))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.NOT_FOUND);
  }

  @Test
  void validateAllocation_closedShelter_throwsConflict() {
    UUID shelterId = UUID.randomUUID();
    Shelter shelter =
        Shelter.create("Closed Shelter", UUID.randomUUID(), "Address", 6.9, 79.8, 100, null);
    shelter.close();
    when(shelters.findById(shelterId)).thenReturn(Optional.of(shelter));

    AllocationRequest req =
        new AllocationRequest(UUID.randomUUID(), shelterId, UUID.randomUUID(), 10);

    assertThatThrownBy(() -> validator.validateAllocation(req, null))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.CONFLICT);
  }

  @Test
  void validateAllocation_stockNotFound_throwsNotFound() {
    UUID shelterId = UUID.randomUUID();
    UUID stockId = UUID.randomUUID();
    Shelter shelter =
        Shelter.create("Open Shelter", UUID.randomUUID(), "Address", 6.9, 79.8, 100, null);
    when(shelters.findById(shelterId)).thenReturn(Optional.of(shelter));
    when(stocks.findById(stockId)).thenReturn(Optional.empty());

    AllocationRequest req = new AllocationRequest(stockId, shelterId, UUID.randomUUID(), 10);

    assertThatThrownBy(() -> validator.validateAllocation(req, null))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.NOT_FOUND);
  }

  @Test
  void validateAllocation_insufficientStock_throwsInsufficientStockWithAlternatives() {
    UUID shelterId = UUID.randomUUID();
    UUID stockId = UUID.randomUUID();
    UUID districtId = UUID.randomUUID();
    UUID itemId = UUID.randomUUID();

    Shelter shelter = Shelter.create("Open Shelter", districtId, "Address", 6.9, 79.8, 100, null);
    ReliefStock stock = ReliefStock.create(itemId, UUID.randomUUID(), districtId, 20);

    when(shelters.findById(shelterId)).thenReturn(Optional.of(shelter));
    when(stocks.findById(stockId)).thenReturn(Optional.of(stock));

    ReliefStock altStock = ReliefStock.create(itemId, UUID.randomUUID(), districtId, 50);
    when(stocks.findByDistrictId(districtId)).thenReturn(List.of(stock, altStock));

    AllocationRequest req = new AllocationRequest(stockId, shelterId, UUID.randomUUID(), 30);

    assertThatThrownBy(() -> validator.validateAllocation(req, districtId))
        .isInstanceOf(AppException.class)
        .satisfies(
            e -> {
              AppException ae = (AppException) e;
              assertThat(ae.code()).isEqualTo(ErrorCode.INSUFFICIENT_STOCK);
              assertThat(ae.details()).containsKey("alternatives");
            });

    // Also test fallback to stock district when userDistrictId is null
    assertThatThrownBy(() -> validator.validateAllocation(req, null))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.INSUFFICIENT_STOCK);
  }

  @Test
  void validateAllocation_validRequest_returnsStock() {
    UUID shelterId = UUID.randomUUID();
    UUID stockId = UUID.randomUUID();
    UUID districtId = UUID.randomUUID();

    Shelter shelter = Shelter.create("Open Shelter", districtId, "Address", 6.9, 79.8, 100, null);
    ReliefStock stock = ReliefStock.create(UUID.randomUUID(), UUID.randomUUID(), districtId, 100);

    when(shelters.findById(shelterId)).thenReturn(Optional.of(shelter));
    when(stocks.findById(stockId)).thenReturn(Optional.of(stock));

    AllocationRequest req = new AllocationRequest(stockId, shelterId, UUID.randomUUID(), 50);

    ReliefStock result = validator.validateAllocation(req, districtId);
    assertThat(result).isSameAs(stock);
  }

  @Test
  void validateDistribution_nullOrInvalidQuantity_throwsValidationError() {
    ResourceAllocation allocation =
        ResourceAllocation.create(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 50, UUID.randomUUID());

    assertThatThrownBy(() -> validator.validateDistribution(allocation, null))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.VALIDATION_ERROR);

    assertThatThrownBy(
            () ->
                validator.validateDistribution(
                    allocation, new DistributionRequest(null, null, null, false)))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.VALIDATION_ERROR);

    assertThatThrownBy(
            () ->
                validator.validateDistribution(
                    allocation, new DistributionRequest(0, null, null, false)))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.VALIDATION_ERROR);

    assertThatThrownBy(
            () ->
                validator.validateDistribution(
                    allocation, new DistributionRequest(-1, null, null, false)))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.VALIDATION_ERROR);
  }

  @Test
  void validateDistribution_exceedsAllocationQuantity_throwsCapacityExceeded() {
    ResourceAllocation allocation =
        ResourceAllocation.create(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 50, UUID.randomUUID());

    assertThatThrownBy(
            () ->
                validator.validateDistribution(
                    allocation, new DistributionRequest(51, null, null, false)))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.CAPACITY_EXCEEDED);
  }

  @Test
  void validateDistribution_valid_succeeds() {
    ResourceAllocation allocation =
        ResourceAllocation.create(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 50, UUID.randomUUID());

    assertThatCode(
            () ->
                validator.validateDistribution(
                    allocation, new DistributionRequest(50, null, null, false)))
        .doesNotThrowAnyException();
  }
}
