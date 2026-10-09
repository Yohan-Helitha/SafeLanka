package lk.dmc.disaster.analytics.entity;

import java.util.List;
import java.util.UUID;

public record CitizensReached(
    long uniqueCitizensTargeted,
    long uniqueCitizensReached,
    double deliveryRate,
    List<ChannelStats> byChannel,
    List<DistrictStats> byDistrict
) {
    public record ChannelStats(String channel, long delivered, long failed) {}
    public record DistrictStats(UUID districtId, String districtName, long targeted, long reached) {}
}
