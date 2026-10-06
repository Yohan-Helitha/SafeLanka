package lk.dmc.disaster.warnings.repository;

import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.warnings.entity.NotificationDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Delivery records. The filtered, paged list uses {@link DeliverySpecifications}. */
public interface DeliveryRepository
    extends JpaRepository<NotificationDelivery, UUID>,
        JpaSpecificationExecutor<NotificationDelivery> {

  /** Delivery counts of one warning, grouped by channel and status. */
  @Query(
      """
      select new lk.dmc.disaster.warnings.repository.DeliveryCount(d.channel, d.status, count(d))
      from NotificationDelivery d
      where d.warningId = :warningId
      group by d.channel, d.status
      """)
  List<DeliveryCount> countByChannelAndStatus(@Param("warningId") UUID warningId);

  /** How many different citizens the warning was sent to. */
  @Query(
      "select count(distinct d.citizenId) from NotificationDelivery d where d.warningId = :warningId")
  long countTargetedCitizens(@Param("warningId") UUID warningId);
}
