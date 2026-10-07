package lk.dmc.disaster.reports.repository;

import java.time.Clock;
import java.time.Year;
import lk.dmc.disaster.reports.entity.ReportRules;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Hands out report numbers such as {@code RPT-2026-0013} from the database sequence. */
@Component
public class ReferenceNumberGenerator {

  private final JdbcClient jdbc;
  private final Clock clock;

  public ReferenceNumberGenerator(JdbcClient jdbc, Clock clock) {
    this.jdbc = jdbc;
    this.clock = clock;
  }

  /** The sequence never repeats a value, so two callers can never share a number. */
  public String next() {
    long sequence =
        jdbc.sql("select nextval('report_reference_seq')").query(Long.class).single();
    return ReportRules.REFERENCE_FORMAT.formatted(Year.now(clock).getValue(), sequence);
  }
}
