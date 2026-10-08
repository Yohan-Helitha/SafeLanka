package lk.dmc.disaster.analytics.section;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import lk.dmc.disaster.analytics.query.OccupancyQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShelterOccupancySectionTest {
    @Mock OccupancyQuery query;
    @InjectMocks ShelterOccupancySection section;

    @Test
    void generate_noLogs_unavailable() {
        when(query.getSeries(any())).thenReturn(List.of());
        var res = section.generate(null);
        assertThat(res.isUnavailable()).isTrue();
    }

    @Test
    void generate_withOccupancy_success() {
        when(query.getSeries(any())).thenReturn(java.util.List.of(
            new lk.dmc.disaster.analytics.query.OccupancyQuery.ShelterSeriesRecord(java.util.UUID.randomUUID(), "S1", java.util.UUID.randomUUID(), 100, java.time.Instant.now(), 50)
        ));
        when(query.getPeaks(any())).thenReturn(java.util.List.of(
            new lk.dmc.disaster.analytics.query.OccupancyQuery.ShelterPeakRecord(java.util.UUID.randomUUID(), 90, 100, java.time.Instant.now())
        ));
        
        var res = section.generate(null);
        assertThat(res.isUnavailable()).isFalse();
        var data = (lk.dmc.disaster.analytics.domain.ShelterOccupancy) res.data();
        assertThat(data.series()).hasSize(1);
        assertThat(data.peaks()).hasSize(1);
    }
}
