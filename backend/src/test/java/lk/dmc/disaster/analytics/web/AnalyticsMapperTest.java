package lk.dmc.disaster.analytics.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lk.dmc.disaster.analytics.domain.DisasterReport;
import lk.dmc.disaster.analytics.domain.EventSummary;
import lk.dmc.disaster.analytics.domain.ReportContext;
import org.junit.jupiter.api.Test;

class AnalyticsMapperTest {
    @Test
    void toContext_success() {
        var req = new GenerateReportRequest(UUID.randomUUID(), java.util.Set.of(UUID.randomUUID()), Instant.now(), Instant.now());
        var mapper = new AnalyticsMapper(null, null);
        var ctx = mapper.toContext(req, UUID.randomUUID());
        assertThat(ctx).isNotNull();
        assertThat(ctx.eventId()).isEqualTo(req.eventId());
        assertThat(ctx.districtIds()).isEqualTo(req.districtIds());
        assertThat(ctx.fromTime()).isEqualTo(req.from());
        assertThat(ctx.toTime()).isEqualTo(req.to());
    }

    @Test
    void toResponse_success() {
        var report = new DisasterReport(UUID.randomUUID(), Map.of(), Map.of(), List.of(), UUID.randomUUID(), Instant.now());
        var mapper = new AnalyticsMapper(null, null);
        var res = mapper.toResponse(report);
        assertThat(res).isNotNull();
        assertThat(res.eventId()).isEqualTo(report.getEventId());
        assertThat(res.filters()).isNotNull();
    }
}
