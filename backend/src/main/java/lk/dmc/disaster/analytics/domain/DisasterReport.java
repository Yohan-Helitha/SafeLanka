package lk.dmc.disaster.analytics.domain;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lk.dmc.disaster.shared.domain.BaseEntity;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "disaster_reports")
@Getter
public class DisasterReport extends BaseEntity {

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "filters", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> filters;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sections", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> sections;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "unavailable_sections", nullable = false, columnDefinition = "jsonb")
    private List<Map<String, String>> unavailableSections;

    @Column(name = "generated_by", nullable = false)
    private UUID generatedBy;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    protected DisasterReport() {}

    public DisasterReport(UUID eventId, Map<String, Object> filters, Map<String, Object> sections,
                          List<Map<String, String>> unavailableSections, UUID generatedBy, Instant generatedAt) {
        setId(UUID.randomUUID());
        this.eventId = eventId;
        this.filters = filters;
        this.sections = sections;
        this.unavailableSections = unavailableSections;
        this.generatedBy = generatedBy;
        this.generatedAt = generatedAt;
    }
}
