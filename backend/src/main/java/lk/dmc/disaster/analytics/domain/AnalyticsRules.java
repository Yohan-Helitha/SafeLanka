package lk.dmc.disaster.analytics.domain;
import java.time.Duration;
public final class AnalyticsRules {
    private AnalyticsRules() {}
    public static final Duration MAX_TIME_WINDOW = Duration.ofDays(14);
}
