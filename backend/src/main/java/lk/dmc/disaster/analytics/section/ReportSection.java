package lk.dmc.disaster.analytics.section;

import lk.dmc.disaster.analytics.entity.ReportContext;
import lk.dmc.disaster.analytics.entity.SectionKey;
import lk.dmc.disaster.analytics.entity.SectionResult;

/** One part of a disaster report. Each implementation is a Spring bean; the report builder runs them in {@link SectionKey} order. */
public interface ReportSection {
    SectionKey getKey();
    SectionResult<?> generate(ReportContext context);
}
