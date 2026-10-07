package lk.dmc.disaster.response.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response model for the District Officer Dashboard.
 * Matches frontend types: ResponseDashboard & ActivityEntry (frontend/src/types/response.ts).
 */
public record DistrictDashboard(
    UUID districtId,
    UUID activeEventId,
    Map<String, Long> teamsByStatus,
    long openAssignments,
    long pendingAcknowledgement,
    long sheltersOccupied,
    long shelterCapacity,
    long nearlyFullShelters,
    long stockLines,
    long activeWarnings,
    List<ActivityEntry> activity) {

  public record ActivityEntry(
      UUID id,
      String type,
      String message,
      Instant occurredAt) {}
}
