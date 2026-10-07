package lk.dmc.disaster.response.dto.response;

import java.time.Instant;
import java.util.UUID;
import lk.dmc.disaster.response.entity.Shelter;
import lk.dmc.disaster.response.entity.ShelterStatus;

public record ShelterDto(
    UUID id,
    String name,
    UUID districtId,
    String address,
    double latitude,
    double longitude,
    int capacity,
    int currentOccupancy,
    ShelterStatus status,
    UUID coordinatorId,
    long version,
    Instant createdAt,
    Instant updatedAt) {

  public static ShelterDto from(Shelter shelter) {
    return new ShelterDto(
        shelter.getId(), shelter.getName(), shelter.getDistrictId(), shelter.getAddress(),
        shelter.getLatitude(), shelter.getLongitude(), shelter.getCapacity(),
        shelter.getCurrentOccupancy(), shelter.getStatus(), shelter.getCoordinatorId(),
        shelter.getVersion(), shelter.getCreatedAt(), shelter.getUpdatedAt());
  }
}
