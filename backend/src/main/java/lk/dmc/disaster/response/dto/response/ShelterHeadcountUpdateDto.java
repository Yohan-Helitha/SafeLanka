package lk.dmc.disaster.response.dto.response;

import java.time.Instant;
import java.util.UUID;
import lk.dmc.disaster.response.entity.HeadcountUpdateStatus;
import lk.dmc.disaster.response.entity.Shelter;
import lk.dmc.disaster.response.entity.ShelterHeadcountUpdate;

public record ShelterHeadcountUpdateDto(
    UUID id,
    UUID shelterId,
    String shelterName,
    UUID districtId,
    int reportedOccupancy,
    Integer previousOccupancy,
    int currentShelterOccupancy,
    int shelterCapacity,
    String reportedByName,
    String reportedByRole,
    String message,
    HeadcountUpdateStatus status,
    Instant reportedAt,
    Instant processedAt) {

  public static ShelterHeadcountUpdateDto from(ShelterHeadcountUpdate update, Shelter shelter) {
    return new ShelterHeadcountUpdateDto(
        update.getId(),
        update.getShelterId(),
        shelter != null ? shelter.getName() : "Unknown Shelter",
        update.getDistrictId(),
        update.getReportedOccupancy(),
        update.getPreviousOccupancy(),
        shelter != null ? shelter.getCurrentOccupancy() : 0,
        shelter != null ? shelter.getCapacity() : 0,
        update.getReportedByName(),
        update.getReportedByRole(),
        update.getMessage(),
        update.getStatus(),
        update.getReportedAt(),
        update.getProcessedAt());
  }
}

