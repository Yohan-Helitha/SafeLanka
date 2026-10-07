package lk.dmc.disaster.warnings.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.warnings.entity.Channel;
import lk.dmc.disaster.warnings.entity.DeliveryStatus;
import lk.dmc.disaster.warnings.entity.Hazard;
import lk.dmc.disaster.warnings.entity.HazardStatus;
import lk.dmc.disaster.warnings.entity.NotificationDelivery;
import lk.dmc.disaster.warnings.entity.Warning;
import lk.dmc.disaster.warnings.entity.WarningStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

/**
 * The optional-filter rule of every specification (a null filter matches everything), checked with
 * a stand-in criteria builder. The queries themselves run against PostgreSQL in the repository
 * tests.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
class SpecificationsTest {

  private CriteriaBuilder cb;
  private CriteriaQuery<?> query;
  private Predicate everything;
  private Predicate restricted;

  @BeforeEach
  void setUp() {
    cb = mock(CriteriaBuilder.class);
    query = mock(CriteriaQuery.class);
    everything = mock(Predicate.class);
    restricted = mock(Predicate.class);
    when(cb.conjunction()).thenReturn(everything);
    when(cb.equal(any(), any(Object.class))).thenReturn(restricted);
  }

  private <T> Root<T> rootWith(String attribute) {
    Root<T> root = mock(Root.class);
    Path<Object> path = mock(Path.class);
    when(root.get(attribute)).thenReturn(path);
    when(path.in(any(java.util.Collection.class))).thenReturn(restricted);
    return root;
  }

  private <T> Predicate apply(Specification<T> specification, Root<T> root) {
    return specification.toPredicate(root, (CriteriaQuery) query, cb);
  }

  @Test
  void hazardStatusIn_nullOrEmpty_matchesEverything_otherwiseRestricts() {
    Root<Hazard> root = rootWith("status");

    assertThat(apply(HazardSpecifications.statusIn(null), root)).isSameAs(everything);
    assertThat(apply(HazardSpecifications.statusIn(List.of()), root)).isSameAs(everything);
    assertThat(apply(HazardSpecifications.statusIn(Set.of(HazardStatus.WARNED)), root))
        .isSameAs(restricted);
  }

  @Test
  void hazardTypeAndDistrict_nullMatchesEverything_valueRestricts() {
    Root<Hazard> root = mock(Root.class);
    when(root.get("hazardTypeId")).thenReturn(mock(Path.class));
    when(root.get("districtId")).thenReturn(mock(Path.class));

    assertThat(apply(HazardSpecifications.ofType(null), root)).isSameAs(everything);
    assertThat(apply(HazardSpecifications.inDistrict(null), root)).isSameAs(everything);
    assertThat(apply(HazardSpecifications.ofType(UUID.randomUUID()), root)).isSameAs(restricted);
    assertThat(apply(HazardSpecifications.inDistrict(UUID.randomUUID()), root))
        .isSameAs(restricted);
  }

  @Test
  void warningStatusAndEvent_nullMatchesEverything_valueRestricts() {
    Root<Warning> root = mock(Root.class);
    when(root.get("status")).thenReturn(mock(Path.class));
    when(root.get("eventId")).thenReturn(mock(Path.class));

    assertThat(apply(WarningSpecifications.withStatus(null), root)).isSameAs(everything);
    assertThat(apply(WarningSpecifications.forEvent(null), root)).isSameAs(everything);
    assertThat(apply(WarningSpecifications.withStatus(WarningStatus.ACTIVE), root))
        .isSameAs(restricted);
    assertThat(apply(WarningSpecifications.forEvent(UUID.randomUUID()), root)).isSameAs(restricted);
  }

  @Test
  void deliveryFilters_warningAlwaysRestricts_statusAndChannelOnlyWhenGiven() {
    Root<NotificationDelivery> root = mock(Root.class);
    when(root.get("warningId")).thenReturn(mock(Path.class));
    when(root.get("status")).thenReturn(mock(Path.class));
    when(root.get("channel")).thenReturn(mock(Path.class));

    assertThat(apply(DeliverySpecifications.forWarning(UUID.randomUUID()), root))
        .isSameAs(restricted);
    assertThat(apply(DeliverySpecifications.withStatus(null), root)).isSameAs(everything);
    assertThat(apply(DeliverySpecifications.onChannel(null), root)).isSameAs(everything);
    assertThat(apply(DeliverySpecifications.withStatus(DeliveryStatus.FAILED), root))
        .isSameAs(restricted);
    assertThat(apply(DeliverySpecifications.onChannel(Channel.SMS), root)).isSameAs(restricted);
  }

  @Test
  void nullFilters_neverTouchTheCriteriaBuilderBeyondTheEmptyPredicate() {
    Root<Hazard> root = mock(Root.class);

    apply(HazardSpecifications.ofType(null), root);

    verify(cb, never()).equal(any(), any(Object.class));
  }

  // ---- default methods of HazardRepository -------------------------------------------------

  @Test
  void findNewestOpenMatching_asksForOnePageOfOneAndReturnsTheFirst() {
    HazardRepository repository = Mockito.mock(HazardRepository.class, Mockito.CALLS_REAL_METHODS);
    Hazard newest = Mockito.mock(Hazard.class);
    UUID type = UUID.randomUUID();
    UUID district = UUID.randomUUID();
    Mockito.doReturn(List.of(newest))
        .when(repository)
        .findOpenMatching(type, district, Set.of(), PageRequest.of(0, 1));

    Optional<Hazard> result = repository.findNewestOpenMatching(type, district, Set.of());

    assertThat(result).containsSame(newest);
  }

  @Test
  void findNewestOpenMatching_nothingFound_isEmpty() {
    HazardRepository repository = Mockito.mock(HazardRepository.class, Mockito.CALLS_REAL_METHODS);
    Mockito.doReturn(List.of())
        .when(repository)
        .findOpenMatching(any(), any(), any(), any(Pageable.class));

    assertThat(repository.findNewestOpenMatching(UUID.randomUUID(), UUID.randomUUID(), Set.of()))
        .isEmpty();
  }

  @Test
  void findOpenForSensor_looksForAnyStatusExceptResolved() {
    HazardRepository repository = Mockito.mock(HazardRepository.class, Mockito.CALLS_REAL_METHODS);
    UUID sensor = UUID.randomUUID();
    Hazard open = Mockito.mock(Hazard.class);
    Mockito.doReturn(Optional.of(open))
        .when(repository)
        .findFirstBySensorIdAndStatusNotOrderByDetectedAtDesc(sensor, HazardStatus.RESOLVED);

    assertThat(repository.findOpenForSensor(sensor)).containsSame(open);
  }
}
