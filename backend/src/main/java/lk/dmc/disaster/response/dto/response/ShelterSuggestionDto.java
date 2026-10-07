package lk.dmc.disaster.response.dto.response;

import java.util.UUID;
import lk.dmc.disaster.response.entity.Shelter;

public record ShelterSuggestionDto(
    UUID id,
    String name,
    String address,
    double latitude,
    double longitude,
    int capacity,
    int currentOccupancy,
    int availableCapacity,
    double distanceKm) {

  public static ShelterSuggestionDto from(Shelter shelter, double distanceKm) {
    return new ShelterSuggestionDto(
        shelter.getId(), shelter.getName(), shelter.getAddress(),
        shelter.getLatitude(), shelter.getLongitude(), shelter.getCapacity(),
        shelter.getCurrentOccupancy(), shelter.getAvailableCapacity(), distanceKm);
  }
}
