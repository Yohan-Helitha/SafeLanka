package lk.dmc.disaster.response.validation;

import java.util.Map;
import lk.dmc.disaster.response.dto.request.OccupancyUpdateRequest;
import lk.dmc.disaster.response.entity.Shelter;
import lk.dmc.disaster.shared.actor.ActingUser;
import lk.dmc.disaster.shared.domain.Role;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import org.springframework.stereotype.Component;

/**
 * Validation logic corresponding to frontend screen: DistrictSheltersScreen
 * (frontend/src/screens/response/DistrictSheltersScreen.tsx) and modal HeadcountDialog
 * (frontend/src/components/response/HeadcountDialog.tsx).
 *
 * <p>Handles: - Headcount occupancy update: - Checks that occupancy is non-negative. - Checks that
 * occupancy does not exceed maximum shelter capacity. - Checks role permission: allowed for Shelter
 * Coordinator of the shelter or District Officer.
 */
@Component
public class DistrictSheltersScreenValidator {

  public void validateOccupancyUpdate(
      Shelter shelter, OccupancyUpdateRequest request, ActingUser user) {
    if (request == null || request.occupancy() == null) {
      throw new AppException(
          ErrorCode.VALIDATION_ERROR,
          "Occupancy value is required",
          Map.of("occupancy", "Must not be null"));
    }

    if (request.occupancy() < 0) {
      throw new AppException(
          ErrorCode.VALIDATION_ERROR,
          "Occupancy cannot be negative",
          Map.of("occupancy", "Must be 0 or more"));
    }

    if (request.occupancy() > shelter.getCapacity()) {
      throw new AppException(
          ErrorCode.CAPACITY_EXCEEDED,
          "Headcount cannot exceed shelter capacity of " + shelter.getCapacity(),
          Map.of(
              "capacity", shelter.getCapacity(),
              "requested", request.occupancy(),
              "excess", request.occupancy() - shelter.getCapacity()));
    }

    // Role check: either DISTRICT_OFFICER or assigned SHELTER_COORDINATOR
    if (user.role() == Role.DISTRICT_OFFICER) {
      return;
    }

    if (user.role() == Role.SHELTER_COORDINATOR) {
      if (shelter.getCoordinatorId() != null && shelter.getCoordinatorId().equals(user.id())) {
        return;
      }
      throw new AppException(
          ErrorCode.FORBIDDEN_ROLE, "Not authorized to update headcount for this shelter");
    }

    throw new AppException(
        ErrorCode.FORBIDDEN_ROLE,
        "Only District Officers and Shelter Coordinators can update headcount");
  }
}
