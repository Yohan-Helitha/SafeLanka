package lk.dmc.disaster.analytics.export;

import lk.dmc.disaster.analytics.domain.DisasterReport;

public interface ReportExporter {
    boolean supports(String format);
    byte[] export(DisasterReport report);
}
