package lk.dmc.disaster.reports.repository;

import java.util.Optional;
import java.util.UUID;
import lk.dmc.disaster.reports.entity.ReportPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportPhotoRepository extends JpaRepository<ReportPhoto, UUID> {

  Optional<ReportPhoto> findByReportId(UUID reportId);
}
