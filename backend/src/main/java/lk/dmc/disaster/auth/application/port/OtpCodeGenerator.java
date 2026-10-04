package lk.dmc.disaster.auth.application.port;

/** Source of verification codes; replaceable in tests so the code is known. */
public interface OtpCodeGenerator {

  /** A fresh six-digit code, zero padded. */
  String next();
}
