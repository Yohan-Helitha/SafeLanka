package lk.dmc.disaster.auth.infrastructure;

import java.security.SecureRandom;
import lk.dmc.disaster.auth.application.port.OtpCodeGenerator;
import org.springframework.stereotype.Component;

@Component
class SecureOtpCodeGenerator implements OtpCodeGenerator {

  private final SecureRandom random = new SecureRandom();

  @Override
  public String next() {
    return String.format("%06d", random.nextInt(1_000_000));
  }
}
