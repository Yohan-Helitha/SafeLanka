package lk.dmc.disaster.shared.error;

/** The requested record does not exist (404). */
public class NotFoundException extends AppException {

  public NotFoundException(String message) {
    super(ErrorCode.NOT_FOUND, message);
  }
}
