package lk.dmc.disaster.reports.repository;

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

  private static Specification<HazardReport> equalTo(String field, Object value) {
    return value == null
        ? Specification.unrestricted()
        : (root, query, cb) -> cb.equal(root.get(field), value);
  }
}
