package lk.dmc.disaster.warnings.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lk.dmc.disaster.warnings.entity.Hazard;
import lk.dmc.disaster.warnings.entity.HazardStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Hazards. The filtered list uses {@link HazardSpecifications}. */
public interface HazardRepository
    extends JpaRepository<Hazard, UUID>, JpaSpecificationExecutor<Hazard> {

  /** The newest hazard of the sensor that is not in the given status, if any. */
  Optional<Hazard> findFirstBySensorIdAndStatusNotOrderByDetectedAtDesc(
      UUID sensorId, HazardStatus status);

  /** The hazard a sensor opened that is still open (not RESOLVED), if any. */
  default Optional<Hazard> findOpenForSensor(UUID sensorId) {
    return findFirstBySensorIdAndStatusNotOrderByDetectedAtDesc(sensorId, HazardStatus.RESOLVED);
  }

  @Query(
      """
      select h from Hazard h
      where h.status <> lk.dmc.disaster.warnings.entity.HazardStatus.RESOLVED
        and h.hazardTypeId = :typeId
        and (h.districtId = :districtId or h.riverBasinId in :basinIds)
      order by h.detectedAt desc
      """)
  List<Hazard> findOpenMatching(
      @Param("typeId") UUID typeId,
      @Param("districtId") UUID districtId,
      @Param("basinIds") Collection<UUID> basinIds,
      Pageable page);

  /**
   * The newest open hazard of this type in the district, or in any of the basins. Used to decide
   * where a newly verified report belongs.
   */
  default Optional<Hazard> findNewestOpenMatching(
      UUID typeId, UUID districtId, Collection<UUID> basinIds) {
    return findOpenMatching(typeId, districtId, basinIds, PageRequest.of(0, 1)).stream()
        .findFirst();
  }
}
