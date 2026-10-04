package lk.dmc.disaster.shared.sms;

/**
 * Sends text messages. Modules (sign-up codes, SMS warnings) depend on this interface only, so the
 * provider behind it can change without touching them.
 */
public interface SmsGateway {

  /**
   * Sends one message.
   *
   * @param recipientE164 the number in E.164 form, for example {@code +94771234567}
   * @throws SmsDeliveryException when the provider did not accept the message
   */
  void send(String recipientE164, String message);

  /**
   * True when messages do not really leave the system. Callers use it to decide whether a code may
   * be shown on screen for demos, which must never happen with a real gateway.
   */
  boolean simulated();
}
