package lk.dmc.disaster.warnings.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** One district or one river basin a warning targets. Exactly one of the two ids is set. */
@Entity
@Table(name = "warning_target_areas")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WarningTargetArea {

  @Id private UUID id;

  @Column(name = "district_id")
  private UUID districtId;

  @Column(name = "river_basin_id")
  private UUID riverBasinId;

  public static WarningTargetArea ofDistrict(UUID districtId) {
    WarningTargetArea area = new WarningTargetArea();
    area.id = UUID.randomUUID();
    area.districtId = districtId;
    return area;
  }

  public static WarningTargetArea ofRiverBasin(UUID riverBasinId) {
    WarningTargetArea area = new WarningTargetArea();
    area.id = UUID.randomUUID();
    area.riverBasinId = riverBasinId;
    return area;
  }
}
