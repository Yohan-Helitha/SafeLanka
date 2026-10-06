package lk.dmc.disaster.warnings.service;

/** Result of sending one warning: people targeted and delivery attempts that worked or failed. */
public record DispatchSummary(int citizens, int delivered, int failed) {}
