package lk.dmc.disaster.reports.controller;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lk.dmc.disaster.reports.entity.RejectionReason;
import lk.dmc.disaster.reports.entity.ReportRules;

/** Rejection reason; the comment is required when the reason is OTHER (checked in the domain). */
public record RejectRequest(
    @NotNull RejectionReason reason, @Size(max = ReportRules.COMMENT_MAX) String comment) {}
