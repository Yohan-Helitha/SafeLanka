package lk.dmc.disaster.analytics.domain;

import java.util.List;
import java.util.UUID;

public record ResourceDistribution(
    List<DistrictDistribution> byDistrict,
    List<OrganisationTypeDistribution> byOrganisationType,
    List<ItemDistribution> byItem
) {
    public record DistrictDistribution(
        UUID districtId,
        String districtName,
        String itemCode,
        String unit,
        long allocated,
        long distributed
    ) {}
    public record OrganisationTypeDistribution(String type, long distributed) {}
    public record ItemDistribution(String itemCode, String unit, long distributed) {}
}
