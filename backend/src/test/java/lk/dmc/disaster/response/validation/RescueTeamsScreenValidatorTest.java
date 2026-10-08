package lk.dmc.disaster.response.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lk.dmc.disaster.response.dto.request.AssignTeamRequest;
import lk.dmc.disaster.response.dto.request.CancelAssignmentRequest;
import lk.dmc.disaster.response.entity.RescueAssignment;
import lk.dmc.disaster.response.entity.RescueTeam;
import lk.dmc.disaster.response.entity.RescueTeamStatus;
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
class RescueTeamsScreenValidatorTest {

  @Mock private RescueTeamRepository teams;

  private RescueTeamsScreenValidator validator;

  @BeforeEach
  void setUp() {
    validator = new RescueTeamsScreenValidator(teams);
  }

  private RescueAssignment createAssignment() {
    return RescueAssignment.create(
        UUID.randomUUID(),
        null,
        UUID.randomUUID(),
        6.9,
        79.8,
        "Location",
        "Task",
        (short) 1,
        10,
        null);
  }

  @Test
  void validateAssignTeam_nullRequestOrTeamId_throwsValidationError() {
    RescueAssignment assignment = createAssignment();

    assertThatThrownBy(() -> validator.validateAssignTeam(assignment, null, null))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.VALIDATION_ERROR);

    assertThatThrownBy(
            () -> validator.validateAssignTeam(assignment, new AssignTeamRequest(null), null))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.VALIDATION_ERROR);
  }

  @Test
  void validateAssignTeam_terminalStatuses_throwsConflict() {
    RescueAssignment completedAssignment = createAssignment();
    completedAssignment.complete();

    assertThatThrownBy(
            () ->
                validator.validateAssignTeam(
                    completedAssignment, new AssignTeamRequest(UUID.randomUUID()), null))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.CONFLICT);

    RescueAssignment cancelledAssignment = createAssignment();
    cancelledAssignment.cancel("Cancelled");

    assertThatThrownBy(
            () ->
                validator.validateAssignTeam(
                    cancelledAssignment, new AssignTeamRequest(UUID.randomUUID()), null))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.CONFLICT);
  }

  @Test
  void validateAssignTeam_teamNotFound_throwsNotFound() {
    RescueAssignment assignment = createAssignment();
    UUID teamId = UUID.randomUUID();
    when(teams.findById(teamId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> validator.validateAssignTeam(assignment, new AssignTeamRequest(teamId), null))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.NOT_FOUND);
  }

  @Test
  void validateAssignTeam_teamUnavailable_throwsTeamNotAvailableWithAlternatives() {
    RescueAssignment assignment = createAssignment();
    UUID teamId = UUID.randomUUID();
    UUID districtId = UUID.randomUUID();

    RescueTeam busyTeam =
        RescueTeam.create("Busy 1", UUID.randomUUID(), districtId, TeamType.SEARCH, 8);
    busyTeam.markDispatched();
    when(teams.findById(teamId)).thenReturn(Optional.of(busyTeam));

    RescueTeam freeTeam =
        RescueTeam.create("Free 1", UUID.randomUUID(), districtId, TeamType.SEARCH, 8);
    when(teams.findByDistrictIdAndStatus(districtId, RescueTeamStatus.AVAILABLE))
        .thenReturn(List.of(freeTeam));

    assertThatThrownBy(
            () ->
                validator.validateAssignTeam(assignment, new AssignTeamRequest(teamId), districtId))
        .isInstanceOf(AppException.class)
        .satisfies(
            e -> {
              AppException ae = (AppException) e;
              assertThat(ae.code()).isEqualTo(ErrorCode.TEAM_NOT_AVAILABLE);
              assertThat(ae.details()).containsKey("alternatives");
            });

    // Also test districtId == null
    assertThatThrownBy(
            () -> validator.validateAssignTeam(assignment, new AssignTeamRequest(teamId), null))
        .isInstanceOf(AppException.class)
        .satisfies(
            e -> {
              AppException ae = (AppException) e;
              assertThat(ae.code()).isEqualTo(ErrorCode.TEAM_NOT_AVAILABLE);
              assertThat((List<?>) ae.details().get("alternatives")).isEmpty();
            });
  }

  @Test
  void validateAssignTeam_teamAvailable_returnsTeam() {
    RescueAssignment assignment = createAssignment();
    UUID teamId = UUID.randomUUID();
    RescueTeam freeTeam =
        RescueTeam.create("Ready 1", UUID.randomUUID(), UUID.randomUUID(), TeamType.SEARCH, 8);
    when(teams.findById(teamId)).thenReturn(Optional.of(freeTeam));

    RescueTeam result =
        validator.validateAssignTeam(assignment, new AssignTeamRequest(teamId), null);
    assertThat(result).isSameAs(freeTeam);
  }

  @Test
  void validateCancelAssignment_nullOrBlankReason_throwsValidationError() {
    RescueAssignment assignment = createAssignment();

    assertThatThrownBy(() -> validator.validateCancelAssignment(assignment, null))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.VALIDATION_ERROR);

    assertThatThrownBy(
            () -> validator.validateCancelAssignment(assignment, new CancelAssignmentRequest(null)))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.VALIDATION_ERROR);

    assertThatThrownBy(
            () ->
                validator.validateCancelAssignment(assignment, new CancelAssignmentRequest("   ")))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.VALIDATION_ERROR);
  }

  @Test
  void validateCancelAssignment_alreadyCompleted_throwsConflict() {
    RescueAssignment assignment = createAssignment();
    assignment.complete();

    assertThatThrownBy(
            () ->
                validator.validateCancelAssignment(
                    assignment, new CancelAssignmentRequest("Not needed")))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.CONFLICT);
  }

  @Test
  void validateCancelAssignment_alreadyCancelled_throwsConflict() {
    RescueAssignment assignment = createAssignment();
    assignment.cancel("First cancel");

    assertThatThrownBy(
            () ->
                validator.validateCancelAssignment(
                    assignment, new CancelAssignmentRequest("Second cancel")))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.CONFLICT);
  }

  @Test
  void validateCancelAssignment_valid_succeeds() {
    RescueAssignment assignment = createAssignment();

    assertThatCode(
            () ->
                validator.validateCancelAssignment(
                    assignment, new CancelAssignmentRequest("Operation aborted safely")))
        .doesNotThrowAnyException();
  }
}
