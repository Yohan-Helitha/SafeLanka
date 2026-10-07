package lk.dmc.disaster.warnings.service;

import java.util.Comparator;
import lk.dmc.disaster.warnings.entity.Warning;

/** How warnings are listed to the public and to other modules: most serious, then newest. */
final class WarningOrdering {

  static final Comparator<Warning> HIGHEST_LEVEL_FIRST =
      Comparator.comparingInt((Warning w) -> w.getLevel().rank())
          .thenComparing(Warning::getIssuedAt)
          .reversed();

  private WarningOrdering() {}
}
