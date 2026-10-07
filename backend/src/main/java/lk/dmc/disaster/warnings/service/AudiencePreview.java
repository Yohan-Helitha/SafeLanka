package lk.dmc.disaster.warnings.service;

import java.util.Set;
import java.util.UUID;

/**
 * How many people a warning would reach.
 *
 * @param resolvedDistrictIds the picked districts plus every district of the picked basins
 */
public record AudiencePreview(long citizenCount, Set<UUID> resolvedDistrictIds) {}
