package lk.dmc.disaster.shared.reference;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.List;
import lk.dmc.disaster.shared.domain.BaseEntity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Hazard type entity. */
@Entity
@Table(name = "hazard_types")
public class HazardType extends BaseEntity {
    private String code;
    private String name;

    @Enumerated(EnumType.STRING)
    private OnsetSpeed onsetSpeed;

    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> reportCategories;

    private boolean active;

    protected HazardType() {}

    public String getCode() { return code; }
    public String getName() { return name; }
    public OnsetSpeed getOnsetSpeed() { return onsetSpeed; }
    public List<String> getReportCategories() { return reportCategories; }
    public boolean isActive() { return active; }
}
