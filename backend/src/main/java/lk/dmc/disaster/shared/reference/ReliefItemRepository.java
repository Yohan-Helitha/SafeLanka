package lk.dmc.disaster.shared.reference;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ReliefItemRepository extends JpaRepository<ReliefItem, UUID> {}
