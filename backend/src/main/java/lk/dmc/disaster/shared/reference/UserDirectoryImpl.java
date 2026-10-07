package lk.dmc.disaster.shared.reference;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.shared.domain.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserDirectoryImpl implements UserDirectory {

    private final AppUserRepository appUserRepository;

    public UserDirectoryImpl(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    public Optional<UserView> findById(UUID id) {
        return appUserRepository.findById(id).map(UserView::new);
    }

    @Override
    public List<CitizenContact> findCitizensInAreas(Set<UUID> districtIds, Set<UUID> basinIds) {
        return appUserRepository.findCitizensInAreas(districtIds, basinIds, Set.of(Role.CITIZEN, Role.VOLUNTEER))
                .stream().map(CitizenContact::new).toList();
    }

    @Override
    public long countCitizensInAreas(Set<UUID> districtIds, Set<UUID> basinIds) {
        return appUserRepository.countCitizensInAreas(districtIds, basinIds, Set.of(Role.CITIZEN, Role.VOLUNTEER));
    }
}
