package lk.dmc.disaster.warnings.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.warnings.entity.HazardEvidence;
import lk.dmc.disaster.warnings.entity.HazardEvidenceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Verified reports linked to hazards. */
public interface HazardEvidenceRepository extends JpaRepository<HazardEvidence, HazardEvidenceId> {

  List<HazardEvidence> findByIdHazardId(UUID hazardId);

  /** True when the report is already linked to any hazard, so the listener can skip it. */
  boolean existsByIdReportId(UUID reportId);

  /** Evidence counts for many hazards in one query. Hazards with no evidence are absent. */
  @Query(
      """
      select new lk.dmc.disaster.warnings.repository.HazardEvidenceCount(e.id.hazardId, count(e))
      from HazardEvidence e
      where e.id.hazardId in :hazardIds
      group by e.id.hazardId
      """)
  List<HazardEvidenceCount> countByHazardIds(@Param("hazardIds") Collection<UUID> hazardIds);
}
