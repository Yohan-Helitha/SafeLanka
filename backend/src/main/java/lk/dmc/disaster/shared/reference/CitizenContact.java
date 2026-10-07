package lk.dmc.disaster.shared.reference;
import java.util.UUID;
/** Citizen contact info. */
public record CitizenContact(UUID userId, String phone, String preferredLanguage, UUID districtId, UUID riverBasinId) {
    public CitizenContact(AppUser u) {
        this(u.getId(), u.getPhone(), u.getPreferredLanguage(), u.getDistrictId(), u.getRiverBasinId());
    }
}
