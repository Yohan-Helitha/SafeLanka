package lk.dmc.disaster.warnings.dto;

import java.time.Instant;
import java.util.UUID;

/** A verified report linked to a hazard as evidence. */
public record EvidenceResponse(
    UUID reportId,
    String referenceNo,
    String category,
    String description,
    UUID districtId,
    Instant capturedAt) {}
