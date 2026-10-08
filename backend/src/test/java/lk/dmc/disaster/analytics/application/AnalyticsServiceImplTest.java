package lk.dmc.disaster.analytics.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lk.dmc.disaster.analytics.domain.DisasterReport;
import lk.dmc.disaster.analytics.domain.ReportContext;
import lk.dmc.disaster.analytics.persistence.DisasterReportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.simple.JdbcClient;
import lk.dmc.disaster.analytics.query.EventSummaryQuery;
import lk.dmc.disaster.analytics.domain.EventSummary;
import java.util.Optional;
import java.util.UUID;
import lk.dmc.disaster.support.TestIds;
@ExtendWith(MockitoExtension.class)
class AnalyticsServiceImplTest {

    @Mock ReportBuilder reportBuilder;
    @Mock DisasterReportRepository repository;
    @Mock EventSummaryQuery eventSummaryQuery;
    @Mock JdbcClient jdbcClient;
    @Mock JdbcClient.StatementSpec statementSpec;
    @Mock JdbcClient.MappedQuerySpec<Integer> countQuerySpec;

    @InjectMocks AnalyticsServiceImpl service;

    @Test
    void unknownEventSavesNothing() {
        var context = new ReportContext(TestIds.event(1), null, null, null, TestIds.user(1));

        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);
        when(statementSpec.query(Integer.class)).thenReturn(countQuerySpec);
        when(countQuerySpec.single()).thenReturn(0);

        assertThatThrownBy(() -> service.generateReport(context))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unknown event");

        verify(repository, never()).save(any());
    }
    
    @Test
    void generateReportSuccess() {
        var context = new ReportContext(TestIds.event(1), null, null, null, TestIds.user(1));
        var report = new DisasterReport(TestIds.event(1), java.util.Map.of(), java.util.Map.of(), List.of(), TestIds.user(1), Instant.now());

        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);
        when(statementSpec.query(Integer.class)).thenReturn(countQuerySpec);
        when(countQuerySpec.single()).thenReturn(1);
        
        when(reportBuilder.build(context)).thenReturn(report);
        when(repository.save(report)).thenReturn(report);

        var result = service.generateReport(context);
        assertThat(result).isNotNull();
        verify(repository).save(report);
    }

    @Test
    void shouldThrowWhenTimeWindowTooLarge() {
        var context = new ReportContext(TestIds.event(1), null, Instant.now().minus(20, ChronoUnit.DAYS), Instant.now(), TestIds.user(1));

        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);
        when(statementSpec.query(Integer.class)).thenReturn(countQuerySpec);
        when(countQuerySpec.single()).thenReturn(1);

        assertThatThrownBy(() -> service.generateReport(context))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Time range exceeds");

        verify(repository, never()).save(any());
    }

    @Test
    void listAvailableEvents_callsQuery() {
        var summary = new EventSummary(TestIds.event(1), "Test Event", TestIds.hazardType(1), "ACTIVE", Instant.now(), null, new UUID[0], 10, 5);
        when(eventSummaryQuery.execute("ACTIVE")).thenReturn(List.of(summary));
        
        var result = service.listAvailableEvents("ACTIVE");
        assertThat(result).hasSize(1);
        verify(eventSummaryQuery).execute("ACTIVE");
    }

    @Test
    void listSavedReports_callsRepository() {
        var eventId = TestIds.event(1);
        when(repository.findByEventIdOrderByGeneratedAtDesc(eventId)).thenReturn(List.of());
        
        var result = service.listSavedReports(eventId);
        assertThat(result).isEmpty();
        verify(repository).findByEventIdOrderByGeneratedAtDesc(eventId);
    }

    @Test
    void getReport_callsRepository() {
        var reportId = UUID.randomUUID();
        var report = new DisasterReport(TestIds.event(1), java.util.Map.of(), java.util.Map.of(), List.of(), TestIds.user(1), Instant.now());
        when(repository.findById(reportId)).thenReturn(Optional.of(report));
        
        var result = service.getReport(reportId);
        assertThat(result).isNotNull();
        verify(repository).findById(reportId);
    }
    
    @Test
    void unknownEventSavesNothing_whenCountIsNull() {
        var context = new ReportContext(TestIds.event(1), null, null, null, TestIds.user(1));

        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);
        when(statementSpec.query(Integer.class)).thenReturn(countQuerySpec);
        when(countQuerySpec.single()).thenReturn(null);

        assertThatThrownBy(() -> service.generateReport(context))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unknown event");
    }
    
    @Test
    void generateReportSuccess_withTimeWindow() {
        var context = new ReportContext(TestIds.event(1), null, Instant.now().minus(5, ChronoUnit.DAYS), Instant.now(), TestIds.user(1));
        var report = new DisasterReport(TestIds.event(1), java.util.Map.of(), java.util.Map.of(), List.of(), TestIds.user(1), Instant.now());

        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);
        when(statementSpec.query(Integer.class)).thenReturn(countQuerySpec);
        when(countQuerySpec.single()).thenReturn(1);
        
        when(reportBuilder.build(context)).thenReturn(report);
        when(repository.save(report)).thenReturn(report);

        var result = service.generateReport(context);
        assertThat(result).isNotNull();
    }
    
    @Test
    void listSavedReports_withNullEventId_callsFindAll() {
        when(repository.findAllByOrderByGeneratedAtDesc()).thenReturn(List.of());
        var result = service.listSavedReports(null);
        assertThat(result).isEmpty();
        verify(repository).findAllByOrderByGeneratedAtDesc();
    }
    
    @Test
    void getReport_throwsWhenNotFound() {
        var reportId = UUID.randomUUID();
        when(repository.findById(reportId)).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> service.getReport(reportId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Report not found");
    }
}
