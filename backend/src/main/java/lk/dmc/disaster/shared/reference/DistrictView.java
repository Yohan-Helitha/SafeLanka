package lk.dmc.disaster.shared.reference;
import java.util.UUID;
/** View for District. */
public record DistrictView(UUID id, String code, String name, String province) {
    public DistrictView(District d) {
        this(d.getId(), d.getCode(), d.getName(), d.getProvince());
    }
}
