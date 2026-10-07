package lk.dmc.disaster.analytics.query;

import lk.dmc.disaster.analytics.domain.ReportContext;
import lk.dmc.disaster.analytics.domain.SectionResult;

public interface SectionQuery<T> {
    SectionResult<T> execute(ReportContext context);
}
