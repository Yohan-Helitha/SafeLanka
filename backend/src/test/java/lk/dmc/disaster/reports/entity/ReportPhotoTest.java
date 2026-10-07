package lk.dmc.disaster.reports.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import lk.dmc.disaster.shared.error.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ReportPhotoTest {

  private static final UUID REPORT = UUID.randomUUID();

  @Test
  void attach_keepsPathTypeAndSize() {
    ReportPhoto p = ReportPhoto.attach(REPORT, "reports/a.jpg", "image/jpeg", 1234);

    assertThat(p.getId()).isNotNull();
    assertThat(p.getReportId()).isEqualTo(REPORT);
    assertThat(p.getFilePath()).isEqualTo("reports/a.jpg");
    assertThat(p.getMimeType()).isEqualTo("image/jpeg");
    assertThat(p.getSizeBytes()).isEqualTo(1234);
  }

  @ParameterizedTest
  @ValueSource(ints = {1, ReportRules.PHOTO_MAX_BYTES})
  void attach_sizeBoundariesAreAccepted(int size) {
    assertThat(ReportPhoto.attach(REPORT, "reports/a.png", "image/png", size).getSizeBytes())
        .isEqualTo(size);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1, ReportRules.PHOTO_MAX_BYTES + 1})
  void attach_sizeOutsideTheLimitsIsRejected(int size) {
    assertThatThrownBy(() -> ReportPhoto.attach(REPORT, "reports/a.png", "image/png", size))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void attach_otherMimeTypeIsRejected() {
    assertThatThrownBy(() -> ReportPhoto.attach(REPORT, "reports/a.gif", "image/gif", 10))
        .isInstanceOf(BusinessRuleException.class);
  }
}
