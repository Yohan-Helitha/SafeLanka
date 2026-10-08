package lk.dmc.disaster.response.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ShelterTest {

  @Test
  void create_initializesWithOpenStatusAndZeroOccupancy() {
    UUID districtId = UUID.randomUUID();
    UUID coordId = UUID.randomUUID();

    Shelter shelter =
        Shelter.create(
            "Central College Shelter",
            districtId,
            "12 Galle Road, Colombo 03",
            6.91,
            79.85,
            150,
            coordId);

    assertThat(shelter.getId()).isNotNull();
    assertThat(shelter.getName()).isEqualTo("Central College Shelter");
    assertThat(shelter.getDistrictId()).isEqualTo(districtId);
    assertThat(shelter.getAddress()).isEqualTo("12 Galle Road, Colombo 03");
    assertThat(shelter.getLatitude()).isEqualTo(6.91);
    assertThat(shelter.getLongitude()).isEqualTo(79.85);
    assertThat(shelter.getCapacity()).isEqualTo(150);
    assertThat(shelter.getCurrentOccupancy()).isZero();
    assertThat(shelter.getStatus()).isEqualTo(ShelterStatus.OPEN);
    assertThat(shelter.getCoordinatorId()).isEqualTo(coordId);
    assertThat(shelter.isOpen()).isTrue();
    assertThat(shelter.getAvailableCapacity()).isEqualTo(150);
    assertThat(shelter.getVersion()).isEqualTo(0L);
  }

  @Test
  void updateOccupancy_updatesStatusToOpenOrFull() {
    Shelter shelter =
        Shelter.create("Temple Shelter", UUID.randomUUID(), "Address", 6.9, 79.8, 100, null);

    shelter.updateOccupancy(50);
    assertThat(shelter.getCurrentOccupancy()).isEqualTo(50);
    assertThat(shelter.getAvailableCapacity()).isEqualTo(50);
    assertThat(shelter.getStatus()).isEqualTo(ShelterStatus.OPEN);
    assertThat(shelter.isOpen()).isTrue();

    shelter.updateOccupancy(100);
    assertThat(shelter.getCurrentOccupancy()).isEqualTo(100);
    assertThat(shelter.getAvailableCapacity()).isZero();
    assertThat(shelter.getStatus()).isEqualTo(ShelterStatus.FULL);
    assertThat(shelter.isOpen()).isFalse();

    shelter.updateOccupancy(80);
    assertThat(shelter.getCurrentOccupancy()).isEqualTo(80);
    assertThat(shelter.getStatus()).isEqualTo(ShelterStatus.OPEN);
  }

  @Test
  void updateOccupancy_rejectsNegativeOrExceedingValues() {
    Shelter shelter =
        Shelter.create("Church Shelter", UUID.randomUUID(), "Address", 6.9, 79.8, 50, null);

    assertThatThrownBy(() -> shelter.updateOccupancy(-1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Occupancy cannot be negative");

    assertThatThrownBy(() -> shelter.updateOccupancy(51))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Occupancy exceeds capacity");
  }

  @Test
  void closeAndReopen_changesShelterStatus() {
    Shelter shelter =
        Shelter.create("Community Centre", UUID.randomUUID(), "Address", 6.9, 79.8, 60, null);

    shelter.close();
    assertThat(shelter.getStatus()).isEqualTo(ShelterStatus.CLOSED);
    assertThat(shelter.isOpen()).isFalse();

    shelter.reopen();
    assertThat(shelter.getStatus()).isEqualTo(ShelterStatus.OPEN);
    assertThat(shelter.isOpen()).isTrue();
  }
}
