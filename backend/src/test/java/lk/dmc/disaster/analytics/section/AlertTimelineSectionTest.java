package lk.dmc.disaster.analytics.section;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import lk.dmc.disaster.analytics.domain.AlertTimeline;
import lk.dmc.disaster.analytics.domain.ReportContext;
import lk.dmc.disaster.analytics.domain.SectionKey;
import lk.dmc.disaster.analytics.domain.SectionResult;
import lk.dmc.disaster.analytics.query.AlertTimelineQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import lk.dmc.disaster.support.TestIds;

@ExtendWith(MockitoExtension.class)
class AlertTimelineSectionTest {
    @Mock AlertTimelineQuery query;
    @InjectMocks AlertTimelineSection section;

    @Test
    void shouldReturnSectionResultOnSuccess() {
        var context = new ReportContext(TestIds.event(1), null, null, null, TestIds.user(1));
        var data = new AlertTimeline(List.of(new AlertTimeline.TimelineEvent(Instant.now(), "WARN", "Title", "ACTIVE")));
        when(query.execute(context)).thenReturn(SectionResult.success(SectionKey.ALERT_TIMELINE, data));

        var result = section.generate(context);
        assertThat(result.key()).isEqualTo(SectionKey.ALERT_TIMELINE);
        assertThat(result.isUnavailable()).isFalse();
    }

    @Test
    void shouldReturnUnavailableOnException() {
        var context = new ReportContext(TestIds.event(1), null, null, null, TestIds.user(1));
        when(query.execute(context)).thenThrow(new RuntimeException("DB down"));

        var result = section.generate(context);
        assertThat(result.key()).isEqualTo(SectionKey.ALERT_TIMELINE);
        assertThat(result.isUnavailable()).isTrue();
        assertThat(result.unavailableReason()).contains("Internal error");
    }
}
