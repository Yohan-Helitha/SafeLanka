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

    @Test
    void generate_withAllocations_success() {
        when(query.getByDistrict(any())).thenReturn(List.of(
            new lk.dmc.disaster.analytics.entity.ResourceDistribution.DistrictDistribution(java.util.UUID.randomUUID(), "D1", "WATER", "L", 100, 80)
        ));
        when(query.getByOrganisationType(any())).thenReturn(List.of(
            new lk.dmc.disaster.analytics.entity.ResourceDistribution.OrganisationTypeDistribution("NGO", 50)
        ));
        
        var res = section.generate(null);
        assertThat(res.isUnavailable()).isFalse();
        var data = (lk.dmc.disaster.analytics.entity.ResourceDistribution) res.data();
        assertThat(data.byDistrict()).hasSize(1);
        assertThat(data.byOrganisationType()).hasSize(1);
    }
}
