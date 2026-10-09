package lk.dmc.disaster.analytics.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lk.dmc.disaster.analytics.entity.DisasterReport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import lk.dmc.disaster.support.TestIds;
import lk.dmc.disaster.TestcontainersConfiguration;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class DisasterReportRepositoryTest {

    @Autowired private DisasterReportRepository repository;

    @Test
    void shouldPersistAndRetrieveJsonbColumns() {
        var report = new DisasterReport(
            TestIds.event(1),
            Map.of("districtIds", List.of(TestIds.district(1).toString())),
            Map.of("ALERT_TIMELINE", Map.of("count", 5)),
            List.of(Map.of("key", "SHELTER_OCCUPANCY", "reason", "No shelters active")),
            TestIds.user(1),
            Instant.now()
        );

        var saved = repository.saveAndFlush(report);
        
        var retrieved = repository.findById(saved.getId()).orElseThrow();
        
        assertThat(retrieved.getFilters()).containsEntry("districtIds", List.of(TestIds.district(1).toString()));
        assertThat(retrieved.getSections()).containsEntry("ALERT_TIMELINE", Map.of("count", 5));
        assertThat(retrieved.getUnavailableSections()).hasSize(1);
        assertThat(retrieved.getUnavailableSections().get(0)).containsEntry("reason", "No shelters active");
    }
}
