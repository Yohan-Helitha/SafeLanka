package lk.dmc.disaster.shared.error;

/** The request is well formed but breaks a business rule (422). */
public class BusinessRuleException extends AppException {

  public BusinessRuleException(String message) {
    super(ErrorCode.BUSINESS_RULE, message);
  }
}
