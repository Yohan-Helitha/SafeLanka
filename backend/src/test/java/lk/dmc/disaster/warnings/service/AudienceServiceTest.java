package lk.dmc.disaster.warnings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import lk.dmc.disaster.warnings.integration.AreaReference;
import lk.dmc.disaster.warnings.integration.CitizenDirectory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AudienceServiceTest {

  private static final UUID COLOMBO = UUID.randomUUID();
  private static final UUID GAMPAHA = UUID.randomUUID();
  private static final UUID KEGALLE = UUID.randomUUID();
  private static final UUID KELANI = UUID.randomUUID();

  @Mock private CitizenDirectory citizens;
  @Mock private AreaReference areas;

  private AudienceService service;

  @BeforeEach
  void setUp() {
    service = new AudienceService(citizens, areas);
  }

  @Test
  void preview_districtsOnly_countsPeopleInThoseDistricts() {
    when(areas.districtsInBasins(Set.of())).thenReturn(Set.of());
    when(citizens.countCitizensInAreas(Set.of(COLOMBO), Set.of())).thenReturn(12L);

    AudiencePreview preview = service.preview(new AudienceSelection(Set.of(COLOMBO), Set.of()));

    assertThat(preview.citizenCount()).isEqualTo(12);
    assertThat(preview.resolvedDistrictIds()).containsExactly(COLOMBO);
  }

  @Test
  void preview_basinOnly_includesTheBasinsDistricts() {
    when(areas.districtsInBasins(Set.of(KELANI))).thenReturn(Set.of(COLOMBO, GAMPAHA, KEGALLE));
    when(citizens.countCitizensInAreas(Set.of(COLOMBO, GAMPAHA, KEGALLE), Set.of(KELANI)))
        .thenReturn(24L);

    AudiencePreview preview = service.preview(new AudienceSelection(Set.of(), Set.of(KELANI)));

    assertThat(preview.citizenCount()).isEqualTo(24);
    assertThat(preview.resolvedDistrictIds()).containsExactlyInAnyOrder(COLOMBO, GAMPAHA, KEGALLE);
  }

  @Test
  void resolveDistricts_districtAlsoInTheBasin_isListedOnce() {
    when(areas.districtsInBasins(Set.of(KELANI))).thenReturn(Set.of(COLOMBO, GAMPAHA));

    Set<UUID> districts =
        service.resolveDistricts(new AudienceSelection(Set.of(COLOMBO), Set.of(KELANI)));

    assertThat(districts).containsExactlyInAnyOrder(COLOMBO, GAMPAHA);
  }

  @Test
  void findCitizenIds_passesResolvedDistrictsAndBasinsToTheDirectory() {
    UUID citizen = UUID.randomUUID();
    when(areas.districtsInBasins(Set.of(KELANI))).thenReturn(Set.of(COLOMBO));
    when(citizens.findCitizenIdsInAreas(Set.of(COLOMBO), Set.of(KELANI)))
        .thenReturn(List.of(citizen));

    List<UUID> ids = service.findCitizenIds(new AudienceSelection(Set.of(), Set.of(KELANI)));

    assertThat(ids).containsExactly(citizen);
  }

  @Test
  void selection_bothEmptyOrNull_isValidationError() {
    assertThatThrownBy(() -> new AudienceSelection(Set.of(), Set.of()))
        .isInstanceOfSatisfying(
            AppException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    assertThatThrownBy(() -> new AudienceSelection(null, null)).isInstanceOf(AppException.class);
  }

  @Test
  void selection_nullOneSide_isTreatedAsEmpty() {
    AudienceSelection selection = new AudienceSelection(null, Set.of(KELANI));

    assertThat(selection.districtIds()).isEmpty();
    assertThat(selection.riverBasinIds()).containsExactly(KELANI);
  }
}
