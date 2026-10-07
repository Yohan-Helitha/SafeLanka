package lk.dmc.disaster.response.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lk.dmc.disaster.response.entity.ReliefStock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReliefStockRepository extends JpaRepository<ReliefStock, UUID> {

  List<ReliefStock> findByDistrictId(UUID districtId);

  List<ReliefStock> findByItemId(UUID itemId);

  Optional<ReliefStock> findByItemIdAndOrganisationIdAndDistrictId(
      UUID itemId, UUID organisationId, UUID districtId);
}
