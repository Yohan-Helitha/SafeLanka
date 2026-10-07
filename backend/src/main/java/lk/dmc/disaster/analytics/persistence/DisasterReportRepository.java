package lk.dmc.disaster.analytics.persistence;

import java.util.UUID;
import lk.dmc.disaster.analytics.domain.DisasterReport;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DisasterReportRepository extends JpaRepository<DisasterReport, UUID> {
    List<DisasterReport> findByEventIdOrderByGeneratedAtDesc(UUID eventId);
}
