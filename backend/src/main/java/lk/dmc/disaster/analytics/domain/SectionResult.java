package lk.dmc.disaster.analytics.domain;

import org.springframework.lang.Nullable;

public record SectionResult<T>(
    SectionKey key,
    @Nullable T data,
    @Nullable String unavailableReason
) {
    public static <T> SectionResult<T> success(SectionKey key, T data) {
        return new SectionResult<>(key, data, null);
    }
    public static <T> SectionResult<T> unavailable(SectionKey key, String reason) {
        return new SectionResult<>(key, null, reason);
    }
    
    public boolean isUnavailable() {
        return unavailableReason != null;
    }
}
