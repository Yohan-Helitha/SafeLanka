package lk.dmc.disaster.shared.reference;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.Set;
import lk.dmc.disaster.shared.domain.BaseEntity;

/** River basin entity. */
@Entity
@Table(name = "river_basins")
public class RiverBasin extends BaseEntity {
  private String code;
  private String name;

  /** The districts the basin flows through; {@link District} owns the join table. */
  @ManyToMany(mappedBy = "riverBasins")
  private Set<District> districts;

  protected RiverBasin() {}

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public Set<District> getDistricts() {
    return districts;
  }
}
