package lk.dmc.disaster.shared.domain;

/** Severity of a public warning, lowest to highest. Values match the CHECK on warnings.level. */
public enum WarningLevel {
  ADVISORY(1),
  WATCH(2),
  WARNING(3),
  EVACUATE(4);

  private final int rank;

  WarningLevel(int rank) {
    this.rank = rank;
  }

  /** Position in the severity order, 1 (lowest) to 4 (highest). */
  public int rank() {
    return rank;
  }

  /** True when this level is strictly more severe than {@code other}. */
  public boolean isHigherThan(WarningLevel other) {
    return rank > other.rank;
  }
}
