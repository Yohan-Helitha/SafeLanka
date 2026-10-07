package lk.dmc.disaster.analytics.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.analytics.domain.ResourceDistribution;
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
class ResourceDistributionQueryTest {
    @Mock JdbcClient jdbcClient;
    @Mock JdbcClient.StatementSpec statementSpec;
    @Mock JdbcClient.MappedQuerySpec<ResourceDistribution.DistributionRecord> mappedQuerySpec;

    @InjectMocks ResourceDistributionQuery query;

    @Test
    void shouldReturnResourceDistributionWhenRecordsExist() {
        ReportContext context = new ReportContext(TestIds.event(1), null, null, null, TestIds.user(1));
        List<ResourceDistribution.DistributionRecord> records = List.of(
            new ResourceDistribution.DistributionRecord(UUID.randomUUID(), 100, 50)
        );

        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);
        when(statementSpec.query(ResourceDistribution.DistributionRecord.class)).thenReturn(mappedQuerySpec);
        when(mappedQuerySpec.list()).thenReturn(records);

        var result = query.execute(context);

        assertThat(result.key()).isEqualTo(SectionKey.RESOURCE_DISTRIBUTION);
        assertThat(result.isUnavailable()).isFalse();
        assertThat(result.data().records()).hasSize(1);
    }
}
