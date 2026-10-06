package lk.dmc.disaster.warnings.entity;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;

/**
 * A table of allowed status changes. Statuses with no entry are final. Shared by the warning and
 * hazard machines so the transition check is written once.
 */
final class StatusMachine<S extends Enum<S>> {

  private final String subject;
  private final Map<S, Set<S>> allowed;

  private StatusMachine(String subject, Map<S, Set<S>> allowed) {
    this.subject = subject;
    this.allowed = allowed;
  }

  static <S extends Enum<S>> Builder<S> builder(String subject, Class<S> type) {
    return new Builder<>(subject, type);
  }

  boolean canTransition(S from, S to) {
    return allowed.getOrDefault(from, Set.of()).contains(to);
  }

  /**
   * Checks a status change.
   *
   * @throws AppException INVALID_STATE_TRANSITION when the change is not allowed
   */
  void require(S from, S to) {
    if (!canTransition(from, to)) {
      throw new AppException(
          ErrorCode.INVALID_STATE_TRANSITION,
          subject + " cannot change from " + from + " to " + to + ".");
    }
  }

  /** Collects the allowed changes, then builds the immutable machine. */
  static final class Builder<S extends Enum<S>> {

    private final String subject;
    private final Class<S> type;
    private final Map<S, Set<S>> allowed;

    private Builder(String subject, Class<S> type) {
      this.subject = subject;
      this.type = type;
      this.allowed = new EnumMap<>(type);
    }

    @SafeVarargs
    final Builder<S> allow(S from, S... to) {
      Set<S> targets = EnumSet.noneOf(type);
      targets.addAll(Set.of(to));
      allowed.put(from, targets);
      return this;
    }

    StatusMachine<S> build() {
      return new StatusMachine<>(subject, allowed);
    }
  }
}
