package lk.dmc.disaster.shared.error;

/** A status change the state machine does not allow (409). */
public class InvalidStateTransitionException extends AppException {

  public InvalidStateTransitionException(Object from, Object to) {
    super(
        ErrorCode.INVALID_STATE_TRANSITION,
        "Cannot change status from " + from + " to " + to + ".");
  }
}
