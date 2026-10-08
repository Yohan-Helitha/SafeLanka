package lk.dmc.disaster.shared.reference;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface DisasterEventRepository extends JpaRepository<DisasterEvent, UUID> {
    List<DisasterEvent> findByStatus(EventStatus status);
}
