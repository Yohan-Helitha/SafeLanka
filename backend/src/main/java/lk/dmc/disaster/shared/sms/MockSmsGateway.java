package lk.dmc.disaster.shared.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Stand-in used when no real provider is configured (the assignment allows mocking the gateway).
 * The message text, which holds the code, is logged at DEBUG only; INFO shows just the masked
 * number.
 */
@Slf4j
@Component
@ConditionalOnProperty(
    prefix = "app.integrations.sms",
    name = "provider",
    havingValue = "mock",
    matchIfMissing = true)
class MockSmsGateway implements SmsGateway {

  @Override
  public void send(String recipientE164, String message) {
    log.info("[mock SMS] message queued for {}", mask(recipientE164));
    log.debug("[mock SMS] to {}: {}", mask(recipientE164), message);
  }

  @Override
  public boolean simulated() {
    return true;
  }

  static String mask(String e164) {
    return e164.length() <= 4 ? "****" : "***" + e164.substring(e164.length() - 4);
  }
}
