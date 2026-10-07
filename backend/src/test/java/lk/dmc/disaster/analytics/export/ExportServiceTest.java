package lk.dmc.disaster.analytics.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import lk.dmc.disaster.analytics.domain.DisasterReport;
import org.junit.jupiter.api.Test;
import lk.dmc.disaster.support.TestIds;

class ExportServiceTest {

    @Test
    void shouldExportSupportedFormat() {
        var exporters = List.<ReportExporter>of(new PdfReportExporter(), new CsvReportExporter());
        var service = new ExportService(exporters);
        var report = new DisasterReport(TestIds.event(1), Map.of(), Map.of(), List.of(), TestIds.user(1), Instant.now());
        
        byte[] pdfBytes = service.exportReport(report, "PDF");
        assertThat(pdfBytes).isNotEmpty();
        
        byte[] csvBytes = service.exportReport(report, "CSV");
        assertThat(csvBytes).isNotEmpty();
    }

    @Test
    void shouldThrowForUnsupportedFormat() {
        var service = new ExportService(List.of(new PdfReportExporter()));
        var report = new DisasterReport(TestIds.event(1), Map.of(), Map.of(), List.of(), TestIds.user(1), Instant.now());
        
        assertThatThrownBy(() -> service.exportReport(report, "EXCEL"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported format");
    }
}
