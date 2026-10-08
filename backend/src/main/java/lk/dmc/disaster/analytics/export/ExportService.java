package lk.dmc.disaster.analytics.export;

import java.util.List;
import lk.dmc.disaster.analytics.domain.DisasterReport;
import org.springframework.stereotype.Service;

@Service
public class ExportService {
    private final List<ReportExporter> exporters;

    public ExportService(List<ReportExporter> exporters) {
        this.exporters = exporters;
    }

    public byte[] exportReport(DisasterReport report, String format) {
        return exporters.stream()
            .filter(e -> e.supports(format.toUpperCase()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unsupported format: " + format))
            .export(report);
    }
}
