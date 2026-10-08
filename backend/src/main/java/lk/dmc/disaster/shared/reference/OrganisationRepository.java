package lk.dmc.disaster.shared.reference;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface OrganisationRepository extends JpaRepository<Organisation, UUID> {
    List<Organisation> findByType(OrganisationType type);
}
