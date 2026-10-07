package lk.dmc.disaster.analytics.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import lk.dmc.disaster.analytics.domain.DisasterReport;
import org.junit.jupiter.api.Test;
import lk.dmc.disaster.support.TestIds;

class PdfReportExporterTest {
    @Test
    void shouldGeneratePdf() {
        var exporter = new PdfReportExporter();
        var report = new DisasterReport(TestIds.event(1), Map.of(), Map.of("TEST", "DATA"), List.of(), TestIds.user(1), Instant.now());
        
        byte[] bytes = exporter.export(report);
        assertThat(bytes).isNotEmpty();
        // A valid PDF starts with %PDF-
        assertThat(new String(bytes)).startsWith("%PDF-");
    }
}
