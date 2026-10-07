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
}
