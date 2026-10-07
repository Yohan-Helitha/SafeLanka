package lk.dmc.disaster.warnings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.shared.domain.WarningLevel;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import lk.dmc.disaster.warnings.entity.Channel;
import lk.dmc.disaster.warnings.entity.Warning;
import lk.dmc.disaster.warnings.entity.WarningStatus;
import lk.dmc.disaster.warnings.repository.WarningRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class WarningQueryServiceTest {

  private static final UUID COLOMBO = UUID.randomUUID();
  private static final UUID GAMPAHA = UUID.randomUUID();
  private static final UUID KELANI = UUID.randomUUID();

  @Mock private WarningRepository warnings;
  @Mock private AudienceService audience;
  @Mock private DeliveryQueryService deliveries;

  private WarningQueryService service;

  @BeforeEach
  void setUp() {
    service = new WarningQueryService(warnings, audience, deliveries);
  }

  private static final DeliveryOutcome OUTCOME =
      new DeliveryOutcome(24, 70, 2, List.of(new ChannelOutcome(Channel.SMS, 22, 2)));

  @Test
  void get_returnsTheWarningWithItsAreasResolvedDistrictsAndDeliveryTotals() {
    Warning warning =
        ServiceFixtures.warning(
            WarningLevel.WARNING, ServiceFixtures.basinTarget(KELANI), ServiceFixtures.NOW);
    when(warnings.findById(warning.getId())).thenReturn(Optional.of(warning));
    when(audience.resolveDistricts(new AudienceSelection(Set.of(), Set.of(KELANI))))
        .thenReturn(Set.of(COLOMBO, GAMPAHA));
    when(deliveries.outcomeOf(warning.getId())).thenReturn(OUTCOME);

    WarningView view = service.get(warning.getId());

    assertThat(view.warning()).isSameAs(warning);
    assertThat(view.target().riverBasinIds()).containsExactly(KELANI);
    assertThat(view.resolvedDistrictIds()).containsExactlyInAnyOrder(COLOMBO, GAMPAHA);
    assertThat(view.deliveries()).isEqualTo(OUTCOME);
    assertThat(view.evidenceReportIds()).isEmpty();
  }

  @Test
  void get_unknownWarning_isNotFound() {
    UUID id = UUID.randomUUID();
    when(warnings.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.get(id))
        .isInstanceOfSatisfying(
            AppException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.NOT_FOUND));
  }

  @Test
  @SuppressWarnings("unchecked")
  void list_buildsAViewForEveryWarningOnThePage() {
    Warning first =
        ServiceFixtures.warning(
            WarningLevel.WATCH, ServiceFixtures.districtTarget(COLOMBO), ServiceFixtures.NOW);
    Warning second =
        ServiceFixtures.warning(
            WarningLevel.EVACUATE, ServiceFixtures.districtTarget(COLOMBO), ServiceFixtures.NOW);
    Pageable pageable = PageRequest.of(0, 20);
    Page<Warning> page = new PageImpl<>(List.of(first, second), pageable, 2);
    when(warnings.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
    when(audience.resolveDistricts(any(AudienceSelection.class))).thenReturn(Set.of(COLOMBO));
    when(deliveries.outcomeOf(any(UUID.class))).thenReturn(OUTCOME);

    Page<WarningView> result = service.list(WarningStatus.ACTIVE, null, pageable);

    assertThat(result.getTotalElements()).isEqualTo(2);
    assertThat(result.getContent())
        .extracting(view -> view.warning().getId())
        .containsExactly(first.getId(), second.getId());
  }
}
