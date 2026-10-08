package lk.dmc.disaster.reports.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lk.dmc.disaster.reports.entity.ReportRules;

/**
 * Optional note and hazard severity when verifying.
 *
 * @param severity how dangerous the officer judges the hazard, 1 (low) to 5 (very dangerous), or
 *     null to let the evidence decide
 */
public record VerifyRequest(
    @Size(max = ReportRules.COMMENT_MAX) String comment, @Min(1) @Max(5) Integer severity) {}
