package lk.dmc.disaster.analytics.query;

import static org.assertj.core.api.Assertions.assertThat;

import lk.dmc.disaster.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class EventSummaryQueryTest {
    @Autowired EventSummaryQuery query;

    @Test
    void execute_returnsEvents() {
        var res = query.execute(null);
        assertThat(res).hasSize(2);
    }
}
