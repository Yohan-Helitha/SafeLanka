package lk.dmc.disaster.analytics.entity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ShelterOccupancy(
    List<ShelterSeries> series,
    List<ShelterPeak> peaks
) {
    public record ShelterSeries(
        UUID shelterId,
        String shelterName,
        UUID districtId,
        int capacity,
        List<OccupancyPoint> points
    ) {}
    public record OccupancyPoint(Instant recordedAt, int occupancy) {}
    public record ShelterPeak(
        UUID shelterId,
        int peakOccupancy,
        int capacity,
        double peakRatio,
        Instant peakAt
    ) {}
}
