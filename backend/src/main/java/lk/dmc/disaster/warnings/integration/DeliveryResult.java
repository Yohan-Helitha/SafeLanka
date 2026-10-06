package lk.dmc.disaster.warnings.integration;

/** Outcome of one send. The reason is only set when the send failed. */
public record DeliveryResult(boolean success, String failureReason) {

  public static DeliveryResult delivered() {
    return new DeliveryResult(true, null);
  }

  public static DeliveryResult failed(String reason) {
    return new DeliveryResult(false, reason);
  }
}
