package lk.dmc.disaster.response.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lk.dmc.disaster.response.dto.request.AssignmentRequest;
import lk.dmc.disaster.response.entity.RescueTeam;
import lk.dmc.disaster.response.entity.TeamType;
import lk.dmc.disaster.response.repository.RescueTeamRepository;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NewAssignmentScreenValidatorTest {

  @Mock private RescueTeamRepository teams;

  private NewAssignmentScreenValidator validator;

  @BeforeEach
  void setUp() {
    validator = new NewAssignmentScreenValidator(teams);
  }

  @Test
  void validateCreation_nullRequest_throwsValidationError() {
    assertThatThrownBy(() -> validator.validateCreation(null))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.VALIDATION_ERROR);
  }

  @Test
  void validateCreation_missingEventId_throwsValidationError() {
    AssignmentRequest req =
        new AssignmentRequest(
            null, null, null, 6.9, 79.8, "Location", "Task description", (short) 1, 5, null, null);

    assertThatThrownBy(() -> validator.validateCreation(req))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.VALIDATION_ERROR);
  }

  @Test
  void validateCreation_invalidLocationText_throwsValidationError() {
    AssignmentRequest reqNullLoc =
        new AssignmentRequest(
            UUID.randomUUID(),
            null,
            null,
            6.9,
            79.8,
            null,
            "Task description",
            (short) 1,
            5,
            null,
            null);
    assertThatThrownBy(() -> validator.validateCreation(reqNullLoc))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.VALIDATION_ERROR);

    AssignmentRequest reqShortLoc =
        new AssignmentRequest(
            UUID.randomUUID(),
            null,
            null,
            6.9,
            79.8,
            "AB",
            "Task description",
            (short) 1,
            5,
            null,
            null);
    assertThatThrownBy(() -> validator.validateCreation(reqShortLoc))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.VALIDATION_ERROR);
  }

  @Test
  void validateCreation_invalidTask_throwsValidationError() {
    AssignmentRequest reqNullTask =
        new AssignmentRequest(
            UUID.randomUUID(), null, null, 6.9, 79.8, "Grandpass", null, (short) 1, 5, null, null);
    assertThatThrownBy(() -> validator.validateCreation(reqNullTask))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.VALIDATION_ERROR);

    AssignmentRequest reqShortTask =
        new AssignmentRequest(
            UUID.randomUUID(),
            null,
            null,
            6.9,
            79.8,
            "Grandpass",
            "Help",
            (short) 1,
            5,
            null,
            null);
    assertThatThrownBy(() -> validator.validateCreation(reqShortTask))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.VALIDATION_ERROR);

    AssignmentRequest reqLongTask =
        new AssignmentRequest(
            UUID.randomUUID(),
            null,
            null,
            6.9,
            79.8,
            "Grandpass",
            "A".repeat(501),
            (short) 1,
            5,
            null,
            null);
    assertThatThrownBy(() -> validator.validateCreation(reqLongTask))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.VALIDATION_ERROR);
  }

  @Test
  void validateCreation_invalidPriority_throwsValidationError() {
    AssignmentRequest reqLow =
        new AssignmentRequest(
            UUID.randomUUID(),
            null,
            null,
            6.9,
            79.8,
            "Grandpass",
            "Valid task description",
            (short) 0,
            5,
            null,
            null);
    assertThatThrownBy(() -> validator.validateCreation(reqLow))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.VALIDATION_ERROR);

    AssignmentRequest reqHigh =
        new AssignmentRequest(
            UUID.randomUUID(),
            null,
            null,
            6.9,
            79.8,
            "Grandpass",
            "Valid task description",
            (short) 4,
            5,
            null,
            null);
    assertThatThrownBy(() -> validator.validateCreation(reqHigh))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.VALIDATION_ERROR);
  }

  @Test
  void validateCreation_negativePeopleEstimated_throwsValidationError() {
    AssignmentRequest reqNeg =
        new AssignmentRequest(
            UUID.randomUUID(),
            null,
            null,
            6.9,
            79.8,
            "Grandpass",
            "Valid task description",
            (short) 2,
            -1,
            null,
            null);
    assertThatThrownBy(() -> validator.validateCreation(reqNeg))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.VALIDATION_ERROR);
  }

  @Test
  void validateCreation_validWithTeam_succeeds() {
    UUID teamId = UUID.randomUUID();
    RescueTeam readyTeam =
        RescueTeam.create("Team Ready", UUID.randomUUID(), UUID.randomUUID(), TeamType.BOAT, 5);
    when(teams.findById(teamId)).thenReturn(Optional.of(readyTeam));

    AssignmentRequest req =
        new AssignmentRequest(
            UUID.randomUUID(),
            null,
            null,
            6.9,
            79.8,
            "Valid location",
            "Valid task description",
            (short) 2,
            10,
            null,
            teamId);

    assertThatCode(() -> validator.validateCreation(req)).doesNotThrowAnyException();
  }

  @Test
  void validateTeamAvailability_teamNotFound_throwsNotFound() {
    UUID teamId = UUID.randomUUID();
    when(teams.findById(teamId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> validator.validateTeamAvailability(teamId, null))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.NOT_FOUND);
  }

  @Test
  void validateTeamAvailability_teamUnavailable_withoutDistrict_throwsTeamNotAvailable() {
    UUID teamId = UUID.randomUUID();
    RescueTeam busyTeam =
        RescueTeam.create("Team Busy", UUID.randomUUID(), UUID.randomUUID(), TeamType.BOAT, 5);
    busyTeam.markActive();
    when(teams.findById(teamId)).thenReturn(Optional.of(busyTeam));

    assertThatThrownBy(() -> validator.validateTeamAvailability(teamId, null))
        .isInstanceOf(AppException.class)
        .satisfies(
            e -> {
              AppException ae = (AppException) e;
              assertThat(ae.code()).isEqualTo(ErrorCode.TEAM_NOT_AVAILABLE);
              assertThat(ae.details()).containsKey("alternatives");
              assertThat((List<?>) ae.details().get("alternatives")).isEmpty();
            });

    UUID districtId = UUID.randomUUID();
    RescueTeam altTeam =
        RescueTeam.create("Team Alt", UUID.randomUUID(), districtId, TeamType.BOAT, 5);
    when(teams.findByDistrictIdAndStatus(
            districtId, lk.dmc.disaster.response.entity.RescueTeamStatus.AVAILABLE))
        .thenReturn(List.of(altTeam));

    assertThatThrownBy(() -> validator.validateTeamAvailability(teamId, districtId))
        .isInstanceOf(AppException.class)
        .satisfies(
            e -> {
              AppException ae = (AppException) e;
              assertThat(ae.code()).isEqualTo(ErrorCode.TEAM_NOT_AVAILABLE);
              assertThat((List<?>) ae.details().get("alternatives")).hasSize(1);
            });
  }
}
