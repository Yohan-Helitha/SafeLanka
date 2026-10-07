package lk.dmc.disaster.warnings.integration;

/** Outcome of one send. The reason is only set when the send failed. */
public record DeliveryResult(boolean success, String failureReason) {

  /** A successful send. */
  public static DeliveryResult delivered() {
    return new DeliveryResult(true, null);
  }

  /** A failed send, with the reason shown to the officer. */
  public static DeliveryResult failed(String reason) {
    return new DeliveryResult(false, reason);
  }
}
