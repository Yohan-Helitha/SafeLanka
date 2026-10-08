package lk.dmc.disaster.warnings.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lk.dmc.disaster.reports.VerifiedReportQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VerifiedReportQueryAdapterTest {

  private static final UUID COLOMBO = UUID.randomUUID();
  private static final Instant CAPTURED = Instant.parse("2026-10-06T10:00:00Z");

  @Mock private VerifiedReportQuery reports;

  private VerifiedReportQueryAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new VerifiedReportQueryAdapter(reports);
  }

  private static lk.dmc.disaster.reports.VerifiedReportSummary reportsModuleSummary(UUID id) {
    return new lk.dmc.disaster.reports.VerifiedReportSummary(
        id,
        "RPT-2026-0001",
        UUID.randomUUID(),
        "FLOOD",
        "Water over the road.",
        COLOMBO,
        6.9,
        79.9,
        false,
        "/photo.jpg",
        CAPTURED,
        CAPTURED.plusSeconds(60),
        UUID.randomUUID());
  }

  @Test
  void findVerified_keepsOnlyTheEvidenceFields() {
    UUID id = UUID.randomUUID();
    when(reports.findVerifiedById(id)).thenReturn(Optional.of(reportsModuleSummary(id)));

    assertThat(adapter.findVerified(List.of(id)))
        .containsExactly(
            new VerifiedReportSummary(
                id, "RPT-2026-0001", "FLOOD", "Water over the road.", COLOMBO, CAPTURED));
  }

  @Test
  void findVerified_leavesOutReportsThatAreNotVerified() {
    UUID verified = UUID.randomUUID();
    UUID pending = UUID.randomUUID();
    when(reports.findVerifiedById(verified))
        .thenReturn(Optional.of(reportsModuleSummary(verified)));
    when(reports.findVerifiedById(pending)).thenReturn(Optional.empty());

    assertThat(adapter.findVerified(List.of(verified, pending)))
        .extracting(VerifiedReportSummary::id)
        .containsExactly(verified);
  }

  @Test
  void findVerified_repeatedId_isAskedOnce() {
    UUID id = UUID.randomUUID();
    when(reports.findVerifiedById(id)).thenReturn(Optional.of(reportsModuleSummary(id)));

    assertThat(adapter.findVerified(List.of(id, id))).hasSize(1);
  }

  @Test
  void findVerified_noIds_doesNotAskTheReportsModule() {
    assertThat(adapter.findVerified(List.of())).isEmpty();

    verifyNoInteractions(reports);
  }
}
