package lk.dmc.disaster.shared.reference;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lk.dmc.disaster.shared.domain.BaseEntity;

/** River basin entity. */
@Entity
@Table(name = "river_basins")
public class RiverBasin extends BaseEntity {
    private String code;
    private String name;

    protected RiverBasin() {}

    public String getCode() { return code; }
    public String getName() { return name; }
}
