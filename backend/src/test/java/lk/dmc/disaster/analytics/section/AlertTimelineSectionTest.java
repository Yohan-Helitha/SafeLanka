package lk.dmc.disaster.analytics.section;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import lk.dmc.disaster.analytics.domain.ReportContext;
import lk.dmc.disaster.analytics.query.ReportTimingQuery;
import lk.dmc.disaster.analytics.query.WarningTimelineQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AlertTimelineSectionTest {
    @Mock WarningTimelineQuery warningQuery;
    @Mock ReportTimingQuery timingQuery;
    @InjectMocks AlertTimelineSection section;

    @Test
    void generate_noWarnings_unavailable() {
        when(warningQuery.execute(any())).thenReturn(List.of());
        var res = section.generate(null);
        assertThat(res.isUnavailable()).isTrue();
    }
}
