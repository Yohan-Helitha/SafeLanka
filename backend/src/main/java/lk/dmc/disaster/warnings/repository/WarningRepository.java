package lk.dmc.disaster.warnings.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.warnings.entity.Warning;
import lk.dmc.disaster.warnings.entity.WarningStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Warnings. The filtered, paged list uses {@link WarningSpecifications}. */
public interface WarningRepository
    extends JpaRepository<Warning, UUID>, JpaSpecificationExecutor<Warning> {

  /** The hazard's warnings, newest first. */
  List<Warning> findByHazardIdOrderByIssuedAtDesc(UUID hazardId);

  List<Warning> findByHazardIdAndStatus(UUID hazardId, WarningStatus status);

  /**
   * ACTIVE warnings that target any of the given districts or any of the given river basins. Either
   * collection may be empty. Ordering by level is left to the caller, which knows the level rank.
   */
  @Query(
      """
      select distinct w from Warning w join w.targetAreas a
      where w.status = lk.dmc.disaster.warnings.entity.WarningStatus.ACTIVE
        and (a.districtId in :districtIds or a.riverBasinId in :basinIds)
      """)
  List<Warning> findActiveCovering(
      @Param("districtIds") Collection<UUID> districtIds,
      @Param("basinIds") Collection<UUID> basinIds);
}
