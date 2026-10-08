package lk.dmc.disaster.warnings.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lk.dmc.disaster.shared.domain.WarningLevel;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** One attempt to reach one citizen over one channel. Starts QUEUED, ends DELIVERED or FAILED. */
@Entity
@Table(name = "notification_deliveries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationDelivery {

  @Id private UUID id;

  @Column(name = "warning_id", nullable = false)
  private UUID warningId;

  @Column(name = "citizen_id", nullable = false)
  private UUID citizenId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Channel channel;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private WarningLevel level;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private DeliveryStatus status;

  @Column(name = "attempted_at", nullable = false)
  private Instant attemptedAt;

  @Column(name = "delivered_at")
  private Instant deliveredAt;

  @Column(name = "failure_reason")
  private String failureReason;

  /** Queues one attempt; {@code level} is the warning level being sent. */
  public static NotificationDelivery queue(
      UUID warningId, UUID citizenId, Channel channel, WarningLevel level, Instant now) {
    NotificationDelivery delivery = new NotificationDelivery();
    delivery.id = UUID.randomUUID();
    delivery.warningId = warningId;
    delivery.citizenId = citizenId;
    delivery.channel = channel;
    delivery.level = level;
    delivery.status = DeliveryStatus.QUEUED;
    delivery.attemptedAt = now;
    return delivery;
  }

  /**
   * Marks the attempt successful.
   *
   * @throws AppException INVALID_STATE_TRANSITION when the attempt is already settled
   */
  public void delivered(Instant at) {
    requireQueued();
    status = DeliveryStatus.DELIVERED;
    deliveredAt = at;
  }

  /**
   * Marks the attempt failed. The reason is trimmed to the column length.
   *
   * @throws AppException INVALID_STATE_TRANSITION when the attempt is already settled
   */
  public void failed(String reason) {
    requireQueued();
    String text = reason == null || reason.isBlank() ? "Unknown failure" : reason.trim();
    status = DeliveryStatus.FAILED;
    failureReason = text.substring(0, Math.min(text.length(), WarningRules.FAILURE_REASON_MAX));
  }

  private void requireQueued() {
    if (status != DeliveryStatus.QUEUED) {
      throw new AppException(
          ErrorCode.INVALID_STATE_TRANSITION, "Delivery is already " + status + ".");
    }
  }
}
