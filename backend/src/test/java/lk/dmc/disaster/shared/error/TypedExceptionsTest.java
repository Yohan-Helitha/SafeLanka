package lk.dmc.disaster.shared.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TypedExceptionsTest {

  @Test
  void notFound_mapsTo404() {
    var e = new NotFoundException("Report not found.");

    assertThat(e.code()).isEqualTo(ErrorCode.NOT_FOUND);
    assertThat(e.getMessage()).isEqualTo("Report not found.");
  }

  @Test
  void conflict_defaultsToConflictCode() {
    assertThat(new ConflictException("Already used.").code()).isEqualTo(ErrorCode.CONFLICT);
  }

  @Test
  void conflict_acceptsOtherConflictCodes() {
    var e = new ConflictException(ErrorCode.INSUFFICIENT_STOCK, "Not enough.");

    assertThat(e.code()).isEqualTo(ErrorCode.INSUFFICIENT_STOCK);
  }

  @Test
  void conflict_rejectsNonConflictCode() {
    assertThatThrownBy(() -> new ConflictException(ErrorCode.NOT_FOUND, "x"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void invalidStateTransition_namesBothStatuses() {
    var e = new InvalidStateTransitionException("VERIFIED", "REJECTED");

    assertThat(e.code()).isEqualTo(ErrorCode.INVALID_STATE_TRANSITION);
    assertThat(e.getMessage()).contains("VERIFIED").contains("REJECTED");
  }

  @Test
  void businessRule_mapsTo422() {
    assertThat(new BusinessRuleException("x").code().status().value()).isEqualTo(422);
  }

  @Test
  void forbiddenRole_mapsTo403() {
    assertThat(new ForbiddenRoleException("x").code().status().value()).isEqualTo(403);
  }
}
