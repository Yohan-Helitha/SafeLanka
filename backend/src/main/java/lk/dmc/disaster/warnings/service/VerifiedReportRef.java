package lk.dmc.disaster.warnings.service;

import java.util.UUID;

/** What the warnings module needs to know about a newly verified report. */
public record VerifiedReportRef(
    UUID reportId, UUID hazardTypeId, UUID districtId, String description) {}
