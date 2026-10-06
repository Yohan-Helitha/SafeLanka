package lk.dmc.disaster.warnings.repository;

import java.util.UUID;
import lk.dmc.disaster.warnings.entity.Warning;
import lk.dmc.disaster.warnings.entity.WarningStatus;
import org.springframework.data.jpa.domain.Specification;

/** Optional filters for the warning list. A null filter matches everything. */
public final class WarningSpecifications {

  private WarningSpecifications() {}

  public static Specification<Warning> withStatus(WarningStatus status) {
    return (root, query, cb) ->
        status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
  }

  public static Specification<Warning> forEvent(UUID eventId) {
    return (root, query, cb) ->
        eventId == null ? cb.conjunction() : cb.equal(root.get("eventId"), eventId);
  }
}
