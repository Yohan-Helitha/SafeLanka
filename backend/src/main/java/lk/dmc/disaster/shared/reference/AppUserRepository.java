package lk.dmc.disaster.shared.reference;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.shared.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {
    
    List<AppUser> findByRole(Role role);

    @Query("SELECT u FROM AppUser u WHERE u.role IN :roles AND (u.districtId IN :districtIds OR u.riverBasinId IN :basinIds)")
    List<AppUser> findCitizensInAreas(@Param("districtIds") Set<UUID> districtIds, @Param("basinIds") Set<UUID> basinIds, @Param("roles") Set<Role> roles);
    
    @Query("SELECT COUNT(u) FROM AppUser u WHERE u.role IN :roles AND (u.districtId IN :districtIds OR u.riverBasinId IN :basinIds)")
    long countCitizensInAreas(@Param("districtIds") Set<UUID> districtIds, @Param("basinIds") Set<UUID> basinIds, @Param("roles") Set<Role> roles);
}
