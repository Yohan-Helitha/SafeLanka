package lk.dmc.disaster.reports.repository;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lk.dmc.disaster.reports.entity.HazardReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Storage of ground reports; queue filters go through {@link ReportSpecifications}. */
public interface HazardReportRepository
    extends JpaRepository<HazardReport, UUID>, JpaSpecificationExecutor<HazardReport> {

  /** The report a phone already synced under this offline key, if any. */
  Optional<HazardReport> findByClientRef(UUID clientRef);

  /** Loads a report and locks its row until the transaction ends, for decisions. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<HazardReport> findWithLockById(UUID id);

  Page<HazardReport> findByReporterIdOrderByCapturedAtDesc(UUID reporterId, Pageable pageable);

  /**
   * Reports that could be the same event: same hazard type, not rejected, with GPS, captured inside
   * the window. The exact distance check is done by the caller.
   */
  @Query(
      """
      select r from HazardReport r
      where r.hazardTypeId = :hazardTypeId
        and r.id <> :excludeId
        and r.status <> lk.dmc.disaster.reports.entity.ReportStatus.REJECTED
        and r.latitude is not null and r.longitude is not null
        and r.capturedAt between :from and :to
      """)
  List<HazardReport> findDuplicateCandidates(
      @Param("hazardTypeId") UUID hazardTypeId,
      @Param("excludeId") UUID excludeId,
      @Param("from") Instant from,
      @Param("to") Instant to);
}
