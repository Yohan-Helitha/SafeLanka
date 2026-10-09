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
import lk.dmc.disaster.response.dto.request.CreateHeadcountUpdateRequest;
import lk.dmc.disaster.response.dto.response.ShelterDto;
import lk.dmc.disaster.response.dto.response.ShelterHeadcountUpdateDto;
import lk.dmc.disaster.response.dto.response.ShelterSuggestionDto;
import lk.dmc.disaster.response.entity.HeadcountUpdateStatus;
import lk.dmc.disaster.response.entity.Shelter;
import lk.dmc.disaster.response.entity.ShelterHeadcountUpdate;
import lk.dmc.disaster.response.entity.ShelterStatus;
import lk.dmc.disaster.response.repository.ActivityLogRepository;
import lk.dmc.disaster.response.repository.OccupancyLogRepository;
import lk.dmc.disaster.response.repository.ShelterHeadcountUpdateRepository;
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
  @Mock private ShelterHeadcountUpdateRepository headcountUpdates;
  @Mock private OccupancyLogRepository occupancyLogs;
  @Mock private ActivityLogRepository activityLogs;
  @Mock private ActingUserContext actingUser;
  @Mock private DistrictSheltersScreenValidator validator;

  private DistrictSheltersServiceImpl service;

  @BeforeEach
  void setUp() {
    service =
        new DistrictSheltersServiceImpl(
            shelters, headcountUpdates, occupancyLogs, activityLogs, actingUser, validator);
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
    verify(occupancyLogs).save(any());
    verify(activityLogs).save(any());
  }

  @Test
  void getHeadcountUpdates_returnsMappedDtos() {
    UUID districtId = UUID.randomUUID();
    UUID shelterId = UUID.randomUUID();
    Shelter shelter = Shelter.create("Shelter X", districtId, "Address", 6.9, 79.8, 200, null);
    ShelterHeadcountUpdate update =
        ShelterHeadcountUpdate.create(
            shelterId, districtId, 150, 100, "Coordinator", "SHELTER_COORDINATOR", "New arrivals");

    when(headcountUpdates.findByDistrictIdAndStatusOrderByReportedAtDesc(
            districtId, HeadcountUpdateStatus.PENDING))
        .thenReturn(List.of(update));
    when(shelters.findAll()).thenReturn(List.of(shelter));

    List<ShelterHeadcountUpdateDto> list =
        service.getHeadcountUpdates(districtId, HeadcountUpdateStatus.PENDING);

    assertThat(list).hasSize(1);
    assertThat(list.get(0).reportedOccupancy()).isEqualTo(150);
  }

  @Test
  void applyHeadcountUpdate_valid_appliesAndSaves() {
    UUID updateId = UUID.randomUUID();
    UUID shelterId = UUID.randomUUID();
    UUID districtId = UUID.randomUUID();
    Shelter shelter = Shelter.create("Shelter X", districtId, "Address", 6.9, 79.8, 200, null);
    shelter.updateOccupancy(100);

    ShelterHeadcountUpdate update =
        ShelterHeadcountUpdate.create(
            shelterId, districtId, 160, 100, "Coordinator", "SHELTER_COORDINATOR", "High intake");

    when(headcountUpdates.findById(updateId)).thenReturn(Optional.of(update));
    when(shelters.findById(shelterId)).thenReturn(Optional.of(shelter));
    ActingUser user = new ActingUser(UUID.randomUUID(), Role.DISTRICT_OFFICER, null, null, null);
    when(actingUser.require()).thenReturn(user);
    when(shelters.save(any(Shelter.class))).thenAnswer(inv -> inv.getArgument(0));

    ShelterDto result = service.applyHeadcountUpdate(updateId, null);

    assertThat(result.currentOccupancy()).isEqualTo(160);
    assertThat(update.getStatus()).isEqualTo(HeadcountUpdateStatus.APPLIED);
    verify(headcountUpdates).save(update);
    verify(occupancyLogs).save(any());
    verify(activityLogs).save(any());
  }

  @Test
  void createHeadcountUpdate_savesAndReturnsDto() {
    UUID shelterId = UUID.randomUUID();
    UUID districtId = UUID.randomUUID();
    Shelter shelter = Shelter.create("Shelter B", districtId, "Road", 6.9, 79.8, 300, null);
    when(shelters.findById(shelterId)).thenReturn(Optional.of(shelter));
    when(headcountUpdates.save(any(ShelterHeadcountUpdate.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    CreateHeadcountUpdateRequest req =
        new CreateHeadcountUpdateRequest(
            shelterId, 180, "Field Lead", "VOLUNTEER", "30 families arrived");

    ShelterHeadcountUpdateDto dto = service.createHeadcountUpdate(req);

    assertThat(dto).isNotNull();
    assertThat(dto.reportedOccupancy()).isEqualTo(180);
    assertThat(dto.reportedByName()).isEqualTo("Field Lead");
    verify(headcountUpdates).save(any());
    verify(activityLogs).save(any());
  }

  @Test
  void applyHeadcountUpdate_notFound_throwsException() {
    UUID updateId = UUID.randomUUID();
    when(headcountUpdates.findById(updateId)).thenReturn(Optional.empty());

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.applyHeadcountUpdate(updateId, null))
        .isInstanceOf(lk.dmc.disaster.shared.error.AppException.class)
        .hasMessageContaining("Headcount update not found");
  }

  @Test
  void applyHeadcountUpdate_alreadyProcessed_throwsConflictException() {
    UUID updateId = UUID.randomUUID();
    ShelterHeadcountUpdate update =
        ShelterHeadcountUpdate.create(
            UUID.randomUUID(), UUID.randomUUID(), 100, 50, "Lead", "VOLUNTEER", "Old report");
    update.markApplied(UUID.randomUUID());
    when(headcountUpdates.findById(updateId)).thenReturn(Optional.of(update));

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.applyHeadcountUpdate(updateId, null))
        .isInstanceOf(lk.dmc.disaster.shared.error.AppException.class)
        .hasMessageContaining("already been processed");
  }

  @Test
  void applyHeadcountUpdate_withCustomOccupancy_overridesReportedOccupancy() {
    UUID updateId = UUID.randomUUID();
    UUID shelterId = UUID.randomUUID();
    UUID districtId = UUID.randomUUID();
    Shelter shelter = Shelter.create("Shelter X", districtId, "Address", 6.9, 79.8, 200, null);
    shelter.updateOccupancy(50);

    ShelterHeadcountUpdate update =
        ShelterHeadcountUpdate.create(
            shelterId, districtId, 160, 50, "Coordinator", "SHELTER_COORDINATOR", "High intake");

    when(headcountUpdates.findById(updateId)).thenReturn(Optional.of(update));
    when(shelters.findById(shelterId)).thenReturn(Optional.of(shelter));
    ActingUser user = new ActingUser(UUID.randomUUID(), Role.DISTRICT_OFFICER, null, null, null);
    when(actingUser.require()).thenReturn(user);
    when(shelters.save(any(Shelter.class))).thenAnswer(inv -> inv.getArgument(0));

    // Custom occupancy of 170 overrides reported 160
    ShelterDto result = service.applyHeadcountUpdate(updateId, 170);

    assertThat(result.currentOccupancy()).isEqualTo(170);
    assertThat(update.getStatus()).isEqualTo(HeadcountUpdateStatus.APPLIED);
    verify(headcountUpdates).save(update);
  }

  @Test
  void dismissHeadcountUpdate_valid_marksDismissed() {
    UUID updateId = UUID.randomUUID();
    UUID shelterId = UUID.randomUUID();
    UUID districtId = UUID.randomUUID();
    Shelter shelter = Shelter.create("Shelter X", districtId, "Address", 6.9, 79.8, 200, null);
    ShelterHeadcountUpdate update =
        ShelterHeadcountUpdate.create(
            shelterId, districtId, 160, 50, "Coordinator", "SHELTER_COORDINATOR", "High intake");

    when(headcountUpdates.findById(updateId)).thenReturn(Optional.of(update));
    when(shelters.findById(shelterId)).thenReturn(Optional.of(shelter));
    ActingUser user = new ActingUser(UUID.randomUUID(), Role.DISTRICT_OFFICER, null, null, null);
    when(actingUser.require()).thenReturn(user);
    when(headcountUpdates.save(any(ShelterHeadcountUpdate.class))).thenAnswer(inv -> inv.getArgument(0));

    ShelterHeadcountUpdateDto dto = service.dismissHeadcountUpdate(updateId);

    assertThat(dto.status()).isEqualTo(HeadcountUpdateStatus.DISMISSED);
    assertThat(update.getStatus()).isEqualTo(HeadcountUpdateStatus.DISMISSED);
    verify(headcountUpdates).save(update);
  }

  @Test
  void dismissHeadcountUpdate_notFound_throwsException() {
    UUID updateId = UUID.randomUUID();
    when(headcountUpdates.findById(updateId)).thenReturn(Optional.empty());

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.dismissHeadcountUpdate(updateId))
        .isInstanceOf(lk.dmc.disaster.shared.error.AppException.class)
        .hasMessageContaining("Headcount update not found");
  }
}
