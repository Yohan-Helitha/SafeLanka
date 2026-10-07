package lk.dmc.disaster.shared.reference;
import java.util.UUID;
/** View for RiverBasin. */
public record RiverBasinView(UUID id, String code, String name) {
    public RiverBasinView(RiverBasin b) {
        this(b.getId(), b.getCode(), b.getName());
    }
}
