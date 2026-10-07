package lk.dmc.disaster.response.service;

import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.response.dto.response.ShelterDto;
import lk.dmc.disaster.response.dto.response.ShelterSuggestionDto;

public interface DistrictSheltersService {

  List<ShelterDto> getShelters(UUID districtId, Boolean availableOnly);

  List<ShelterSuggestionDto> getSuggestions(UUID districtId, int people);

  ShelterDto updateOccupancy(UUID shelterId, int occupancy);
}
