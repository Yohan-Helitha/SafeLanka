$ErrorActionPreference = "Stop"
$queryTestDir = "src/test/java/lk/dmc/disaster/analytics/query"
$sectionTestDir = "src/test/java/lk/dmc/disaster/analytics/section"

New-Item -ItemType Directory -Force -Path $queryTestDir
New-Item -ItemType Directory -Force -Path $sectionTestDir

Set-Content -Path "$queryTestDir/WarningTimelineQueryTest.java" -Value @"
package lk.dmc.disaster.analytics.query;

import static org.assertj.core.api.Assertions.assertThat;

import lk.dmc.disaster.TestcontainersConfiguration;
import lk.dmc.disaster.analytics.domain.ReportContext;
import lk.dmc.disaster.support.TestIds;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@JdbcTest
@Import({TestcontainersConfiguration.class, WarningTimelineQuery.class})
@ActiveProfiles("test")
class WarningTimelineQueryTest {
    @Autowired WarningTimelineQuery query;

    @Test
    void execute_kaluEvent_returnsWarnings() {
        ReportContext ctx = new ReportContext(TestIds.event(2), null, null, null, TestIds.user(1));
        var res = query.execute(ctx);
        assertThat(res).hasSize(3);
    }
}
"@

Set-Content -Path "$queryTestDir/ReportTimingQueryTest.java" -Value @"
package lk.dmc.disaster.analytics.query;

import static org.assertj.core.api.Assertions.assertThat;

import lk.dmc.disaster.TestcontainersConfiguration;
import lk.dmc.disaster.analytics.domain.ReportContext;
import lk.dmc.disaster.support.TestIds;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@JdbcTest
@Import({TestcontainersConfiguration.class, ReportTimingQuery.class})
@ActiveProfiles("test")
class ReportTimingQueryTest {
    @Autowired ReportTimingQuery query;

    @Test
    void execute_kaluEvent_returnsTiming() {
        ReportContext ctx = new ReportContext(TestIds.event(2), null, null, null, TestIds.user(1));
        var res = query.execute(ctx);
        assertThat(res).isPresent();
    }
}
"@

Set-Content -Path "$queryTestDir/DeliveryStatsQueryTest.java" -Value @"
package lk.dmc.disaster.analytics.query;

import static org.assertj.core.api.Assertions.assertThat;

import lk.dmc.disaster.TestcontainersConfiguration;
import lk.dmc.disaster.analytics.domain.ReportContext;
import lk.dmc.disaster.support.TestIds;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@JdbcTest
@Import({TestcontainersConfiguration.class, DeliveryStatsQuery.class})
@ActiveProfiles("test")
class DeliveryStatsQueryTest {
    @Autowired DeliveryStatsQuery query;

    @Test
    void execute_kaluEvent_returnsStats() {
        ReportContext ctx = new ReportContext(TestIds.event(2), null, null, null, TestIds.user(1));
        assertThat(query.getUniqueTargeted(ctx)).isGreaterThan(0);
        assertThat(query.getUniqueReached(ctx)).isGreaterThan(0);
        assertThat(query.getChannelStats(ctx)).isNotEmpty();
        assertThat(query.getDistrictStats(ctx)).isNotEmpty();
    }
}
"@

Set-Content -Path "$queryTestDir/OccupancyQueryTest.java" -Value @"
package lk.dmc.disaster.analytics.query;

import static org.assertj.core.api.Assertions.assertThat;

import lk.dmc.disaster.TestcontainersConfiguration;
import lk.dmc.disaster.analytics.domain.ReportContext;
import lk.dmc.disaster.support.TestIds;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@JdbcTest
@Import({TestcontainersConfiguration.class, OccupancyQuery.class})
@ActiveProfiles("test")
class OccupancyQueryTest {
    @Autowired OccupancyQuery query;

    @Test
    void getPeaks_kaluEvent_returns290() {
        ReportContext ctx = new ReportContext(TestIds.event(2), null, null, null, TestIds.user(1));
        var peaks = query.getPeaks(ctx);
        assertThat(peaks).isNotEmpty();
        assertThat(peaks.get(0).peakOccupancy()).isEqualTo(290);
    }
}
"@

Set-Content -Path "$queryTestDir/DistributionQueryTest.java" -Value @"
package lk.dmc.disaster.analytics.query;

import static org.assertj.core.api.Assertions.assertThat;

import lk.dmc.disaster.TestcontainersConfiguration;
import lk.dmc.disaster.analytics.domain.ReportContext;
import lk.dmc.disaster.support.TestIds;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@JdbcTest
@Import({TestcontainersConfiguration.class, DistributionQuery.class})
@ActiveProfiles("test")
class DistributionQueryTest {
    @Autowired DistributionQuery query;

    @Test
    void execute_kaluEvent_returnsStats() {
        ReportContext ctx = new ReportContext(TestIds.event(2), null, null, null, TestIds.user(1));
        var byDistrict = query.getByDistrict(ctx);
        assertThat(byDistrict).isNotEmpty();
    }
}
"@

Set-Content -Path "$queryTestDir/EventSummaryQueryTest.java" -Value @"
package lk.dmc.disaster.analytics.query;

import static org.assertj.core.api.Assertions.assertThat;

import lk.dmc.disaster.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@JdbcTest
@Import({TestcontainersConfiguration.class, EventSummaryQuery.class})
@ActiveProfiles("test")
class EventSummaryQueryTest {
    @Autowired EventSummaryQuery query;

    @Test
    void execute_returnsEvents() {
        var res = query.execute(null);
        assertThat(res).isNotEmpty();
    }
}
"@

Set-Content -Path "$sectionTestDir/AlertTimelineSectionTest.java" -Value @"
package lk.dmc.disaster.analytics.section;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import lk.dmc.disaster.analytics.domain.ReportContext;
import lk.dmc.disaster.analytics.query.ReportTimingQuery;
import lk.dmc.disaster.analytics.query.WarningTimelineQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AlertTimelineSectionTest {
    @Mock WarningTimelineQuery warningQuery;
    @Mock ReportTimingQuery timingQuery;
    @InjectMocks AlertTimelineSection section;

    @Test
    void generate_noWarnings_unavailable() {
        when(warningQuery.execute(any())).thenReturn(List.of());
        var res = section.generate(null);
        assertThat(res.isUnavailable()).isTrue();
    }
}
"@

Set-Content -Path "$sectionTestDir/CitizensReachedSectionTest.java" -Value @"
package lk.dmc.disaster.analytics.section;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import lk.dmc.disaster.analytics.query.DeliveryStatsQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CitizensReachedSectionTest {
    @Mock DeliveryStatsQuery query;
    @InjectMocks CitizensReachedSection section;

    @Test
    void generate_noDeliveries_unavailable() {
        when(query.getUniqueTargeted(any())).thenReturn(0L);
        var res = section.generate(null);
        assertThat(res.isUnavailable()).isTrue();
    }
}
"@

Set-Content -Path "$sectionTestDir/ShelterOccupancySectionTest.java" -Value @"
package lk.dmc.disaster.analytics.section;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import lk.dmc.disaster.analytics.query.OccupancyQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShelterOccupancySectionTest {
    @Mock OccupancyQuery query;
    @InjectMocks ShelterOccupancySection section;

    @Test
    void generate_noLogs_unavailable() {
        when(query.getSeries(any())).thenReturn(List.of());
        var res = section.generate(null);
        assertThat(res.isUnavailable()).isTrue();
    }
}
"@

Set-Content -Path "$sectionTestDir/ResourceDistributionSectionTest.java" -Value @"
package lk.dmc.disaster.analytics.section;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import lk.dmc.disaster.analytics.query.DistributionQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResourceDistributionSectionTest {
    @Mock DistributionQuery query;
    @InjectMocks ResourceDistributionSection section;

    @Test
    void generate_noAllocations_unavailable() {
        when(query.getByDistrict(any())).thenReturn(List.of());
        var res = section.generate(null);
        assertThat(res.isUnavailable()).isTrue();
    }
}
"@
