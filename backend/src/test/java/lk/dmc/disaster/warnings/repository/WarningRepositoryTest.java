package lk.dmc.disaster.warnings.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.shared.domain.WarningLevel;
import lk.dmc.disaster.warnings.entity.Channel;
import lk.dmc.disaster.warnings.entity.DeliveryStatus;
import lk.dmc.disaster.warnings.entity.Hazard;
import lk.dmc.disaster.warnings.entity.HazardArea;
import lk.dmc.disaster.warnings.entity.NotificationDelivery;
import lk.dmc.disaster.warnings.entity.Warning;
import lk.dmc.disaster.warnings.entity.WarningStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.domain.Specification;

class WarningRepositoryTest extends RepositoryTestSupport {

  @Autowired private DeliveryRepository deliveries;

  private UUID districtA;
  private UUID districtB;
  private UUID basin;
  private Hazard hazard;

  @BeforeEach
  void setUp() {
    List<UUID> districtIds = ids("districts", 2);
    districtA = districtIds.get(0);
    districtB = districtIds.get(1);
    basin = firstId("river_basins");
    hazard = savedHazard(firstId("hazard_types"), new HazardArea(districtA, null), NOW);
  }

  @Test
  void findActiveCovering_districtTargeted_matchesOnlyThatDistrict() {
    Warning warning = savedWarning(hazard, districtTarget(districtA));

    assertThat(warnings.findActiveCovering(Set.of(districtA), Set.of())).contains(warning);
    assertThat(warnings.findActiveCovering(Set.of(districtB), Set.of())).doesNotContain(warning);
  }

  @Test
  void findActiveCovering_basinTargeted_matchesByBasin() {
    Warning warning = savedWarning(hazard, basinTarget(basin));

    assertThat(warnings.findActiveCovering(Set.of(), Set.of(basin))).contains(warning);
  }

  @Test
  void findActiveCovering_bothCollectionsEmpty_returnsNothing() {
    savedWarning(hazard, districtTarget(districtA));

    assertThat(warnings.findActiveCovering(Set.of(), Set.of())).isEmpty();
  }

  @Test
  void findActiveCovering_warningWithTwoAreas_isReturnedOnce() {
    Warning warning = savedWarning(hazard, districtTarget(districtA, districtB));

    assertThat(warnings.findActiveCovering(Set.of(districtA, districtB), Set.of()))
        .containsOnlyOnce(warning);
  }

  @Test
  void findActiveCovering_cancelledWarning_isHidden() {
    Warning warning = savedWarning(hazard, districtTarget(districtA));
    warning.cancel("Water level receded", NOW);
    warnings.saveAndFlush(warning);

    assertThat(warnings.findActiveCovering(Set.of(districtA), Set.of())).doesNotContain(warning);
  }

  @Test
  void findActiveCovering_afterEscalation_returnsOnlyTheNewWarning() {
    Warning old = savedWarning(hazard, districtTarget(districtA));
    Warning next =
        old.escalateTo(
            WarningLevel.EVACUATE, old.content(), old.getIssuedBy(), NOW.plusSeconds(60));
    warnings.saveAndFlush(old);
    warnings.saveAndFlush(next);

    assertThat(warnings.findActiveCovering(Set.of(districtA), Set.of())).containsExactly(next);
    assertThat(next.getSupersedesId()).isEqualTo(old.getId());
  }

  @Test
  void findByHazard_returnsNewestFirstAndFiltersByStatus() {
    Warning first = savedWarning(hazard, districtTarget(districtA));
    first.cancel("Issued by mistake", NOW);
    warnings.saveAndFlush(first);
    Warning second = savedWarning(hazard, districtTarget(districtA));

    assertThat(warnings.findByHazardIdAndStatus(hazard.getId(), WarningStatus.ACTIVE))
        .containsExactly(second);
    assertThat(warnings.findByHazardIdOrderByIssuedAtDesc(hazard.getId()))
        .containsExactlyInAnyOrder(first, second);
  }

  @Test
  void specifications_filterByStatusAndEvent_andNullMatchesAll() {
    Warning warning = savedWarning(hazard, districtTarget(districtA));

    Specification<Warning> active = WarningSpecifications.withStatus(WarningStatus.ACTIVE);
    Specification<Warning> cancelled = WarningSpecifications.withStatus(WarningStatus.CANCELLED);

    assertThat(warnings.findAll(active)).contains(warning);
    assertThat(warnings.findAll(cancelled)).doesNotContain(warning);
    assertThat(warnings.findAll(WarningSpecifications.withStatus(null))).contains(warning);
    assertThat(warnings.findAll(WarningSpecifications.forEvent(UUID.randomUUID())))
        .doesNotContain(warning);
    assertThat(warnings.findAll(WarningSpecifications.forEvent(null))).contains(warning);
  }

  @Test
  void targetAreaWithBothDistrictAndBasin_isRejectedByTheDatabase() {
    Warning warning = savedWarning(hazard, districtTarget(districtA));

    assertThatThrownBy(
            () ->
                jdbc.sql(
                        "insert into warning_target_areas (warning_id, district_id, river_basin_id)"
                            + " values (:w, :d, :b)")
                    .param("w", warning.getId())
                    .param("d", districtA)
                    .param("b", basin)
                    .update())
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void delivery_sameCitizenAndChannelTwice_isRejectedByTheDatabase() {
    Warning warning = savedWarning(hazard, districtTarget(districtA));
    UUID citizen = userWithRole("CITIZEN", 0);
    deliveries.saveAndFlush(NotificationDelivery.queue(warning.getId(), citizen, Channel.SMS, NOW));

    assertThatThrownBy(
            () ->
                deliveries.saveAndFlush(
                    NotificationDelivery.queue(warning.getId(), citizen, Channel.SMS, NOW)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void deliveryCounts_groupByChannelAndStatus_andCountDistinctCitizens() {
    Warning warning = savedWarning(hazard, districtTarget(districtA));
    UUID citizen1 = userWithRole("CITIZEN", 0);
    UUID citizen2 = userWithRole("CITIZEN", 1);
    NotificationDelivery push1 = saveDelivery(warning, citizen1, Channel.PUSH);
    NotificationDelivery push2 = saveDelivery(warning, citizen2, Channel.PUSH);
    NotificationDelivery sms = saveDelivery(warning, citizen1, Channel.SMS);
    push1.delivered(NOW);
    push2.delivered(NOW);
    sms.failed("Simulated gateway timeout");
    deliveries.flush();

    assertThat(deliveries.countByChannelAndStatus(warning.getId()))
        .containsExactlyInAnyOrder(
            new DeliveryCount(Channel.PUSH, DeliveryStatus.DELIVERED, 2),
            new DeliveryCount(Channel.SMS, DeliveryStatus.FAILED, 1));
    assertThat(deliveries.countTargetedCitizens(warning.getId())).isEqualTo(2);
  }

  @Test
  void deliverySpecifications_filterByStatusAndChannel() {
    Warning warning = savedWarning(hazard, districtTarget(districtA));
    UUID citizen = userWithRole("CITIZEN", 0);
    NotificationDelivery push = saveDelivery(warning, citizen, Channel.PUSH);
    NotificationDelivery sms = saveDelivery(warning, citizen, Channel.SMS);
    sms.failed("Simulated gateway timeout");
    deliveries.flush();

    Specification<NotificationDelivery> forWarning =
        DeliverySpecifications.forWarning(warning.getId());

    assertThat(deliveries.findAll(forWarning)).containsExactlyInAnyOrder(push, sms);
    assertThat(
            deliveries.findAll(
                forWarning.and(DeliverySpecifications.withStatus(DeliveryStatus.FAILED))))
        .containsExactly(sms);
    assertThat(deliveries.findAll(forWarning.and(DeliverySpecifications.onChannel(Channel.PUSH))))
        .containsExactly(push);
    assertThat(deliveries.findAll(DeliverySpecifications.forWarning(UUID.randomUUID()))).isEmpty();
  }

  private NotificationDelivery saveDelivery(Warning warning, UUID citizen, Channel channel) {
    return deliveries.save(NotificationDelivery.queue(warning.getId(), citizen, channel, NOW));
  }
}
