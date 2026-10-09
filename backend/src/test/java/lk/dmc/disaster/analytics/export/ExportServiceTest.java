package lk.dmc.disaster.analytics.export;

import static lk.dmc.disaster.analytics.AnalyticsFixtures.AUTHOR_ID;
import static lk.dmc.disaster.analytics.AnalyticsFixtures.EVENT_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import lk.dmc.disaster.analytics.AnalyticsFixtures;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import lk.dmc.disaster.shared.error.NotFoundException;
import lk.dmc.disaster.shared.reference.ReferenceData;
import lk.dmc.disaster.shared.reference.UserDirectory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

  @Mock ReferenceData referenceData;
  @Mock UserDirectory userDirectory;

  private ExportService service;

  @BeforeEach
  void setUp() {
    service =
        new ExportService(
            List.of(new PdfReportExporter(), new CsvReportExporter()), referenceData, userDirectory);
  }

  private void givenEventAndAuthor() {
    when(referenceData.event(EVENT_ID)).thenReturn(AnalyticsFixtures.closedEvent());
    when(userDirectory.require(AUTHOR_ID)).thenReturn(AnalyticsFixtures.author());
  }

  @Test
  void export_pdf_returnsAPdfNamedAfterTheEventAndTheDate() {
    givenEventAndAuthor();

    ExportedFile file = service.export(AnalyticsFixtures.emptyReport(), "PDF");

    assertThat(file.filename()).isEqualTo("disaster-report-kalu-flood-may-2026-20261004.pdf");
    assertThat(file.contentType()).isEqualTo("application/pdf");
    assertThat(new String(file.content(), 0, 5)).isEqualTo("%PDF-");
  }

  @Test
  void export_csv_returnsACsvFile() {
    givenEventAndAuthor();

    ExportedFile file = service.export(AnalyticsFixtures.emptyReport(), "CSV");

    assertThat(file.filename()).isEqualTo("disaster-report-kalu-flood-may-2026-20261004.csv");
    assertThat(file.contentType()).isEqualTo("text/csv");
    assertThat(new String(file.content())).startsWith("section,label,value,extra");
  }

  @Test
  void export_acceptsTheFormatInAnyCase() {
    givenEventAndAuthor();

    assertThat(service.export(AnalyticsFixtures.emptyReport(), "csv").filename()).endsWith(".csv");
  }

  @Test
  void export_unknownFormat_is400AndNothingIsLookedUp() {
    assertThatThrownBy(() -> service.export(AnalyticsFixtures.emptyReport(), "EXCEL"))
        .isInstanceOfSatisfying(
            AppException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.VALIDATION_ERROR))
        .hasMessageContaining("PDF or CSV");

    verifyNoInteractions(referenceData, userDirectory);
  }

  @Test
  void export_nullFormat_is400() {
    assertThatThrownBy(() -> service.export(AnalyticsFixtures.emptyReport(), null))
        .isInstanceOf(AppException.class);
  }

  @Test
  void export_whenNoExporterIsRegisteredForTheFormat_is400() {
    var onlyCsv = new ExportService(List.of(new CsvReportExporter()), referenceData, userDirectory);

    assertThatThrownBy(() -> onlyCsv.export(AnalyticsFixtures.emptyReport(), "PDF"))
        .isInstanceOfSatisfying(
            AppException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.VALIDATION_ERROR));
  }

  @Test
  void export_eventNoLongerExisting_isNotFound() {
    when(referenceData.event(EVENT_ID)).thenThrow(new NotFoundException("Event not found"));

    assertThatThrownBy(() -> service.export(AnalyticsFixtures.emptyReport(), "PDF"))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void fileName_turnsAnyEventNameIntoASafeSlug() {
    Instant at = Instant.parse("2026-12-31T23:59:59Z");

    assertThat(ExportService.fileName("  Kalu: Flood (May) 2026!  ", at, ExportFormat.PDF))
        .isEqualTo("disaster-report-kalu-flood-may-2026-20261231.pdf");
  }

  @Test
  void fileName_usesTheUtcDateOfGeneration() {
    assertThat(ExportService.fileName("Event", Instant.parse("2026-01-02T00:00:00Z"), ExportFormat.CSV))
        .isEqualTo("disaster-report-event-20260102.csv");
  }
}
