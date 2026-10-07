package lk.dmc.disaster.analytics.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import lk.dmc.disaster.analytics.domain.CitizensReached;
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
class CitizensReachedQueryTest {
    @Mock JdbcClient jdbcClient;
    @Mock JdbcClient.StatementSpec statementSpec;
    @Mock JdbcClient.MappedQuerySpec<CitizensReached> mappedQuerySpec;

    @InjectMocks CitizensReachedQuery query;

    @Test
    void shouldReturnCitizensReachedStats() {
        ReportContext context = new ReportContext(TestIds.event(1), null, null, null, TestIds.user(1));
        CitizensReached stats = new CitizensReached(100L, 80L, 20L);

        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);
        when(statementSpec.query(CitizensReached.class)).thenReturn(mappedQuerySpec);
        when(mappedQuerySpec.single()).thenReturn(stats);

        var result = query.execute(context);

        assertThat(result.key()).isEqualTo(SectionKey.CITIZENS_REACHED);
        assertThat(result.isUnavailable()).isFalse();
        assertThat(result.data().totalAttempted()).isEqualTo(100L);
    }
}
