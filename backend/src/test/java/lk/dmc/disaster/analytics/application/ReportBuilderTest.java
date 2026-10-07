package lk.dmc.disaster.analytics.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import lk.dmc.disaster.analytics.domain.ReportContext;
import lk.dmc.disaster.analytics.domain.SectionKey;
import lk.dmc.disaster.analytics.domain.SectionResult;
import lk.dmc.disaster.analytics.section.ReportSection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import lk.dmc.disaster.support.TestIds;

@ExtendWith(MockitoExtension.class)
class ReportBuilderTest {

    @Mock ReportSection section1;
    @Mock ReportSection section2;

    @Test
    void shouldBuildReportWithAvailableAndUnavailableSections() {
        var builder = new ReportBuilder(List.of(section1, section2));
        var context = new ReportContext(TestIds.event(1), null, null, null, TestIds.user(1));

        org.mockito.Mockito.doReturn(SectionResult.success(SectionKey.ALERT_TIMELINE, "DATA1")).when(section1).generate(context);
        org.mockito.Mockito.doReturn(SectionResult.unavailable(SectionKey.CITIZENS_REACHED, "No data")).when(section2).generate(context);

        var report = builder.build(context);

        assertThat(report.getEventId()).isEqualTo(TestIds.event(1));
        assertThat(report.getSections()).containsEntry("ALERT_TIMELINE", "DATA1");
        assertThat(report.getUnavailableSections()).hasSize(1);
        assertThat(report.getUnavailableSections().get(0)).containsEntry("key", "CITIZENS_REACHED");
        assertThat(report.getUnavailableSections().get(0)).containsEntry("reason", "No data");
    }
}
