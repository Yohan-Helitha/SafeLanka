package lk.dmc.disaster.analytics.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.analytics.domain.ShelterOccupancy;
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
class ShelterOccupancyQueryTest {
    @Mock JdbcClient jdbcClient;
    @Mock JdbcClient.StatementSpec statementSpec;
    @Mock JdbcClient.MappedQuerySpec<ShelterOccupancy.ShelterLog> mappedQuerySpec;

    @InjectMocks ShelterOccupancyQuery query;

    @Test
    void shouldReturnShelterOccupancyWhenLogsExist() {
        ReportContext context = new ReportContext(TestIds.event(1), null, null, null, TestIds.user(1));
        List<ShelterOccupancy.ShelterLog> logs = List.of(
            new ShelterOccupancy.ShelterLog(UUID.randomUUID(), 50, Instant.now())
        );

        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);
        when(statementSpec.query(ShelterOccupancy.ShelterLog.class)).thenReturn(mappedQuerySpec);
        when(mappedQuerySpec.list()).thenReturn(logs);

        var result = query.execute(context);

        assertThat(result.key()).isEqualTo(SectionKey.SHELTER_OCCUPANCY);
        assertThat(result.isUnavailable()).isFalse();
        assertThat(result.data().logs()).hasSize(1);
    }
}
