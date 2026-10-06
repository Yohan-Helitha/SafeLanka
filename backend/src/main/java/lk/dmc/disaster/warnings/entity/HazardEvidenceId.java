package lk.dmc.disaster.warnings.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;

/** Composite key of {@link HazardEvidence}: one report can be linked to a hazard only once. */
@Embeddable
public record HazardEvidenceId(
    @Column(name = "hazard_id") UUID hazardId, @Column(name = "report_id") UUID reportId)
    implements Serializable {}
