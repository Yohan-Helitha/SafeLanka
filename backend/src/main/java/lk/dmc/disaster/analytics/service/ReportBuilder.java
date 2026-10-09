package lk.dmc.disaster.analytics.service;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lk.dmc.disaster.analytics.entity.DisasterReport;
import lk.dmc.disaster.analytics.entity.ReportContext;
import lk.dmc.disaster.analytics.entity.SectionKey;
import lk.dmc.disaster.analytics.entity.SectionResult;
import lk.dmc.disaster.analytics.section.ReportSection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Assembles a report from its sections (Builder). Every section runs, in {@link SectionKey} order;
 * one that has no data, or fails, is listed as unavailable with a reason and never stops the others.
 */
@Slf4j
@Component
public class ReportBuilder {

  static final String SECTION_FAILED = "This section could not be generated.";

  private final List<ReportSection> sections;
  private final Clock clock;

  public ReportBuilder(List<ReportSection> sections, Clock clock) {
    this.sections =
        sections.stream().sorted(Comparator.comparing(s -> s.getKey().ordinal())).toList();
    this.clock = clock;
  }

  /** Runs every section for the context; a section without data or that fails is recorded as unavailable with a reason. */
  public DisasterReport build(ReportContext context) {
    Map<String, Object> data = new LinkedHashMap<>();
    List<Map<String, String>> unavailable = new ArrayList<>();
    for (ReportSection section : sections) {
      SectionResult<?> result = run(section, context);
      if (result.isUnavailable()) {
        unavailable.add(Map.of("key", result.key().name(), "reason", result.unavailableReason()));
      } else {
        data.put(result.key().name(), result.data());
      }
    }
    return new DisasterReport(
        context.eventId(),
        filtersOf(context),
        data,
        unavailable,
        context.generatedBy(),
        clock.instant());
  }

  private SectionResult<?> run(ReportSection section, ReportContext context) {
    try {
      return section.generate(context);
    } catch (RuntimeException e) {
      log.error("Report section {} failed for event {}", section.getKey(), context.eventId(), e);
      return SectionResult.unavailable(section.getKey(), SECTION_FAILED);
    }
  }

  private static Map<String, Object> filtersOf(ReportContext context) {
    Map<String, Object> filters = new LinkedHashMap<>();
    if (context.districtIds() != null && !context.districtIds().isEmpty()) {
      filters.put(
          "districtIds", context.districtIds().stream().map(UUID::toString).sorted().toList());
    }
    if (context.fromTime() != null) {
      filters.put("from", context.fromTime().toString());
    }
    if (context.toTime() != null) {
      filters.put("to", context.toTime().toString());
    }
    return filters;
  }
}
