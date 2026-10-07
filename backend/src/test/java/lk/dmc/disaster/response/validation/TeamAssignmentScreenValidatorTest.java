package lk.dmc.disaster.response.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import lk.dmc.disaster.response.dto.request.RespondRequest;
import lk.dmc.disaster.response.dto.request.TeamStatusUpdateRequest;
import lk.dmc.disaster.response.entity.RescueAssignment;
import lk.dmc.disaster.response.entity.RescueTeam;
import lk.dmc.disaster.response.entity.TeamType;
import lk.dmc.disaster.shared.actor.ActingUser;
import lk.dmc.disaster.shared.domain.Role;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TeamAssignmentScreenValidatorTest {

  private TeamAssignmentScreenValidator validator;
  private UUID teamId;
  private RescueAssignment assignment;

  @BeforeEach
  void setUp() {
    validator = new TeamAssignmentScreenValidator();
    teamId = UUID.randomUUID();
    assignment =
        RescueAssignment.create(
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
    assignment.assignTeam(teamId);
  }

  @Test
  void validateRespond_nullRequest_throwsValidationError() {
    ActingUser user = new ActingUser(UUID.randomUUID(), Role.RESCUE_MEMBER, null, null, teamId);

    assertThatThrownBy(() -> validator.validateRespond(assignment, null, user))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.VALIDATION_ERROR);
  }

  @Test
  void validateRespond_unauthorizedUser_throwsForbiddenRole() {
    ActingUser wrongTeamUser =
        new ActingUser(UUID.randomUUID(), Role.RESCUE_MEMBER, null, null, UUID.randomUUID());

    assertThatThrownBy(
            () ->
                validator.validateRespond(
                    assignment, new RespondRequest(true, null), wrongTeamUser))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.FORBIDDEN_ROLE);

    ActingUser nullTeamUser =
        new ActingUser(UUID.randomUUID(), Role.RESCUE_MEMBER, null, null, null);

    assertThatThrownBy(
            () ->
                validator.validateRespond(assignment, new RespondRequest(true, null), nullTeamUser))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.FORBIDDEN_ROLE);

    // user == null branch
    assertThatThrownBy(
            () -> validator.validateRespond(assignment, new RespondRequest(true, null), null))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.FORBIDDEN_ROLE);
  }

  @Test
  void validateRespond_nonPendingStatus_throwsConflict() {
    assignment.acknowledge(); // Now ACCEPTED
    ActingUser user = new ActingUser(UUID.randomUUID(), Role.RESCUE_MEMBER, null, null, teamId);

    assertThatThrownBy(
            () -> validator.validateRespond(assignment, new RespondRequest(true, null), user))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.CONFLICT);
  }

  @Test
  void validateRespond_declineWithoutReason_throwsValidationError() {
    ActingUser user = new ActingUser(UUID.randomUUID(), Role.RESCUE_MEMBER, null, null, teamId);

    assertThatThrownBy(
            () -> validator.validateRespond(assignment, new RespondRequest(false, null), user))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.VALIDATION_ERROR);

    assertThatThrownBy(
            () -> validator.validateRespond(assignment, new RespondRequest(false, "   "), user))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.VALIDATION_ERROR);
  }

  @Test
  void validateRespond_validAcceptAndDecline_succeeds() {
    ActingUser user = new ActingUser(UUID.randomUUID(), Role.RESCUE_MEMBER, null, null, teamId);

    assertThatCode(
            () -> validator.validateRespond(assignment, new RespondRequest(true, null), user))
        .doesNotThrowAnyException();

    assertThatCode(
            () ->
                validator.validateRespond(
                    assignment, new RespondRequest(false, "Engine failure"), user))
        .doesNotThrowAnyException();
  }

  @Test
  void validateStatusUpdate_nullOrInvalidStatus_throwsValidationError() {
    RescueTeam team =
        RescueTeam.create("Team", UUID.randomUUID(), UUID.randomUUID(), TeamType.MEDICAL, 5);

    assertThatThrownBy(() -> validator.validateStatusUpdate(team, null, null))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.VALIDATION_ERROR);

    assertThatThrownBy(
            () ->
                validator.validateStatusUpdate(
                    team, new TeamStatusUpdateRequest(null, null, null, false), null))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.VALIDATION_ERROR);

    assertThatThrownBy(
            () ->
                validator.validateStatusUpdate(
                    team, new TeamStatusUpdateRequest("INVALID_STATUS", null, null, false), null))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.VALIDATION_ERROR);
  }

  @Test
  void validateStatusUpdate_validStatus_succeeds() {
    RescueTeam team =
        RescueTeam.create("Team", UUID.randomUUID(), UUID.randomUUID(), TeamType.MEDICAL, 5);

    assertThatCode(
            () ->
                validator.validateStatusUpdate(
                    team, new TeamStatusUpdateRequest("ACTIVE", null, null, false), null))
        .doesNotThrowAnyException();
  }
}
