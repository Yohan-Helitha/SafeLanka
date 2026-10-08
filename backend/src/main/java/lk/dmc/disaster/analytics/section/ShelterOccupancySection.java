package lk.dmc.disaster.analytics.section;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lk.dmc.disaster.analytics.domain.ReportContext;
import lk.dmc.disaster.analytics.domain.SectionKey;
import lk.dmc.disaster.analytics.domain.SectionResult;
import lk.dmc.disaster.analytics.domain.ShelterOccupancy;
import lk.dmc.disaster.analytics.domain.ShelterOccupancy.OccupancyPoint;
import lk.dmc.disaster.analytics.domain.ShelterOccupancy.ShelterPeak;
import lk.dmc.disaster.analytics.domain.ShelterOccupancy.ShelterSeries;
import lk.dmc.disaster.analytics.query.OccupancyQuery;
import org.springframework.stereotype.Component;

@Component
public class ShelterOccupancySection implements ReportSection {
    private final OccupancyQuery query;

    public ShelterOccupancySection(OccupancyQuery query) { this.query = query; }

    @Override
    public SectionKey getKey() { return SectionKey.SHELTER_OCCUPANCY; }

    @Override
    public SectionResult<?> generate(ReportContext context) {
        List<OccupancyQuery.ShelterSeriesRecord> records = query.getSeries(context);
        if (records.isEmpty()) {
            return SectionResult.unavailable(getKey(), "No shelter occupancy was logged for this event.");
        }
        Map<UUID, List<OccupancyQuery.ShelterSeriesRecord>> grouped = records.stream()
            .collect(Collectors.groupingBy(OccupancyQuery.ShelterSeriesRecord::shelterId));
            
        List<ShelterSeries> series = new ArrayList<>();
        for (var entry : grouped.entrySet()) {
            var r = entry.getValue().get(0);
            List<OccupancyPoint> points = entry.getValue().stream()
                .map(x -> new OccupancyPoint(x.recordedAt(), x.occupancy()))
                .toList();
            series.add(new ShelterSeries(r.shelterId(), r.shelterName(), r.districtId(), r.capacity(), points));
        }
        
        List<ShelterPeak> peaks = query.getPeaks(context).stream().map(p -> {
            double ratio = p.capacity() > 0 ? Math.round(((double) p.peakOccupancy() / p.capacity()) * 100.0) / 100.0 : 0.0;
            return new ShelterPeak(p.shelterId(), p.peakOccupancy(), p.capacity(), ratio, p.peakAt());
        }).toList();

        return SectionResult.success(getKey(), new ShelterOccupancy(series, peaks));
    }
}
