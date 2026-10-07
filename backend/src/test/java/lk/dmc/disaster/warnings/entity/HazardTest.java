package lk.dmc.disaster.warnings.entity;

import static lk.dmc.disaster.warnings.entity.EntityFixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import org.junit.jupiter.api.Test;

class HazardTest {

  private static final UUID TYPE = UUID.randomUUID();
  private static final HazardArea AREA = new HazardArea(UUID.randomUUID(), null);
  private static final String DESCRIPTION = "Kelani river is rising near Hanwella.";

  private static Hazard manual() {
    return Hazard.manual(TYPE, 3, AREA, DESCRIPTION, null, NOW);
  }

  @Test
  void manual_validInput_startsUnderAssessment() {
    UUID event = UUID.randomUUID();

    Hazard hazard = Hazard.manual(TYPE, 4, AREA, DESCRIPTION, event, NOW);

    assertThat(hazard.getStatus()).isEqualTo(HazardStatus.UNDER_ASSESSMENT);
    assertThat(hazard.getSource()).isEqualTo(HazardSource.MANUAL);
    assertThat(hazard.getSeverity()).isEqualTo(4);
    assertThat(hazard.getEventId()).isEqualTo(event);
    assertThat(hazard.getDistrictId()).isEqualTo(AREA.districtId());
    assertThat(hazard.getDetectedAt()).isEqualTo(NOW);
    assertThat(hazard.isOpen()).isTrue();
  }

  @Test
  void manual_severityOutOfRange_isValidationError() {
    assertThatThrownBy(() -> Hazard.manual(TYPE, 6, AREA, DESCRIPTION, null, NOW))
        .isInstanceOfSatisfying(
            AppException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.VALIDATION_ERROR));
  }

  @Test
  void manual_shortDescription_isValidationError() {
    assertThatThrownBy(() -> Hazard.manual(TYPE, 3, AREA, "too short", null, NOW))
        .isInstanceOfSatisfying(
            AppException.class, e -> assertThat(e.details()).containsEntry("field", "description"));
  }

  @Test
  void fromReport_usesReportSeverityAndSource() {
    Hazard hazard = Hazard.fromReport(TYPE, AREA, DESCRIPTION, NOW);

    assertThat(hazard.getSource()).isEqualTo(HazardSource.REPORT);
    assertThat(hazard.getSeverity()).isEqualTo(WarningRules.REPORT_HAZARD_SEVERITY);
  }

  @Test
  void fromSensor_takesAreaFromSensorAndLinksIt() {
    Sensor sensor = EntityFixtures.sensor("1.20", "2.50");

    Hazard hazard = Hazard.fromSensor(sensor, TYPE, DESCRIPTION, NOW);

    assertThat(hazard.getSource()).isEqualTo(HazardSource.SENSOR);
    assertThat(hazard.getSensorId()).isEqualTo(sensor.getId());
    assertThat(hazard.getDistrictId()).isEqualTo(sensor.getDistrictId());
    assertThat(hazard.getRiverBasinId()).isEqualTo(sensor.getRiverBasinId());
    assertThat(hazard.getSeverity()).isEqualTo(WarningRules.SENSOR_HAZARD_SEVERITY);
  }

  @Test
  void assessAs_allowedChange_updatesStatus() {
    Hazard hazard = manual();

    hazard.assessAs(HazardStatus.MONITORING);

    assertThat(hazard.getStatus()).isEqualTo(HazardStatus.MONITORING);
  }

  @Test
  void assessAs_fromResolved_isInvalidTransition() {
    Hazard hazard = manual();
    hazard.assessAs(HazardStatus.RESOLVED);

    assertThat(hazard.isOpen()).isFalse();
    assertThatThrownBy(() -> hazard.assessAs(HazardStatus.MONITORING))
        .isInstanceOfSatisfying(
            AppException.class,
            e -> assertThat(e.code()).isEqualTo(ErrorCode.INVALID_STATE_TRANSITION));
  }

  @Test
  void markWarned_calledTwice_staysWarned() {
    Hazard hazard = manual();

    hazard.markWarned();
    hazard.markWarned();

    assertThat(hazard.getStatus()).isEqualTo(HazardStatus.WARNED);
  }

  @Test
  void markWarned_resolvedHazard_isInvalidTransition() {
    Hazard hazard = manual();
    hazard.assessAs(HazardStatus.RESOLVED);

    assertThatThrownBy(hazard::markWarned).isInstanceOf(AppException.class);
  }

  @Test
  void raiseSeverity_higherValue_raises_lowerValue_keepsCurrent() {
    Hazard hazard = manual();

    hazard.raiseSeverity(4);
    hazard.raiseSeverity(2);

    assertThat(hazard.getSeverity()).isEqualTo(4);
    assertThatThrownBy(() -> hazard.raiseSeverity(9)).isInstanceOf(AppException.class);
  }

  @Test
  void setSeverity_officerCanRaiseAndLower() {
    Hazard hazard = manual();

    hazard.setSeverity(5);
    assertThat(hazard.getSeverity()).isEqualTo(5);

    hazard.setSeverity(1);
    assertThat(hazard.getSeverity()).isEqualTo(1);
  }

  @Test
  void setSeverity_outsideOneToFive_isValidationError() {
    Hazard hazard = manual();

    assertThatThrownBy(() -> hazard.setSeverity(0))
        .isInstanceOfSatisfying(
            AppException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    assertThatThrownBy(() -> hazard.setSeverity(6)).isInstanceOf(AppException.class);
  }

  @Test
  void setSeverity_resolvedHazard_isInvalidTransition() {
    Hazard hazard = manual();
    hazard.assessAs(HazardStatus.RESOLVED);

    assertThatThrownBy(() -> hazard.setSeverity(4))
        .isInstanceOfSatisfying(
            AppException.class,
            e -> assertThat(e.code()).isEqualTo(ErrorCode.INVALID_STATE_TRANSITION));
  }

  @Test
  void evidenceLink_exposesBothIds() {
    UUID hazardId = UUID.randomUUID();
    UUID reportId = UUID.randomUUID();

    HazardEvidence evidence = HazardEvidence.link(hazardId, reportId, NOW);

    assertThat(evidence.hazardId()).isEqualTo(hazardId);
    assertThat(evidence.reportId()).isEqualTo(reportId);
    assertThat(evidence.getLinkedAt()).isEqualTo(NOW);
  }
}
