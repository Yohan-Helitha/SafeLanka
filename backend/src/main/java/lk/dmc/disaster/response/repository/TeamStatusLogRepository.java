package lk.dmc.disaster.response.repository;

import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.response.entity.TeamStatusLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamStatusLogRepository extends JpaRepository<TeamStatusLog, UUID> {

  List<TeamStatusLog> findByTeamIdOrderByChangedAtDesc(UUID teamId);
}
