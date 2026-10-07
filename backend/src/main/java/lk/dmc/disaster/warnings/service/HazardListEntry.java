package lk.dmc.disaster.warnings.service;

import lk.dmc.disaster.warnings.entity.Hazard;

/**
 * One row of the hazard list.
 *
 * @param latestReading the newest gauge reading, or null when the hazard has no gauge
 */
public record HazardListEntry(
    Hazard hazard, long verifiedReportCount, GaugeReading latestReading) {}
