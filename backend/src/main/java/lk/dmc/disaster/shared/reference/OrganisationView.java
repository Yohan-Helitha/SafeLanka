package lk.dmc.disaster.shared.reference;
import java.util.UUID;
/** View for Organisation. */
public record OrganisationView(UUID id, String name, OrganisationType type, String contactPhone) {
    public OrganisationView(Organisation o) {
        this(o.getId(), o.getName(), o.getType(), o.getContactPhone());
    }
}
