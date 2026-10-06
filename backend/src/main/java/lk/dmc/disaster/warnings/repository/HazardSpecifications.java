package lk.dmc.disaster.warnings.repository;

import java.util.Collection;
import java.util.UUID;
import lk.dmc.disaster.warnings.entity.Hazard;
import lk.dmc.disaster.warnings.entity.HazardStatus;
import org.springframework.data.jpa.domain.Specification;

/** Optional filters for the hazard list. A null filter matches everything. */
public final class HazardSpecifications {

  private HazardSpecifications() {}

  public static Specification<Hazard> statusIn(Collection<HazardStatus> statuses) {
    return (root, query, cb) ->
        statuses == null || statuses.isEmpty() ? cb.conjunction() : root.get("status").in(statuses);
  }

  public static Specification<Hazard> ofType(UUID hazardTypeId) {
    return (root, query, cb) ->
        hazardTypeId == null ? cb.conjunction() : cb.equal(root.get("hazardTypeId"), hazardTypeId);
  }

  public static Specification<Hazard> inDistrict(UUID districtId) {
    return (root, query, cb) ->
        districtId == null ? cb.conjunction() : cb.equal(root.get("districtId"), districtId);
  }
}
