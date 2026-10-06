package lk.dmc.disaster.warnings.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** Demo switches for one channel: whether it is on, and whether its gateway should fail. */
@Entity
@Table(name = "channel_settings")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChannelSetting {

  @Id
  @Enumerated(EnumType.STRING)
  private Channel channel;

  @Column(nullable = false)
  private boolean enabled;

  @Column(name = "simulate_failure", nullable = false)
  private boolean simulateFailure;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public void switchEnabled(boolean on) {
    this.enabled = on;
  }

  public void switchSimulatedFailure(boolean on) {
    this.simulateFailure = on;
  }
}
