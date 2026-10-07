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
}
