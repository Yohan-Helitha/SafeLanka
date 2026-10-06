package lk.dmc.disaster.response.repository;

import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.response.entity.OccupancyLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OccupancyLogRepository extends JpaRepository<OccupancyLog, UUID> {

  List<OccupancyLog> findByShelterIdOrderByRecordedAtDesc(UUID shelterId);
}
