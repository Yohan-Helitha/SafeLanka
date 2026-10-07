package lk.dmc.disaster.shared.reference;
import java.util.UUID;
/** View for ReliefItem. */
public record ReliefItemView(UUID id, String code, String name, String unit, ReliefCategory category) {
    public ReliefItemView(ReliefItem r) {
        this(r.getId(), r.getCode(), r.getName(), r.getUnit(), r.getCategory());
    }
}
