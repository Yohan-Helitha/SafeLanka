package lk.dmc.disaster.response.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import lk.dmc.disaster.response.dto.request.OccupancyUpdateRequest;
import lk.dmc.disaster.response.entity.Shelter;
import lk.dmc.disaster.shared.actor.ActingUser;
import lk.dmc.disaster.shared.domain.Role;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DistrictSheltersScreenValidatorTest {

  private DistrictSheltersScreenValidator validator;
  private Shelter shelter;
  private UUID coordinatorId;

  @BeforeEach
  void setUp() {
    validator = new DistrictSheltersScreenValidator();
    coordinatorId = UUID.randomUUID();
    shelter =
        Shelter.create(
            "Mahanama College", UUID.randomUUID(), "Kollupitiya", 6.90, 79.85, 100, coordinatorId);
  }

  @Test
  void validateOccupancyUpdate_nullRequestOrOccupancy_throwsValidationError() {
    ActingUser officer = new ActingUser(UUID.randomUUID(), Role.DISTRICT_OFFICER, null, null, null);

    assertThatThrownBy(() -> validator.validateOccupancyUpdate(shelter, null, officer))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.VALIDATION_ERROR);

    assertThatThrownBy(
            () ->
                validator.validateOccupancyUpdate(
                    shelter, new OccupancyUpdateRequest(null), officer))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.VALIDATION_ERROR);
  }

  @Test
  void validateOccupancyUpdate_negativeOccupancy_throwsValidationError() {
    ActingUser officer = new ActingUser(UUID.randomUUID(), Role.DISTRICT_OFFICER, null, null, null);

    assertThatThrownBy(
            () ->
                validator.validateOccupancyUpdate(shelter, new OccupancyUpdateRequest(-5), officer))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.VALIDATION_ERROR);
  }

  @Test
  void validateOccupancyUpdate_exceedsCapacity_throwsCapacityExceeded() {
    ActingUser officer = new ActingUser(UUID.randomUUID(), Role.DISTRICT_OFFICER, null, null, null);

    assertThatThrownBy(
            () ->
                validator.validateOccupancyUpdate(
                    shelter, new OccupancyUpdateRequest(105), officer))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.CAPACITY_EXCEEDED);
  }

  @Test
  void validateOccupancyUpdate_districtOfficer_succeeds() {
    ActingUser officer = new ActingUser(UUID.randomUUID(), Role.DISTRICT_OFFICER, null, null, null);

    assertThatCode(
            () ->
                validator.validateOccupancyUpdate(shelter, new OccupancyUpdateRequest(80), officer))
        .doesNotThrowAnyException();
  }

  @Test
  void validateOccupancyUpdate_matchingShelterCoordinator_succeeds() {
    ActingUser coordinator =
        new ActingUser(coordinatorId, Role.SHELTER_COORDINATOR, null, null, null);

    assertThatCode(
            () ->
                validator.validateOccupancyUpdate(
                    shelter, new OccupancyUpdateRequest(60), coordinator))
        .doesNotThrowAnyException();
  }

  @Test
  void validateOccupancyUpdate_mismatchedShelterCoordinator_throwsForbiddenRole() {
    ActingUser otherCoordinator =
        new ActingUser(UUID.randomUUID(), Role.SHELTER_COORDINATOR, null, null, null);

    assertThatThrownBy(
            () ->
                validator.validateOccupancyUpdate(
                    shelter, new OccupancyUpdateRequest(60), otherCoordinator))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.FORBIDDEN_ROLE);

    // Shelter with null coordinatorId
    Shelter shelterNoCoord =
        Shelter.create("Shelter No Coord", UUID.randomUUID(), "Addr", 6.9, 79.8, 100, null);
    assertThatThrownBy(
            () ->
                validator.validateOccupancyUpdate(
                    shelterNoCoord, new OccupancyUpdateRequest(60), otherCoordinator))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.FORBIDDEN_ROLE);
  }

  @Test
  void validateOccupancyUpdate_unauthorizedRole_throwsForbiddenRole() {
    ActingUser citizen = new ActingUser(UUID.randomUUID(), Role.CITIZEN, null, null, null);

    assertThatThrownBy(
            () ->
                validator.validateOccupancyUpdate(shelter, new OccupancyUpdateRequest(60), citizen))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.FORBIDDEN_ROLE);
  }
}
