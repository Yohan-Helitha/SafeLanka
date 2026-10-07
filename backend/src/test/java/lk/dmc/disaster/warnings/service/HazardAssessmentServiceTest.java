package lk.dmc.disaster.warnings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import lk.dmc.disaster.warnings.entity.Hazard;
import lk.dmc.disaster.warnings.entity.HazardArea;
import lk.dmc.disaster.warnings.entity.HazardSource;
import lk.dmc.disaster.warnings.entity.HazardStatus;
import lk.dmc.disaster.warnings.integration.AreaReference;
import lk.dmc.disaster.warnings.integration.HazardTypeDirectory;
import lk.dmc.disaster.warnings.repository.HazardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HazardAssessmentServiceTest {

  private static final UUID TYPE = UUID.randomUUID();
  private static final UUID DISTRICT = UUID.randomUUID();
  private static final UUID BASIN = UUID.randomUUID();
  private static final String DESCRIPTION = "Kelani river is rising near Hanwella.";

  @Mock private HazardRepository hazards;
  @Mock private HazardTypeDirectory hazardTypes;
  @Mock private AreaReference areas;

  private HazardAssessmentService service;

  @BeforeEach
  void setUp() {
    service =
        new HazardAssessmentService(
            hazards, hazardTypes, areas, Clock.fixed(ServiceFixtures.NOW, ZoneOffset.UTC));
  }

  private static void assertCode(Throwable thrown, ErrorCode expected) {
    assertThat(thrown)
        .isInstanceOfSatisfying(AppException.class, e -> assertThat(e.code()).isEqualTo(expected));
  }

  private void knownTypeAndSaveEcho() {
    when(hazardTypes.codesById()).thenReturn(Map.of(TYPE, "FLOOD"));
    when(hazards.save(any(Hazard.class))).thenAnswer(call -> call.getArgument(0));
  }

  // ---- create ------------------------------------------------------------------------------

  @Test
  void create_validCommand_savesAManualHazardUnderAssessment() {
    knownTypeAndSaveEcho();
    when(areas.districtExists(DISTRICT)).thenReturn(true);

    Hazard hazard =
        service.create(
            new CreateHazardCommand(TYPE, 4, new HazardArea(DISTRICT, null), DESCRIPTION, null));

    assertThat(hazard.getSource()).isEqualTo(HazardSource.MANUAL);
    assertThat(hazard.getStatus()).isEqualTo(HazardStatus.UNDER_ASSESSMENT);
    assertThat(hazard.getSeverity()).isEqualTo(4);
    assertThat(hazard.getDetectedAt()).isEqualTo(ServiceFixtures.NOW);
  }

  @Test
  void create_basinOnly_checksOnlyTheBasin() {
    knownTypeAndSaveEcho();
    when(areas.riverBasinExists(BASIN)).thenReturn(true);

    Hazard hazard =
        service.create(
            new CreateHazardCommand(TYPE, 3, new HazardArea(null, BASIN), DESCRIPTION, null));

    assertThat(hazard.getRiverBasinId()).isEqualTo(BASIN);
    verify(areas, never()).districtExists(any());
  }

  @Test
  void create_unknownHazardType_isValidationErrorAndNothingSaved() {
    when(hazardTypes.codesById()).thenReturn(Map.of());

    assertThatThrownBy(
            () ->
                service.create(
                    new CreateHazardCommand(
                        TYPE, 3, new HazardArea(DISTRICT, null), DESCRIPTION, null)))
        .satisfies(e -> assertCode(e, ErrorCode.VALIDATION_ERROR));
    verify(hazards, never()).save(any());
  }

  @Test
  void create_unknownDistrictOrBasin_isValidationError() {
    when(hazardTypes.codesById()).thenReturn(Map.of(TYPE, "FLOOD"));
    when(areas.districtExists(DISTRICT)).thenReturn(false);

    assertThatThrownBy(
            () ->
                service.create(
                    new CreateHazardCommand(
                        TYPE, 3, new HazardArea(DISTRICT, null), DESCRIPTION, null)))
        .isInstanceOfSatisfying(
            AppException.class, e -> assertThat(e.details()).containsEntry("field", "districtId"));

    when(areas.riverBasinExists(BASIN)).thenReturn(false);
    assertThatThrownBy(
            () ->
                service.create(
                    new CreateHazardCommand(
                        TYPE, 3, new HazardArea(null, BASIN), DESCRIPTION, null)))
        .isInstanceOfSatisfying(
            AppException.class,
            e -> assertThat(e.details()).containsEntry("field", "riverBasinId"));
  }

  @Test
  void create_severityOutOfRange_isValidationError() {
    when(hazardTypes.codesById()).thenReturn(Map.of(TYPE, "FLOOD"));
    when(areas.districtExists(DISTRICT)).thenReturn(true);

    assertThatThrownBy(
            () ->
                service.create(
                    new CreateHazardCommand(
                        TYPE, 6, new HazardArea(DISTRICT, null), DESCRIPTION, null)))
        .satisfies(e -> assertCode(e, ErrorCode.VALIDATION_ERROR));
  }

  // ---- setStatus ---------------------------------------------------------------------------

  @Test
  void setStatus_monitoringAndResolved_areAccepted() {
    Hazard first = ServiceFixtures.manualHazard(DISTRICT);
    Hazard second = ServiceFixtures.manualHazard(DISTRICT);
    when(hazards.findById(first.getId())).thenReturn(Optional.of(first));
    when(hazards.findById(second.getId())).thenReturn(Optional.of(second));

    assertThat(service.setStatus(first.getId(), HazardStatus.MONITORING).getStatus())
        .isEqualTo(HazardStatus.MONITORING);
    assertThat(service.setStatus(second.getId(), HazardStatus.RESOLVED).getStatus())
        .isEqualTo(HazardStatus.RESOLVED);
  }

  @Test
  void setStatus_warnedOrUnderAssessment_isValidationError() {
    UUID id = UUID.randomUUID();

    assertThatThrownBy(() -> service.setStatus(id, HazardStatus.WARNED))
        .satisfies(e -> assertCode(e, ErrorCode.VALIDATION_ERROR));
    assertThatThrownBy(() -> service.setStatus(id, HazardStatus.UNDER_ASSESSMENT))
        .satisfies(e -> assertCode(e, ErrorCode.VALIDATION_ERROR));
    verify(hazards, never()).findById(any());
  }

  @Test
  void setStatus_unknownHazard_isNotFound() {
    UUID id = UUID.randomUUID();
    when(hazards.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.setStatus(id, HazardStatus.RESOLVED))
        .satisfies(e -> assertCode(e, ErrorCode.NOT_FOUND));
  }

  @Test
  void setStatus_resolvedHazard_cannotBeReopened() {
    Hazard hazard = ServiceFixtures.manualHazard(DISTRICT);
    hazard.assessAs(HazardStatus.RESOLVED);
    when(hazards.findById(hazard.getId())).thenReturn(Optional.of(hazard));

    assertThatThrownBy(() -> service.setStatus(hazard.getId(), HazardStatus.MONITORING))
        .satisfies(e -> assertCode(e, ErrorCode.INVALID_STATE_TRANSITION));
  }
}
