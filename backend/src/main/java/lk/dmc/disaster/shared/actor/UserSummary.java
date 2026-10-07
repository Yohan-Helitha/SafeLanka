package lk.dmc.disaster.shared.actor;

import java.util.UUID;
import lk.dmc.disaster.shared.domain.Role;

/** The few user details other modules may show (never contact or credential data). */
public record UserSummary(UUID id, String fullName, Role role) {}
