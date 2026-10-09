package lk.dmc.disaster.response.service;

import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.response.dto.request.CreateHeadcountUpdateRequest;
import lk.dmc.disaster.response.dto.response.ShelterDto;
import lk.dmc.disaster.response.dto.response.ShelterHeadcountUpdateDto;
import lk.dmc.disaster.response.dto.response.ShelterSuggestionDto;
import lk.dmc.disaster.response.entity.HeadcountUpdateStatus;

public interface DistrictSheltersService {

  List<ShelterDto> getShelters(UUID districtId, Boolean availableOnly);

  List<ShelterSuggestionDto> getSuggestions(UUID districtId, int people);

  ShelterDto updateOccupancy(UUID shelterId, int occupancy);

  List<ShelterHeadcountUpdateDto> getHeadcountUpdates(
      UUID districtId, HeadcountUpdateStatus status);

  ShelterDto applyHeadcountUpdate(UUID updateId, Integer customOccupancy);

  ShelterHeadcountUpdateDto dismissHeadcountUpdate(UUID updateId);

  ShelterHeadcountUpdateDto createHeadcountUpdate(CreateHeadcountUpdateRequest request);
}
