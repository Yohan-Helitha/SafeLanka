package lk.dmc.disaster.warnings.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** A verified report linked to a hazard as evidence. */
@Entity
@Table(name = "hazard_evidence")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HazardEvidence {

  @EmbeddedId private HazardEvidenceId id;

  @Column(name = "linked_at", nullable = false)
  private Instant linkedAt;

  public static HazardEvidence link(UUID hazardId, UUID reportId, Instant now) {
    HazardEvidence evidence = new HazardEvidence();
    evidence.id = new HazardEvidenceId(hazardId, reportId);
    evidence.linkedAt = now;
    return evidence;
  }

  public UUID hazardId() {
    return id.hazardId();
  }

  public UUID reportId() {
    return id.reportId();
  }
}
