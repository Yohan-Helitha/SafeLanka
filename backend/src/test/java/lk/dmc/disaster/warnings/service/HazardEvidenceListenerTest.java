package lk.dmc.disaster.warnings.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.reports.ReportVerifiedEvent;
import lk.dmc.disaster.warnings.integration.VerifiedReportSummary;
import lk.dmc.disaster.warnings.integration.VerifiedReports;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HazardEvidenceListenerTest {

  private static final Instant NOW = ServiceFixtures.NOW;
  private static final UUID REPORT = UUID.randomUUID();
  private static final UUID TYPE = UUID.randomUUID();
  private static final UUID DISTRICT = UUID.randomUUID();

  @Mock private HazardEvidenceLinker linker;
  @Mock private VerifiedReports verifiedReports;

  private HazardEvidenceListener listener;

  @BeforeEach
  void setUp() {
    listener = new HazardEvidenceListener(linker, verifiedReports);
  }

  private static ReportVerifiedEvent event() {
    return new ReportVerifiedEvent(REPORT, TYPE, "FLOOD", DISTRICT, null, null, NOW);
  }

  @Test
  void verifiedReport_isLinkedWithItsDescription() {
    when(verifiedReports.findVerified(List.of(REPORT)))
        .thenReturn(
            List.of(
                new VerifiedReportSummary(
                    REPORT, "RPT-1", "FLOOD", "Water over the road.", DISTRICT, NOW)));

    listener.on(event());

    verify(linker).link(new VerifiedReportRef(REPORT, TYPE, DISTRICT, "Water over the road."));
  }

  @Test
  void officersSeverity_isPassedToTheLinker() {
    when(verifiedReports.findVerified(List.of(REPORT)))
        .thenReturn(
            List.of(
                new VerifiedReportSummary(
                    REPORT, "RPT-1", "FLOOD", "Water over the road.", DISTRICT, NOW)));

    listener.on(new ReportVerifiedEvent(REPORT, TYPE, "FLOOD", DISTRICT, null, null, NOW, 4));

    verify(linker).link(new VerifiedReportRef(REPORT, TYPE, DISTRICT, "Water over the road.", 4));
  }

  @Test
  void reportThatCannotBeFound_isNotLinked() {
    when(verifiedReports.findVerified(List.of(REPORT))).thenReturn(List.of());

    listener.on(event());

    verify(linker, never()).link(any());
  }
}
