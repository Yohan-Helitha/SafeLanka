package lk.dmc.disaster.analytics.export;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Everything an exporter prints: the saved report plus the names it only stores ids for. Sections
 * are keyed by {@code SectionKey} name, as saved.
 */
public record DisasterReportView(
    UUID id,
    UUID eventId,
    String eventName,
    Map<String, Object> filters,
    Map<String, Object> sections,
    List<Map<String, String>> unavailableSections,
    String generatedByName,
    Instant generatedAt) {}
