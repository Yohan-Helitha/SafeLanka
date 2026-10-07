package lk.dmc.disaster.response.repository;

import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.response.entity.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, UUID> {

  List<ActivityLog> findByDistrictIdOrderByOccurredAtDesc(UUID districtId);
}
