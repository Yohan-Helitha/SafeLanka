package lk.dmc.disaster.shared.reference;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lk.dmc.disaster.shared.domain.BaseEntity;

/** Relief item entity. */
@Entity
@Table(name = "relief_items")
public class ReliefItem extends BaseEntity {
    private String code;
    private String name;
    private String unit;

    @Enumerated(EnumType.STRING)
    private ReliefCategory category;

    protected ReliefItem() {}

    public String getCode() { return code; }
    public String getName() { return name; }
    public String getUnit() { return unit; }
    public ReliefCategory getCategory() { return category; }
}
