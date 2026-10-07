package lk.dmc.disaster.warnings.service;

import java.time.Clock;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import lk.dmc.disaster.warnings.entity.Hazard;
import lk.dmc.disaster.warnings.entity.HazardArea;
import lk.dmc.disaster.warnings.entity.HazardStatus;
import lk.dmc.disaster.warnings.integration.AreaReference;
import lk.dmc.disaster.warnings.integration.HazardTypeDirectory;
import lk.dmc.disaster.warnings.repository.HazardRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The write side of hazard assessment: record a hazard and decide to keep monitoring it or resolve
 * it. Issuing a warning is {@link WarningPublicationService}'s job. Reading hazards is {@link
 * HazardQueryService}'s.
 */
@Slf4j
@Service
public class HazardAssessmentService {

  /** The only statuses an officer sets directly. WARNED happens by issuing a warning. */
  private static final Set<HazardStatus> OFFICER_STATUSES =
      Set.of(HazardStatus.MONITORING, HazardStatus.RESOLVED);

  private final HazardRepository hazards;
  private final HazardTypeDirectory hazardTypes;
  private final AreaReference areas;
  private final Clock clock;

  public HazardAssessmentService(
      HazardRepository hazards, HazardTypeDirectory hazardTypes, AreaReference areas, Clock clock) {
    this.hazards = hazards;
    this.hazardTypes = hazardTypes;
    this.areas = areas;
    this.clock = clock;
  }

  /**
   * Records a hazard, UNDER_ASSESSMENT, with source MANUAL.
   *
   * @throws AppException VALIDATION_ERROR for an unknown type, district or basin, or for a bad
   *     severity or description
   */
  @Transactional
  public Hazard create(CreateHazardCommand command) {
    requireKnownType(command.hazardTypeId());
    requireKnownAreas(command.area());
    Hazard hazard =
        hazards.save(
            Hazard.manual(
                command.hazardTypeId(),
                command.severity(),
                command.area(),
                command.description(),
                command.eventId(),
                clock.instant()));
    log.info("Hazard {} recorded manually, severity {}", hazard.getId(), hazard.getSeverity());
    return hazard;
  }

  /**
   * Keeps a hazard under monitoring or resolves it.
   *
   * @throws AppException VALIDATION_ERROR for any other status; NOT_FOUND; INVALID_STATE_TRANSITION
   *     when the change is not allowed
   */
  @Transactional
  public Hazard setStatus(UUID hazardId, HazardStatus status) {
    if (!OFFICER_STATUSES.contains(status)) {
      throw new AppException(
          ErrorCode.VALIDATION_ERROR,
          "Status must be MONITORING or RESOLVED.",
          Map.of("field", "status"));
    }
    Hazard hazard =
        hazards
            .findById(hazardId)
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Hazard not found."));
    hazard.assessAs(status);
    log.info("Hazard {} set to {}", hazardId, status);
    return hazard;
  }

  /**
   * Sets the severity the officer judges right, up or down. New verified reports may raise it again
   * later.
   *
   * @throws AppException VALIDATION_ERROR outside 1 to 5; NOT_FOUND; INVALID_STATE_TRANSITION when
   *     the hazard is resolved
   */
  @Transactional
  public Hazard setSeverity(UUID hazardId, int severity) {
    Hazard hazard =
        hazards
            .findById(hazardId)
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Hazard not found."));
    hazard.setSeverity(severity);
    log.info("Hazard {} severity set to {} by the officer", hazardId, severity);
    return hazard;
  }

  private void requireKnownType(UUID hazardTypeId) {
    if (!hazardTypes.codesById().containsKey(hazardTypeId)) {
      throw new AppException(
          ErrorCode.VALIDATION_ERROR, "Unknown hazard type.", Map.of("field", "hazardTypeId"));
    }
  }

  private void requireKnownAreas(HazardArea area) {
    if (area.districtId() != null && !areas.districtExists(area.districtId())) {
      throw new AppException(
          ErrorCode.VALIDATION_ERROR, "Unknown district.", Map.of("field", "districtId"));
    }
    if (area.riverBasinId() != null && !areas.riverBasinExists(area.riverBasinId())) {
      throw new AppException(
          ErrorCode.VALIDATION_ERROR, "Unknown river basin.", Map.of("field", "riverBasinId"));
    }
  }
}
