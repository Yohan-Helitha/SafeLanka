package lk.dmc.disaster.shared.error;

/** The signed-in user may not do this (403). */
public class ForbiddenRoleException extends AppException {

  public ForbiddenRoleException(String message) {
    super(ErrorCode.FORBIDDEN_ROLE, message);
  }
}
