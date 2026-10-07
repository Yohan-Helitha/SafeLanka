package lk.dmc.disaster.reports.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lk.dmc.disaster.reports.entity.ReportPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportPhotoRepository extends JpaRepository<ReportPhoto, UUID> {

  Optional<ReportPhoto> findByReportId(UUID reportId);

  /** One query for a whole page of reports. */
  List<ReportPhoto> findByReportIdIn(Collection<UUID> reportIds);
}
