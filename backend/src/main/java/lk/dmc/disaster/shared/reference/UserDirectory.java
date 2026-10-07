package lk.dmc.disaster.shared.reference;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.shared.error.NotFoundException;

/** User directory interface. */
public interface UserDirectory {
    Optional<UserView> findById(UUID id);
    
    default UserView require(UUID id) {
        return findById(id).orElseThrow(() -> new NotFoundException("User not found"));
    }
    
    List<CitizenContact> findCitizensInAreas(Set<UUID> districtIds, Set<UUID> basinIds);
    long countCitizensInAreas(Set<UUID> districtIds, Set<UUID> basinIds);
}
