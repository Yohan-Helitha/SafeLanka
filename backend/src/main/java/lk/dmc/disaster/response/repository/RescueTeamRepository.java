package lk.dmc.disaster.response.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lk.dmc.disaster.response.entity.RescueTeam;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RescueTeamRepository extends JpaRepository<RescueTeam, UUID> {

  List<RescueTeam> findByDistrictIdAndStatus(
      UUID districtId, lk.dmc.disaster.response.entity.RescueTeamStatus status);

  List<RescueTeam> findByDistrictId(UUID districtId);

  Optional<RescueTeam> findByIdAndStatusNotIn(UUID id, List<String> excludedStatuses);
}
