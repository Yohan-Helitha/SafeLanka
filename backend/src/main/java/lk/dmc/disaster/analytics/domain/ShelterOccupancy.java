package lk.dmc.disaster.analytics.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ShelterOccupancy(List<ShelterLog> logs) {
    public record ShelterLog(
        UUID shelterId,
        int occupancy,
        Instant recordedAt
    ) {}
}
