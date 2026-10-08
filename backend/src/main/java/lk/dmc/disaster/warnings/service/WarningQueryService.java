package lk.dmc.disaster.warnings.service;

import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import lk.dmc.disaster.warnings.entity.Warning;
import lk.dmc.disaster.warnings.entity.WarningStatus;
import lk.dmc.disaster.warnings.entity.WarningTarget;
import lk.dmc.disaster.warnings.repository.WarningRepository;
import lk.dmc.disaster.warnings.repository.WarningSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The read side of warnings: one warning or a page of them, with delivery totals. */
@Service
public class WarningQueryService {

  private final WarningRepository warnings;
  private final AudienceService audience;
  private final DeliveryQueryService deliveries;

  public WarningQueryService(
      WarningRepository warnings, AudienceService audience, DeliveryQueryService deliveries) {
    this.warnings = warnings;
    this.audience = audience;
    this.deliveries = deliveries;
  }

  /**
   * One warning.
   *
   * @throws AppException NOT_FOUND for an unknown warning
   */
  @Transactional(readOnly = true)
  public WarningView get(UUID warningId) {
    Warning warning =
        warnings
            .findById(warningId)
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Warning not found."));
    return viewOf(warning);
  }

  /**
   * A page of warnings, newest first when the pageable says so.
   *
   * @param status only this status, or null for all
   * @param eventId only this event, or null for all
   */
  @Transactional(readOnly = true)
  public Page<WarningView> list(WarningStatus status, UUID eventId, Pageable pageable) {
    return warnings
        .findAll(
            WarningSpecifications.withStatus(status).and(WarningSpecifications.forEvent(eventId)),
            pageable)
        .map(this::viewOf);
  }

  /** All warnings issued for the hazard, newest first. */
  @Transactional(readOnly = true)
  public List<WarningView> listForHazard(UUID hazardId) {
    return warnings.findByHazardIdOrderByIssuedAtDesc(hazardId).stream().map(this::viewOf).toList();
  }

  private WarningView viewOf(Warning warning) {
    WarningTarget target = warning.target();
    return new WarningView(
        warning,
        target,
        warning.evidenceReportIds(),
        audience.resolveDistricts(AudienceSelection.of(target)),
        deliveries.outcomeOf(warning));
  }
}
