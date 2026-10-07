package lk.dmc.disaster.response.service;

import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.shared.actor.ActingUserContext;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import lk.dmc.disaster.response.dto.request.OccupancyUpdateRequest;
import lk.dmc.disaster.response.dto.response.ShelterDto;
import lk.dmc.disaster.response.dto.response.ShelterSuggestionDto;
import lk.dmc.disaster.response.entity.Shelter;
import lk.dmc.disaster.response.entity.ShelterStatus;
import lk.dmc.disaster.response.repository.ShelterRepository;
import lk.dmc.disaster.response.validation.DistrictSheltersScreenValidator;
import org.springframework.stereotype.Service;

@Service
public class DistrictSheltersServiceImpl implements DistrictSheltersService {

  private final ShelterRepository shelters;
  private final ActingUserContext actingUser;
  private final DistrictSheltersScreenValidator validator;

  DistrictSheltersServiceImpl(
      ShelterRepository shelters,
      ActingUserContext actingUser,
      DistrictSheltersScreenValidator validator) {
    this.shelters = shelters;
    this.actingUser = actingUser;
    this.validator = validator;
  }

  @Override
  public List<ShelterDto> getShelters(UUID districtId, Boolean availableOnly) {
    if (availableOnly != null && availableOnly) {
      return shelters.findByDistrictIdAndStatus(districtId, ShelterStatus.OPEN).stream()
          .map(ShelterDto::from)
          .toList();
    }
    return shelters.findByDistrictId(districtId).stream()
        .map(ShelterDto::from)
        .toList();
  }

  @Override
  public List<ShelterSuggestionDto> getSuggestions(UUID districtId, int people) {
    return shelters.findByDistrictIdAndStatusAndCurrentOccupancyLessThan(districtId, ShelterStatus.OPEN, Integer.MAX_VALUE).stream()
        .filter(s -> s.getAvailableCapacity() >= people)
        .sorted((a, b) -> Integer.compare(b.getAvailableCapacity(), a.getAvailableCapacity()))
        .map(s -> ShelterSuggestionDto.from(s, 0))
        .toList();
  }

  @Override
  public ShelterDto updateOccupancy(UUID shelterId, int occupancy) {
    Shelter shelter = shelters.findById(shelterId)
        .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Shelter not found"));

    var user = actingUser.require();
    validator.validateOccupancyUpdate(shelter, new OccupancyUpdateRequest(occupancy), user);

    shelter.updateOccupancy(occupancy);
    Shelter saved = shelters.save(shelter);
    return ShelterDto.from(saved);
  }
}

