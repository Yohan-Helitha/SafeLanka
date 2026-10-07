package lk.dmc.disaster.reports.controller;

import static lk.dmc.disaster.reports.service.ReportFixtures.NOW;

import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.TestcontainersConfiguration;
import lk.dmc.disaster.reports.entity.HazardReport;
import lk.dmc.disaster.reports.service.ReportDetailView;
import lk.dmc.disaster.reports.service.ReportFixtures;
import lk.dmc.disaster.reports.service.ReportQueryService;
import lk.dmc.disaster.reports.service.ReportSubmissionService;
import lk.dmc.disaster.reports.service.ReportVerificationService;
import lk.dmc.disaster.shared.actor.UserSummary;
import lk.dmc.disaster.shared.domain.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Real web stack, real role checks (demo-auth: the {@code X-Acting-User} header names a seeded
 * user), mocked services. The context starts against the configured database but nothing is written.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "demo-auth"})
@Import(TestcontainersConfiguration.class)
abstract class ReportWebTestSupport {

  static final String HEADER = "X-Acting-User";
  static final String OFFICER = "00000000-0000-0000-0006-000000000001";
  static final String DISTRICT_OFFICER = "00000000-0000-0000-0006-000000000002";
  static final String CITIZEN = "00000000-0000-0000-0006-000000000004";
  static final String VOLUNTEER = "00000000-0000-0000-0006-000000000005";

  @Autowired MockMvc mvc;

  @MockitoBean ReportSubmissionService submission;
  @MockitoBean ReportQueryService queries;
  @MockitoBean ReportVerificationService verification;

  static HazardReport gpsReport() {
    return ReportFixtures.gps(UUID.fromString(CITIZEN), 6.9391, 79.8921, NOW);
  }

  static ReportDetailView detailOf(HazardReport report) {
    return new ReportDetailView(
        report,
        null,
        new UserSummary(report.getReporterId(), "Ruwan Fernando", Role.CITIZEN),
        List.of(),
        null);
  }
}
