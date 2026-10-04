package lk.dmc.disaster.shared.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Says at startup which SMS gateway is active, so a mock never passes for the real thing. */
@Slf4j
@Component
class SmsGatewayStartupLog implements ApplicationRunner {

  private final SmsGateway gateway;
  private final SmsProperties properties;

  SmsGatewayStartupLog(SmsGateway gateway, SmsProperties properties) {
    this.gateway = gateway;
    this.properties = properties;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (gateway.simulated()) {
      log.info("SMS gateway: MOCK. No messages are sent (set SMS_PROVIDER=textlk for real SMS).");
    } else {
      log.info(
          "SMS gateway: {} - REAL messages, sender id '{}'.",
          properties.provider(),
          properties.senderId());
    }
  }
}
