package lk.dmc.disaster.response.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lk.dmc.disaster.response.entity.ReliefDistribution;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReliefDistributionRepository extends JpaRepository<ReliefDistribution, UUID> {

  List<ReliefDistribution> findByAllocationId(UUID allocationId);

  Optional<ReliefDistribution> findByClientRef(UUID clientRef);
}
