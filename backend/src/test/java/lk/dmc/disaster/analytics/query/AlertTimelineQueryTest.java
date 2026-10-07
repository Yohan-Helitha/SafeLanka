package lk.dmc.disaster.analytics.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import lk.dmc.disaster.analytics.domain.AlertTimeline;
import lk.dmc.disaster.analytics.domain.ReportContext;
import lk.dmc.disaster.analytics.domain.SectionKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.simple.JdbcClient;
import lk.dmc.disaster.support.TestIds;

@ExtendWith(MockitoExtension.class)
class AlertTimelineQueryTest {
    @Mock JdbcClient jdbcClient;
    @Mock JdbcClient.StatementSpec statementSpec;
    @Mock JdbcClient.MappedQuerySpec<AlertTimeline.TimelineEvent> mappedQuerySpec;

    @InjectMocks AlertTimelineQuery query;

    @Test
    void shouldReturnAlertTimelineWhenEventsExist() {
        ReportContext context = new ReportContext(TestIds.event(1), null, null, null, TestIds.user(1));
        List<AlertTimeline.TimelineEvent> events = List.of(
            new AlertTimeline.TimelineEvent(Instant.now(), "WARNING", "Title", "ACTIVE")
        );

        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);
        when(statementSpec.query(AlertTimeline.TimelineEvent.class)).thenReturn(mappedQuerySpec);
        when(mappedQuerySpec.list()).thenReturn(events);

        var result = query.execute(context);

        assertThat(result.key()).isEqualTo(SectionKey.ALERT_TIMELINE);
        assertThat(result.isUnavailable()).isFalse();
        assertThat(result.data().events()).hasSize(1);
    }
}
