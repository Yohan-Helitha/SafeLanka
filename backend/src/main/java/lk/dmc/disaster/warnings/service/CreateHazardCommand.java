package lk.dmc.disaster.warnings.service;

import java.util.UUID;
import lk.dmc.disaster.warnings.entity.HazardArea;

/**
 * A duty officer recording a hazard by hand.
 *
 * @param eventId the disaster event it belongs to, or null
 */
public record CreateHazardCommand(
    UUID hazardTypeId, int severity, HazardArea area, String description, UUID eventId) {}
