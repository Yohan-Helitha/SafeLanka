package lk.dmc.disaster.auth.application;

import java.time.Instant;

/**
 * Confirmation that a code was sent. {@code devCode} is filled only where dev codes are exposed.
 */
public record OtpReceipt(Instant expiresAt, String devCode) {}
