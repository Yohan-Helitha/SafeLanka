package lk.dmc.disaster.reports.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import lk.dmc.disaster.shared.error.BusinessRuleException;
import lk.dmc.disaster.shared.error.ErrorCode;
import lk.dmc.disaster.shared.error.InvalidStateTransitionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class HazardReportTest {

  private static final Instant NOW = Instant.parse("2026-10-04T08:10:02Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
  private static final Clock LATER = Clock.fixed(NOW.plusSeconds(600), ZoneOffset.UTC);
  private static final UUID REPORTER = UUID.randomUUID();
  private static final UUID OFFICER = UUID.randomUUID();
  private static final String DESCRIPTION = "Water over the road near Kolonnawa canal bridge";

  private static ReportDraft draft(
      String description, Double lat, Double lng, String manualText, Instant capturedAt) {
    return new ReportDraft(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "RISING_WATER",
        description,
        lat,
        lng,
        manualText,
        UUID.randomUUID(),
        capturedAt);
  }

  private static ReportDraft gpsDraft() {
    return draft(DESCRIPTION, 6.9391, 79.8921, null, NOW.minusSeconds(120));
  }

  private static HazardReport pending() {
    return HazardReport.submit(gpsDraft(), REPORTER, "RPT-2026-0013", CLOCK);
  }

  // ---- submit -------------------------------------------------------------------------------

  @Test
  void submit_withGpsCreatesPendingReport() {
    ReportDraft draft = gpsDraft();

    HazardReport r = HazardReport.submit(draft, REPORTER, "RPT-2026-0013", CLOCK);

    assertThat(r.getId()).isNotNull();
    assertThat(r.getReferenceNo()).isEqualTo("RPT-2026-0013");
    assertThat(r.getReporterId()).isEqualTo(REPORTER);
    assertThat(r.getStatus()).isEqualTo(ReportStatus.PENDING);
    assertThat(r.getClientRef()).isEqualTo(draft.clientRef());
    assertThat(r.getCapturedAt()).isEqualTo(draft.capturedAt());
    assertThat(r.getSyncedAt()).isEqualTo(NOW);
    assertThat(r.isManualLocation()).isFalse();
    assertThat(r.point()).hasValueSatisfying(p -> assertThat(p.latitude()).isEqualTo(6.9391));
    assertThat(r.getReviewedBy()).isNull();
    assertThat(r.isReportedBy(REPORTER)).isTrue();
    assertThat(r.isReportedBy(OFFICER)).isFalse();
  }

  @Test
  void submit_withoutGpsStoresPlaceDescription() {
    HazardReport r =
        HazardReport.submit(
            draft(DESCRIPTION, null, null, "  Near the old railway bridge  ", NOW),
            REPORTER,
            "RPT-2026-0014",
            CLOCK);

    assertThat(r.isManualLocation()).isTrue();
    assertThat(r.getLatitude()).isNull();
    assertThat(r.getLongitude()).isNull();
    assertThat(r.getManualLocationText()).isEqualTo("Near the old railway bridge");
    assertThat(r.point()).isEmpty();
  }

  @Test
  void submit_onlyOneCoordinateCountsAsMissingGps() {
    HazardReport r =
        HazardReport.submit(
            draft(DESCRIPTION, 6.9, null, "Near the old railway bridge", NOW),
            REPORTER,
            "RPT-2026-0015",
            CLOCK);

    assertThat(r.isManualLocation()).isTrue();
    assertThat(r.getLatitude()).isNull();
  }

  @Test
  void submit_noLocationAtAllIsRejected() {
    assertThatThrownBy(
            () ->
                HazardReport.submit(
                    draft(DESCRIPTION, null, null, null, NOW), REPORTER, "R", CLOCK))
        .isInstanceOf(BusinessRuleException.class);
  }

  @ParameterizedTest
  @CsvSource({"4,false", "5,true", "200,true", "201,false"})
  void submit_placeDescriptionLengthIsFiveToTwoHundred(int length, boolean accepted) {
    ReportDraft draft = draft(DESCRIPTION, null, null, "x".repeat(length), NOW);

    if (accepted) {
      assertThat(HazardReport.submit(draft, REPORTER, "R", CLOCK).getManualLocationText())
          .hasSize(length);
    } else {
      assertThatThrownBy(() -> HazardReport.submit(draft, REPORTER, "R", CLOCK))
          .isInstanceOf(BusinessRuleException.class);
    }
  }

  @ParameterizedTest
  @CsvSource({"9,false", "10,true", "500,true", "501,false"})
  void submit_descriptionLengthIsTenToFiveHundred(int length, boolean accepted) {
    ReportDraft draft = draft("x".repeat(length), 6.9, 79.9, null, NOW);

    if (accepted) {
      assertThat(HazardReport.submit(draft, REPORTER, "R", CLOCK).getDescription()).hasSize(length);
    } else {
      assertThatThrownBy(() -> HazardReport.submit(draft, REPORTER, "R", CLOCK))
          .isInstanceOf(BusinessRuleException.class);
    }
  }

  @Test
  void submit_descriptionIsTrimmedBeforeCounting() {
    HazardReport r =
        HazardReport.submit(draft("   0123456789   ", 6.9, 79.9, null, NOW), REPORTER, "R", CLOCK);

    assertThat(r.getDescription()).isEqualTo("0123456789");
    assertThatThrownBy(
            () ->
                HazardReport.submit(
                    draft("   012345678   ", 6.9, 79.9, null, NOW), REPORTER, "R", CLOCK))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void submit_nullDescriptionIsRejected() {
    assertThatThrownBy(
            () -> HazardReport.submit(draft(null, 6.9, 79.9, null, NOW), REPORTER, "R", CLOCK))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void submit_coordinatesOutsideSriLankaAreRejected() {
    assertThatThrownBy(
            () ->
                HazardReport.submit(
                    draft(DESCRIPTION, 51.5, -0.12, null, NOW), REPORTER, "R", CLOCK))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("Sri Lanka");
  }

  @Test
  void submit_captureTimeUpToFiveMinutesAheadIsAccepted() {
    Instant ahead = NOW.plus(Duration.ofMinutes(5));

    assertThat(
            HazardReport.submit(draft(DESCRIPTION, 6.9, 79.9, null, ahead), REPORTER, "R", CLOCK)
                .getCapturedAt())
        .isEqualTo(ahead);
  }

  @Test
  void submit_captureTimeMoreThanFiveMinutesAheadIsRejected() {
    Instant ahead = NOW.plus(Duration.ofMinutes(5)).plusSeconds(1);

    assertThatThrownBy(
            () ->
                HazardReport.submit(
                    draft(DESCRIPTION, 6.9, 79.9, null, ahead), REPORTER, "R", CLOCK))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("future");
  }

  @Test
  void submit_oldCaptureTimeFromOfflineQueueIsAccepted() {
    Instant yesterday = NOW.minus(Duration.ofHours(20));

    assertThat(
            HazardReport.submit(
                    draft(DESCRIPTION, 6.9, 79.9, null, yesterday), REPORTER, "R", CLOCK)
                .getSyncedAt())
        .isEqualTo(NOW);
  }

  // ---- verify -------------------------------------------------------------------------------

  @Test
  void verify_setsReviewerTimeAndTrimmedComment() {
    HazardReport r = pending();

    r.verify(OFFICER, "  Confirmed on site  ", LATER);

    assertThat(r.getStatus()).isEqualTo(ReportStatus.VERIFIED);
    assertThat(r.getReviewedBy()).isEqualTo(OFFICER);
    assertThat(r.getReviewedAt()).isEqualTo(LATER.instant());
    assertThat(r.getReviewComment()).isEqualTo("Confirmed on site");
    assertThat(r.getRejectionReason()).isNull();
  }

  @Test
  void verify_commentIsOptional() {
    HazardReport r = pending();

    r.verify(OFFICER, "   ", CLOCK);

    assertThat(r.getReviewComment()).isNull();
  }

  @Test
  void verify_commentOfThreeHundredIsAcceptedAndThreeHundredOneRejected() {
    HazardReport ok = pending();
    ok.verify(OFFICER, "x".repeat(300), CLOCK);
    assertThat(ok.getReviewComment()).hasSize(300);

    HazardReport tooLong = pending();
    assertThatThrownBy(() -> tooLong.verify(OFFICER, "x".repeat(301), CLOCK))
        .isInstanceOf(BusinessRuleException.class);
    assertThat(tooLong.getStatus()).isEqualTo(ReportStatus.PENDING);
  }

  @Test
  void verify_ownReportIsRejectedWithBusinessRule() {
    HazardReport r = pending();

    assertThatThrownBy(() -> r.verify(REPORTER, null, CLOCK))
        .isInstanceOfSatisfying(
            BusinessRuleException.class,
            e -> assertThat(e.code()).isEqualTo(ErrorCode.BUSINESS_RULE));
    assertThat(r.getStatus()).isEqualTo(ReportStatus.PENDING);
    assertThat(r.getReviewedBy()).isNull();
  }

  @Test
  void verify_alreadyVerifiedIsConflict() {
    HazardReport r = pending();
    r.verify(OFFICER, null, CLOCK);

    assertThatThrownBy(() -> r.verify(OFFICER, null, CLOCK))
        .isInstanceOf(InvalidStateTransitionException.class);
  }

  @Test
  void verify_fromNeedsMoreInfoIsAllowed() {
    HazardReport r = pending();
    r.requestInfo(OFFICER, "Please add a photo", CLOCK);

    r.verify(OFFICER, "Photo received", LATER);

    assertThat(r.getStatus()).isEqualTo(ReportStatus.VERIFIED);
    assertThat(r.getReviewComment()).isEqualTo("Photo received");
  }

  // ---- reject -------------------------------------------------------------------------------

  @Test
  void reject_storesReasonAndComment() {
    HazardReport r = pending();

    r.reject(OFFICER, RejectionReason.LOCATION_MISMATCH, "Pin is 2 km away", LATER);

    assertThat(r.getStatus()).isEqualTo(ReportStatus.REJECTED);
    assertThat(r.getRejectionReason()).isEqualTo(RejectionReason.LOCATION_MISMATCH);
    assertThat(r.getReviewComment()).isEqualTo("Pin is 2 km away");
    assertThat(r.getReviewedAt()).isEqualTo(LATER.instant());
  }

  @ParameterizedTest
  @EnumSource(value = RejectionReason.class, mode = EnumSource.Mode.EXCLUDE, names = "OTHER")
  void reject_commentIsOptionalForNamedReasons(RejectionReason reason) {
    HazardReport r = pending();

    r.reject(OFFICER, reason, null, CLOCK);

    assertThat(r.getRejectionReason()).isEqualTo(reason);
    assertThat(r.getReviewComment()).isNull();
  }

  @Test
  void reject_withoutReasonIsRejected() {
    HazardReport r = pending();

    assertThatThrownBy(() -> r.reject(OFFICER, null, "No reason", CLOCK))
        .isInstanceOf(BusinessRuleException.class);
    assertThat(r.getStatus()).isEqualTo(ReportStatus.PENDING);
  }

  @ParameterizedTest
  @CsvSource(
      value = {"null", "''", "'   '"},
      nullValues = "null")
  void reject_otherWithoutCommentIsRejected(String comment) {
    HazardReport r = pending();

    assertThatThrownBy(() -> r.reject(OFFICER, RejectionReason.OTHER, comment, CLOCK))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("Other");
    assertThat(r.getStatus()).isEqualTo(ReportStatus.PENDING);
  }

  @Test
  void reject_otherWithCommentIsAccepted() {
    HazardReport r = pending();

    r.reject(OFFICER, RejectionReason.OTHER, "Test photo from the internet", CLOCK);

    assertThat(r.getStatus()).isEqualTo(ReportStatus.REJECTED);
  }

  @Test
  void reject_ownReportIsRejectedWithBusinessRule() {
    HazardReport r = pending();

    assertThatThrownBy(() -> r.reject(REPORTER, RejectionReason.DUPLICATE, null, CLOCK))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void reject_afterVerifiedIsConflict() {
    HazardReport r = pending();
    r.verify(OFFICER, null, CLOCK);

    assertThatThrownBy(() -> r.reject(OFFICER, RejectionReason.DUPLICATE, null, CLOCK))
        .isInstanceOf(InvalidStateTransitionException.class);
    assertThat(r.getRejectionReason()).isNull();
  }

  @Test
  void reject_fromNeedsMoreInfoIsAllowed() {
    HazardReport r = pending();
    r.requestInfo(OFFICER, "Please add a photo", CLOCK);

    r.reject(OFFICER, RejectionReason.INSUFFICIENT_EVIDENCE, null, LATER);

    assertThat(r.getStatus()).isEqualTo(ReportStatus.REJECTED);
    assertThat(r.getReviewComment()).isNull();
  }

  // ---- request info -------------------------------------------------------------------------

  @Test
  void requestInfo_movesToNeedsMoreInfoWithComment() {
    HazardReport r = pending();

    r.requestInfo(OFFICER, "Which side of the bridge?", LATER);

    assertThat(r.getStatus()).isEqualTo(ReportStatus.NEEDS_MORE_INFO);
    assertThat(r.getReviewedBy()).isEqualTo(OFFICER);
    assertThat(r.getReviewComment()).isEqualTo("Which side of the bridge?");
  }

  @ParameterizedTest
  @CsvSource({"4,false", "5,true", "300,true", "301,false"})
  void requestInfo_commentLengthIsFiveToThreeHundred(int length, boolean accepted) {
    HazardReport r = pending();

    if (accepted) {
      r.requestInfo(OFFICER, "x".repeat(length), CLOCK);
      assertThat(r.getStatus()).isEqualTo(ReportStatus.NEEDS_MORE_INFO);
    } else {
      assertThatThrownBy(() -> r.requestInfo(OFFICER, "x".repeat(length), CLOCK))
          .isInstanceOf(BusinessRuleException.class);
      assertThat(r.getStatus()).isEqualTo(ReportStatus.PENDING);
    }
  }

  @Test
  void requestInfo_missingCommentIsRejected() {
    assertThatThrownBy(() -> pending().requestInfo(OFFICER, null, CLOCK))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void requestInfo_onlyFromPending() {
    HazardReport r = pending();
    r.requestInfo(OFFICER, "Please add a photo", CLOCK);

    assertThatThrownBy(() -> r.requestInfo(OFFICER, "Please add a photo", CLOCK))
        .isInstanceOf(InvalidStateTransitionException.class);
  }

  @Test
  void requestInfo_ownReportIsRejectedWithBusinessRule() {
    assertThatThrownBy(() -> pending().requestInfo(REPORTER, "Please add a photo", CLOCK))
        .isInstanceOf(BusinessRuleException.class);
  }
}
