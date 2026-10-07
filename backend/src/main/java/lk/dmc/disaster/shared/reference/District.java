package lk.dmc.disaster.shared.reference;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.Set;
import lk.dmc.disaster.shared.domain.BaseEntity;

/** District entity. */
@Entity
@Table(name = "districts")
public class District extends BaseEntity {
    private String code;
    private String name;
    private String province;
    
    @ManyToMany
    @JoinTable(
        name = "district_river_basins",
        joinColumns = @JoinColumn(name = "district_id"),
        inverseJoinColumns = @JoinColumn(name = "river_basin_id")
    )
    private Set<RiverBasin> riverBasins;
    
    protected District() {}

    public String getCode() { return code; }
    public String getName() { return name; }
    public String getProvince() { return province; }
    public Set<RiverBasin> getRiverBasins() { return riverBasins; }
}
