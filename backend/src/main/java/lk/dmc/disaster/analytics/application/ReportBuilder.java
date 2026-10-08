package lk.dmc.disaster.analytics.application;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lk.dmc.disaster.analytics.domain.DisasterReport;
import lk.dmc.disaster.analytics.domain.ReportContext;
import lk.dmc.disaster.analytics.domain.SectionResult;
import lk.dmc.disaster.analytics.section.ReportSection;
import org.springframework.stereotype.Component;

@Component
public class ReportBuilder {
    private final List<ReportSection> sections;

    public ReportBuilder(List<ReportSection> sections) {
        this.sections = sections;
    }

    public DisasterReport build(ReportContext context) {
        Map<String, Object> filterMap = new HashMap<>();
        if (context.districtIds() != null && !context.districtIds().isEmpty()) {
            filterMap.put("districtIds", context.districtIds());
        }
        if (context.fromTime() != null) filterMap.put("fromTime", context.fromTime().toString());
        if (context.toTime() != null) filterMap.put("toTime", context.toTime().toString());

        Map<String, Object> sectionMap = new HashMap<>();
        List<Map<String, String>> unavailable = new java.util.ArrayList<>();

        for (ReportSection section : sections) {
            SectionResult<?> result = section.generate(context);
            if (result.isUnavailable()) {
                unavailable.add(Map.of("key", result.key().name(), "reason", result.unavailableReason()));
            } else {
                sectionMap.put(result.key().name(), result.data());
            }
        }

        return new DisasterReport(
            context.eventId(),
            filterMap,
            sectionMap,
            unavailable,
            context.generatedBy(),
            Instant.now()
        );
    }
}
