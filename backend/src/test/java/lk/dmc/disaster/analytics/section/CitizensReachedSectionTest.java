package lk.dmc.disaster.analytics.section;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import lk.dmc.disaster.analytics.query.DeliveryStatsQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CitizensReachedSectionTest {
    @Mock DeliveryStatsQuery query;
    @InjectMocks CitizensReachedSection section;

    @Test
    void generate_noDeliveries_unavailable() {
        when(query.getUniqueTargeted(any())).thenReturn(0L);
        var res = section.generate(null);
        assertThat(res.isUnavailable()).isTrue();
    }

    @Test
    void generate_withDeliveries_success() {
        when(query.getUniqueTargeted(any())).thenReturn(100L);
        when(query.getUniqueReached(any())).thenReturn(80L);
        when(query.getChannelStats(any())).thenReturn(java.util.List.of(
            new lk.dmc.disaster.analytics.domain.CitizensReached.ChannelStats("SMS", 50, 5)
        ));
        when(query.getDistrictStats(any())).thenReturn(java.util.List.of(
            new lk.dmc.disaster.analytics.domain.CitizensReached.DistrictStats(java.util.UUID.randomUUID(), "D1", 100, 80)
        ));
        
        var res = section.generate(null);
        assertThat(res.isUnavailable()).isFalse();
        var data = (lk.dmc.disaster.analytics.domain.CitizensReached) res.data();
        assertThat(data.uniqueCitizensTargeted()).isEqualTo(100L);
        assertThat(data.uniqueCitizensReached()).isEqualTo(80L);
        assertThat(data.deliveryRate()).isEqualTo(0.8);
        assertThat(data.byChannel()).hasSize(1);
        assertThat(data.byDistrict()).hasSize(1);
    }
}
