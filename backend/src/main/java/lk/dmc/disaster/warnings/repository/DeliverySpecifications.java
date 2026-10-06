package lk.dmc.disaster.warnings.repository;

import java.util.UUID;
import lk.dmc.disaster.warnings.entity.Channel;
import lk.dmc.disaster.warnings.entity.DeliveryStatus;
import lk.dmc.disaster.warnings.entity.NotificationDelivery;
import org.springframework.data.jpa.domain.Specification;

/** Filters for the delivery list of one warning. A null status or channel matches everything. */
public final class DeliverySpecifications {

  private DeliverySpecifications() {}

  public static Specification<NotificationDelivery> forWarning(UUID warningId) {
    return (root, query, cb) -> cb.equal(root.get("warningId"), warningId);
  }

  public static Specification<NotificationDelivery> withStatus(DeliveryStatus status) {
    return (root, query, cb) ->
        status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
  }

  public static Specification<NotificationDelivery> onChannel(Channel channel) {
    return (root, query, cb) ->
        channel == null ? cb.conjunction() : cb.equal(root.get("channel"), channel);
  }
}
