package lk.dmc.disaster.analytics.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ExportValuesTest {

  @Test
  void instant_readsAnIsoString() {
    assertThat(ExportValues.instant("2026-05-14T01:30:00Z")).contains(Instant.parse("2026-05-14T01:30:00Z"));
  }

  @Test
  void instant_readsEpochSecondsWithAFraction() {
    assertThat(ExportValues.instant(1778722200.5)).contains(Instant.ofEpochMilli(1778722200500L));
  }

  @Test
  void instant_readsEpochSecondsGivenAsAString() {
    assertThat(ExportValues.instant("1778722200")).contains(Instant.ofEpochSecond(1778722200L));
  }

  @Test
  void instant_readsEpochMilliseconds() {
    assertThat(ExportValues.instant(1778722200000L)).contains(Instant.ofEpochMilli(1778722200000L));
  }

  @Test
  void instant_isEmptyForNullAndForText() {
    assertThat(ExportValues.instant(null)).isEmpty();
    assertThat(ExportValues.instant("yesterday")).isEmpty();
  }

  @Test
  void map_andList_fallBackToEmptyForTheWrongType() {
    assertThat(ExportValues.map("text")).isEmpty();
    assertThat(ExportValues.map(Map.of("a", 1)).get("a")).isEqualTo(1);
    assertThat(ExportValues.list(null)).isEmpty();
    assertThat(ExportValues.list(List.of(1, 2))).hasSize(2);
  }

  @Test
  void text_isEmptyForNull() {
    assertThat(ExportValues.text(null)).isEmpty();
    assertThat(ExportValues.text(42)).isEqualTo("42");
  }

  @Test
  void number_readsNumbersAndNumericStringsAndDefaultsToZero() {
    assertThat(ExportValues.number(7L)).isEqualTo(7);
    assertThat(ExportValues.number("12")).isEqualTo(12);
    assertThat(ExportValues.number("abc")).isZero();
    assertThat(ExportValues.number(null)).isZero();
  }

  @Test
  void decimal_readsNumbersAndNumericStringsAndDefaultsToZero() {
    assertThat(ExportValues.decimal(0.97)).isEqualTo(0.97);
    assertThat(ExportValues.decimal("0.5")).isEqualTo(0.5);
    assertThat(ExportValues.decimal("abc")).isZero();
    assertThat(ExportValues.decimal(null)).isZero();
  }

  @ParameterizedTest
  @ValueSource(strings = {"PDF", "pdf", "Pdf"})
  void exportFormat_parseIgnoresCase(String value) {
    assertThat(ExportFormat.parse(value)).isEqualTo(ExportFormat.PDF);
  }

  @Test
  void exportFormat_knowsItsContentTypeAndExtension() {
    assertThat(ExportFormat.CSV.contentType()).isEqualTo("text/csv");
    assertThat(ExportFormat.CSV.extension()).isEqualTo("csv");
    assertThat(ExportFormat.PDF.contentType()).isEqualTo("application/pdf");
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "XLSX", "pdf ", "docx"})
  void exportFormat_parseRejectsAnythingElseWith400(String value) {
    assertThatThrownBy(() -> ExportFormat.parse(value))
        .isInstanceOfSatisfying(
            AppException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.VALIDATION_ERROR));
  }
}
