package lk.dmc.disaster.reports.controller;

import jakarta.validation.constraints.Size;
import lk.dmc.disaster.reports.entity.ReportRules;

/** Optional note when verifying. */
public record VerifyRequest(@Size(max = ReportRules.COMMENT_MAX) String comment) {}
