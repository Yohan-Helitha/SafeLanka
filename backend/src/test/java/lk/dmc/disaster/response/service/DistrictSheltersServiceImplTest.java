package lk.dmc.disaster.response.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lk.dmc.disaster.response.dto.response.ShelterDto;
import lk.dmc.disaster.response.dto.response.ShelterSuggestionDto;
import lk.dmc.disaster.response.entity.Shelter;
import lk.dmc.disaster.response.entity.ShelterStatus;
import lk.dmc.disaster.response.repository.ShelterRepository;
import lk.dmc.disaster.response.validation.DistrictSheltersScreenValidator;
import lk.dmc.disaster.shared.actor.ActingUser;
import lk.dmc.disaster.shared.actor.ActingUserContext;
import lk.dmc.disaster.shared.domain.Role;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DistrictSheltersServiceImplTest {

  @Mock private ShelterRepository shelters;
  @Mock private ActingUserContext actingUser;
  @Mock private DistrictSheltersScreenValidator validator;

  private DistrictSheltersServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new DistrictSheltersServiceImpl(shelters, actingUser, validator);
  }

  @Test
  void getShelters_whenAvailableOnlyIsTrue_queriesOpenShelters() {
    UUID districtId = UUID.randomUUID();
    Shelter shelter = Shelter.create("Open Shelter", districtId, "Address", 6.9, 79.8, 100, null);
    when(shelters.findByDistrictIdAndStatus(districtId, ShelterStatus.OPEN))
        .thenReturn(List.of(shelter));

    List<ShelterDto> result = service.getShelters(districtId, true);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).name()).isEqualTo("Open Shelter");
    verify(shelters).findByDistrictIdAndStatus(districtId, ShelterStatus.OPEN);
  }

  @Test
  void getShelters_whenAvailableOnlyIsFalseOrNull_queriesAllDistrictShelters() {
    UUID districtId = UUID.randomUUID();
    Shelter s1 = Shelter.create("Shelter 1", districtId, "Address 1", 6.9, 79.8, 100, null);
    Shelter s2 = Shelter.create("Shelter 2", districtId, "Address 2", 6.9, 79.8, 50, null);
    when(shelters.findByDistrictId(districtId)).thenReturn(List.of(s1, s2));

    List<ShelterDto> result = service.getShelters(districtId, null);

    assertThat(result).hasSize(2);
    verify(shelters).findByDistrictId(districtId);

    List<ShelterDto> resultFalse = service.getShelters(districtId, false);
    assertThat(resultFalse).hasSize(2);
  }

  @Test
  void getSuggestions_filtersAndSortsByAvailableCapacity() {
    UUID districtId = UUID.randomUUID();
    Shelter s1 = Shelter.create("Shelter 1", districtId, "Address 1", 6.9, 79.8, 100, null);
    s1.updateOccupancy(50); // available = 50

    Shelter s2 = Shelter.create("Shelter 2", districtId, "Address 2", 6.9, 79.8, 200, null);
    s2.updateOccupancy(80); // available = 120

    Shelter s3 = Shelter.create("Shelter 3", districtId, "Address 3", 6.9, 79.8, 80, null);
    s3.updateOccupancy(60); // available = 20

    when(shelters.findByDistrictIdAndStatusAndCurrentOccupancyLessThan(
            districtId, ShelterStatus.OPEN, Integer.MAX_VALUE))
        .thenReturn(List.of(s1, s2, s3));

    // Requesting shelter for 40 people -> s3 (20) should be excluded, s2 (120) and s1 (50)
    // included, s2 first
    List<ShelterSuggestionDto> suggestions = service.getSuggestions(districtId, 40);

    assertThat(suggestions).hasSize(2);
    assertThat(suggestions.get(0).name()).isEqualTo("Shelter 2");
    assertThat(suggestions.get(1).name()).isEqualTo("Shelter 1");
  }

  @Test
  void updateOccupancy_shelterNotFound_throwsNotFound() {
    UUID shelterId = UUID.randomUUID();
    when(shelters.findById(shelterId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.updateOccupancy(shelterId, 30))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.NOT_FOUND);
  }

  @Test
  void updateOccupancy_valid_validatesUpdatesAndSaves() {
    UUID shelterId = UUID.randomUUID();
    Shelter shelter =
        Shelter.create("Shelter A", UUID.randomUUID(), "Address", 6.9, 79.8, 100, null);
    when(shelters.findById(shelterId)).thenReturn(Optional.of(shelter));

    ActingUser user = new ActingUser(UUID.randomUUID(), Role.DISTRICT_OFFICER, null, null, null);
    when(actingUser.require()).thenReturn(user);
    when(shelters.save(any(Shelter.class))).thenAnswer(inv -> inv.getArgument(0));

    ShelterDto dto = service.updateOccupancy(shelterId, 45);

    assertThat(dto).isNotNull();
    assertThat(dto.currentOccupancy()).isEqualTo(45);
    verify(validator).validateOccupancyUpdate(eq(shelter), any(), eq(user));
    verify(shelters).save(shelter);
  }
}
