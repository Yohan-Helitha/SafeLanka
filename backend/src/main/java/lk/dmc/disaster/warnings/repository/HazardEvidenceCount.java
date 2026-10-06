package lk.dmc.disaster.warnings.repository;

import java.util.UUID;

/** How many verified reports are linked to one hazard. */
public record HazardEvidenceCount(UUID hazardId, long count) {}
