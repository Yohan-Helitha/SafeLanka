package lk.dmc.disaster.analytics.section;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import lk.dmc.disaster.analytics.query.DistributionQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResourceDistributionSectionTest {
    @Mock DistributionQuery query;
    @InjectMocks ResourceDistributionSection section;

    @Test
    void generate_noAllocations_unavailable() {
        when(query.getByDistrict(any())).thenReturn(List.of());
        var res = section.generate(null);
        assertThat(res.isUnavailable()).isTrue();
    }
}
