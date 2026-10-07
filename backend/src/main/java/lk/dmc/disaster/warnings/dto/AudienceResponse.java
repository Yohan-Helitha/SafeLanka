package lk.dmc.disaster.warnings.dto;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Who a warning would reach, and the state of each channel it would go out on. */
public record AudienceResponse(
    long citizenCount, Set<UUID> resolvedDistrictIds, List<ChannelSettingResponse> channels) {}
