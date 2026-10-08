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
        assertThat(new String(bytes)).startsWith("%PDF-");
    }

    @Test
    void shouldGeneratePdfWithAllSections() {
        var exporter = new PdfReportExporter();
        
        var alertTimeline = Map.of(
            "firstWarningAt", Instant.now().toString(),
            "firstVerifiedReportAt", 1778722200.000,
            "reportToWarningMinutes", 45L,
            "entries", List.of(Map.of("level", "WATCH", "status", "ISSUED", "issuedAt", 1778722200L))
        );
        
        var citizensReached = Map.of(
            "uniqueCitizensTargeted", 1000L,
            "uniqueCitizensReached", 800L,
            "deliveryRate", 0.8,
            "byChannel", List.of(Map.of("channel", "SMS", "delivered", 500L, "failed", 10L)),
            "byDistrict", List.of(Map.of("districtName", "Colombo", "targeted", 1000L, "reached", 800L))
        );
        
        var shelterOccupancy = Map.of(
            "peaks", List.of(Map.of("shelterName", "Test Shelter", "capacity", 100L, "peakOccupancy", 90L, "peakAt", 1778722200L)),
            "series", List.of(Map.of("shelterId", "sid1", "shelterName", "S1", "capacity", 100L, "points", List.of(Map.of("recordedAt", 1778722200L, "occupancy", 50L))))
        );
        
        var resourceDistribution = Map.of(
            "byOrganisationType", List.of(Map.of("type", "GOVERNMENT", "distributed", 500L)),
            "byDistrict", List.of(Map.of("districtName", "Colombo", "itemCode", "WATER", "unit", "Liters", "allocated", 1000L, "distributed", 800L))
        );

        var report = new DisasterReport(TestIds.event(1), Map.of(
            "ALERT_TIMELINE", alertTimeline,
            "CITIZENS_REACHED", citizensReached,
            "SHELTER_OCCUPANCY", shelterOccupancy,
            "RESOURCE_DISTRIBUTION", resourceDistribution
        ), Map.of(), List.of(), TestIds.user(1), Instant.now());
        
        byte[] bytes = exporter.export(report);
        assertThat(bytes).isNotEmpty();
    }
}
