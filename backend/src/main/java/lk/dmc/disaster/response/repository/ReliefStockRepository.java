package lk.dmc.disaster.response.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lk.dmc.disaster.response.entity.ReliefStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReliefStockRepository extends JpaRepository<ReliefStock, UUID> {

  List<ReliefStock> findByDistrictId(UUID districtId);

  List<ReliefStock> findByItemId(UUID itemId);

  Optional<ReliefStock> findByItemIdAndOrganisationIdAndDistrictId(
      UUID itemId, UUID organisationId, UUID districtId);

  @Modifying
  @Query("UPDATE ReliefStock s SET s.quantityAvailable = s.quantityAvailable + :amount, s.updatedAt = CURRENT_TIMESTAMP")
  int incrementAllStockQuantities(@Param("amount") int amount);
}
