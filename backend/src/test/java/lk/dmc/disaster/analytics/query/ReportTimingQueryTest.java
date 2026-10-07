package lk.dmc.disaster.analytics.query;

import static org.assertj.core.api.Assertions.assertThat;

import lk.dmc.disaster.TestcontainersConfiguration;
import lk.dmc.disaster.analytics.domain.ReportContext;
import lk.dmc.disaster.support.TestIds;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class ReportTimingQueryTest {
    @Autowired ReportTimingQuery query;

    @Test
    void execute_kaluEvent_returnsTiming() {
        ReportContext ctx = new ReportContext(TestIds.event(2), null, null, null, TestIds.user(1));
        var res = query.execute(ctx);
        assertThat(res).isPresent();
    }
}
