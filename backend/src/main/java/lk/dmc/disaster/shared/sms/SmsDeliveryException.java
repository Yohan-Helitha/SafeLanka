package lk.dmc.disaster.shared.sms;

/** The provider could not take the message (rejected, unreachable or timed out). */
public class SmsDeliveryException extends RuntimeException {

  public SmsDeliveryException(String message) {
    super(message);
  }

  public SmsDeliveryException(String message, Throwable cause) {
    super(message, cause);
  }
}
