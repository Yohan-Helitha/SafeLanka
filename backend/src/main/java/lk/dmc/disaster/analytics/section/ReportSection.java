package lk.dmc.disaster.analytics.section;

import lk.dmc.disaster.analytics.domain.ReportContext;
import lk.dmc.disaster.analytics.domain.SectionKey;
import lk.dmc.disaster.analytics.domain.SectionResult;

public interface ReportSection {
    SectionKey getKey();
    SectionResult<?> generate(ReportContext context);
}
