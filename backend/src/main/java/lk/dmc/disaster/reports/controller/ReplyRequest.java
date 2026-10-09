package lk.dmc.disaster.reports.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lk.dmc.disaster.reports.entity.ReportRules;

/** The reporter's answer to the officer's question. */
public record ReplyRequest(
    @NotBlank @Size(min = ReportRules.REQUEST_INFO_COMMENT_MIN, max = ReportRules.COMMENT_MAX)
        String message) {}
