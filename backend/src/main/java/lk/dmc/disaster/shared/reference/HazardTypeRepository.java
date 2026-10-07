package lk.dmc.disaster.shared.reference;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface HazardTypeRepository extends JpaRepository<HazardType, UUID> {
    List<HazardType> findByActiveTrue();
}
