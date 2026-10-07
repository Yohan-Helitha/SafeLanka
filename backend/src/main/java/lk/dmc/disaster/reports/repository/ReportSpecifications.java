package lk.dmc.disaster.reports.repository;

import java.time.Instant;
import java.util.UUID;
import lk.dmc.disaster.reports.entity.HazardReport;
import lk.dmc.disaster.reports.entity.ReportStatus;
import org.springframework.data.jpa.domain.Specification;

/** Building blocks for the officer queue: every filter is optional (null means any). */
public final class ReportSpecifications {

  private ReportSpecifications() {}

  public static Specification<HazardReport> queue(
      ReportStatus status, UUID hazardTypeId, UUID districtId) {
    return Specification.<HazardReport>unrestricted()
        .and(equalTo("status", status))
        .and(equalTo("hazardTypeId", hazardTypeId))
        .and(equalTo("districtId", districtId));
  }

  /** Verified reports only; each filter is optional and {@code since} is inclusive. */
  public static Specification<HazardReport> verified(
      UUID hazardTypeId, UUID districtId, Instant since) {
    Specification<HazardReport> sinceSpec =
        since == null
            ? Specification.unrestricted()
            : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("reviewedAt"), since);
    return queue(ReportStatus.VERIFIED, hazardTypeId, districtId).and(sinceSpec);
  }

  private static Specification<HazardReport> equalTo(String field, Object value) {
    return value == null
        ? Specification.unrestricted()
        : (root, query, cb) -> cb.equal(root.get(field), value);
  }
}
