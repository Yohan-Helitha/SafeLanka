package lk.dmc.disaster.analytics.export;

import static org.assertj.core.api.Assertions.assertThat;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PdfReportExporterTest {

  private final PdfReportExporter exporter = new PdfReportExporter();

  /** All the text of the PDF, as a reader would see it. */
  private String textOf(DisasterReportView view) throws IOException {
    byte[] bytes = exporter.export(view);
    PdfReader reader = new PdfReader(bytes);
    PdfTextExtractor extractor = new PdfTextExtractor(reader);
    StringBuilder text = new StringBuilder();
    for (int page = 1; page <= reader.getNumberOfPages(); page++) {
      text.append(extractor.getTextFromPage(page)).append('\n');
    }
    return text.toString();
  }

  @Test
  void format_isPdf() {
    assertThat(exporter.format()).isEqualTo(ExportFormat.PDF);
  }

  @Test
  void export_producesARealPdf() {
    byte[] bytes = exporter.export(ReportViews.of(Map.of(), List.of()));

    assertThat(new String(bytes, 0, 5)).isEqualTo("%PDF-");
  }

  @Test
  void export_headerNamesTheEventAndWhoGeneratedIt() throws IOException {
    String text = textOf(ReportViews.of(Map.of(), List.of()));

    assertThat(text).contains("Kalu Flood May 2026", "Nimal Perera");
  }

  @Test
  void export_printsEveryAvailableSection() throws IOException {
    String text = textOf(ReportViews.of(ReportViews.allSections(), List.of()));

    assertThat(text)
        .contains("Alert Timeline", "Citizens Reached", "Shelter Occupancy", "Resource Distribution")
        .contains("WATCH", "SMS", "Ratnapura", "DRY_RATION");
    assertThat(text).doesNotContain("Data unavailable");
  }

  @Test
  void export_printsATimeTheWayAPersonReadsIt() throws IOException {
    String text = textOf(ReportViews.of(ReportViews.allSections(), List.of()));

    assertThat(text).contains("14 May 2026, 01:30 UTC");
  }

  @Test
  void export_anUnavailableSectionIsPrintedAsDataUnavailableWithItsReason() throws IOException {
    String text =
        textOf(
            ReportViews.of(
                Map.of("ALERT_TIMELINE", ReportViews.alertTimeline()),
                List.of(
                    Map.of(
                        "key", "SHELTER_OCCUPANCY",
                        "reason", "No shelter occupancy was logged for this event."))));

    assertThat(text)
        .contains("Alert Timeline", "Shelter Occupancy", "Data unavailable")
        .contains("No shelter occupancy was logged for this event.");
  }

  @Test
  void export_anUnknownSectionKeyIsStillShown() throws IOException {
    String text =
        textOf(ReportViews.of(Map.of(), List.of(Map.of("key", "SOMETHING_NEW", "reason", "Not built yet"))));

    assertThat(text).contains("SOMETHING_NEW", "Data unavailable", "Not built yet");
  }

  @Test
  void export_sectionsWithMissingEmptyOrOddlyTypedValuesStillRender() throws IOException {
    Map<String, Object> timeline = new java.util.HashMap<>();
    timeline.put("firstWarningAt", "1778720000"); // epoch seconds as text
    timeline.put("firstVerifiedReportAt", 1778720000123L); // epoch millis
    timeline.put("reportToWarningMinutes", null);
    timeline.put("entries", List.of("not a map", Map.of("level", "WATCH")));

    Map<String, Object> citizens = new java.util.HashMap<>();
    citizens.put("uniqueCitizensTargeted", "12"); // number as text
    citizens.put("uniqueCitizensReached", "oops"); // unparsable
    citizens.put("deliveryRate", "0.5");
    citizens.put("byChannel", List.of("skip me", Map.of("channel", "SMS")));
    citizens.put("byDistrict", List.of("skip me", Map.of("districtName", "Kalutara")));

    Map<String, Object> shelters = new java.util.HashMap<>();
    shelters.put(
        "peaks",
        List.of("skip me", Map.of("shelterId", "s1", "peakOccupancy", 10, "capacity", 0, "peakAt", "not-a-time")));
    shelters.put(
        "series",
        List.of(
            "skip me",
            Map.of("shelterName", "A", "capacity", 5, "points", List.of("skip me", Map.of("occupancy", 1))),
            Map.of("shelterName", "B", "capacity", 5)));

    Map<String, Object> distribution = new java.util.HashMap<>();
    distribution.put("byDistrict", List.of("skip me", Map.of("districtName", "Ratnapura")));
    distribution.put("byOrganisationType", List.of("skip me", Map.of("type", "GOVERNMENT")));
    distribution.put("byItem", List.of("skip me", Map.of("itemCode", "WATER")));

    Map<String, Object> sections = new java.util.LinkedHashMap<>();
    sections.put("ALERT_TIMELINE", timeline);
    sections.put("CITIZENS_REACHED", citizens);
    sections.put("SHELTER_OCCUPANCY", shelters);
    sections.put("RESOURCE_DISTRIBUTION", distribution);

    String text = textOf(ReportViews.of(sections, List.of(Map.of("key", "ALERT_TIMELINE"))));

    assertThat(text)
        .contains("Unique Citizens Targeted", "50.0%", "not-a-time", "WATER", "GOVERNMENT")
        .contains("Data unavailable", "No reason given.");
  }

  @Test
  void export_emptyListsInEverySectionPrintOnlyTheHeadings() throws IOException {
    Map<String, Object> sections = new java.util.LinkedHashMap<>();
    sections.put("ALERT_TIMELINE", Map.of("entries", List.of()));
    sections.put("CITIZENS_REACHED", Map.of("byChannel", List.of(), "byDistrict", List.of()));
    sections.put("SHELTER_OCCUPANCY", Map.of("peaks", List.of(), "series", List.of()));
    sections.put(
        "RESOURCE_DISTRIBUTION",
        Map.of("byDistrict", List.of(), "byOrganisationType", List.of(), "byItem", List.of()));

    assertThat(textOf(ReportViews.of(sections, List.of())))
        .contains("1. Alert Timeline", "4. Resource Distribution");
  }

  @Test
  void export_aReportWithNoSectionsAtAllIsStillAValidDocument() throws IOException {
    assertThat(textOf(ReportViews.of(Map.of(), List.of()))).contains("Disaster Analytics Report");
  }
}
