package lk.dmc.disaster.response.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ShelterHeadcountUpdateTest {

  @Test
  @DisplayName("create initializes correctly with PENDING status and timestamps")
  void create_initializesCorrectly() {
    UUID shelterId = UUID.randomUUID();
    UUID districtId = UUID.randomUUID();

    ShelterHeadcountUpdate update =
        ShelterHeadcountUpdate.create(
            shelterId,
            districtId,
            120,
            80,
            "Kamal Perera",
            "SHELTER_COORDINATOR",
            "New evacuees arrived from river bank");

    assertThat(update.getId()).isNotNull();
    assertThat(update.getShelterId()).isEqualTo(shelterId);
    assertThat(update.getDistrictId()).isEqualTo(districtId);
    assertThat(update.getReportedOccupancy()).isEqualTo(120);
    assertThat(update.getPreviousOccupancy()).isEqualTo(80);
    assertThat(update.getReportedByName()).isEqualTo("Kamal Perera");
    assertThat(update.getReportedByRole()).isEqualTo("SHELTER_COORDINATOR");
    assertThat(update.getMessage()).isEqualTo("New evacuees arrived from river bank");
    assertThat(update.getStatus()).isEqualTo(HeadcountUpdateStatus.PENDING);
    assertThat(update.getReportedAt()).isNotNull();
    assertThat(update.getProcessedAt()).isNull();
    assertThat(update.getProcessedBy()).isNull();
  }

  @Test
  @DisplayName("create falls back to SHELTER_COORDINATOR when role is null")
  void create_nullRoleDefaultsToCoordinator() {
    ShelterHeadcountUpdate update =
        ShelterHeadcountUpdate.create(
            UUID.randomUUID(), UUID.randomUUID(), 50, null, "Field Volunteer", null, "Headcount report");

    assertThat(update.getReportedByRole()).isEqualTo("SHELTER_COORDINATOR");
  }

  @Test
  @DisplayName("markApplied transitions status to APPLIED and records processor and timestamp")
  void markApplied_updatesStatusAndProcessor() {
    ShelterHeadcountUpdate update =
        ShelterHeadcountUpdate.create(
            UUID.randomUUID(), UUID.randomUUID(), 100, 50, "Lead", "VOLUNTEER", "Headcount update");

    UUID officerId = UUID.randomUUID();
    Instant before = Instant.now();
    update.markApplied(officerId);

    assertThat(update.getStatus()).isEqualTo(HeadcountUpdateStatus.APPLIED);
    assertThat(update.getProcessedBy()).isEqualTo(officerId);
    assertThat(update.getProcessedAt()).isNotNull();
    assertThat(update.getProcessedAt()).isAfterOrEqualTo(before);
  }

  @Test
  @DisplayName("markDismissed transitions status to DISMISSED and records processor and timestamp")
  void markDismissed_updatesStatusAndProcessor() {
    ShelterHeadcountUpdate update =
        ShelterHeadcountUpdate.create(
            UUID.randomUUID(), UUID.randomUUID(), 100, 50, "Lead", "VOLUNTEER", "Spurious report");

    UUID officerId = UUID.randomUUID();
    Instant before = Instant.now();
    update.markDismissed(officerId);

    assertThat(update.getStatus()).isEqualTo(HeadcountUpdateStatus.DISMISSED);
    assertThat(update.getProcessedBy()).isEqualTo(officerId);
    assertThat(update.getProcessedAt()).isNotNull();
    assertThat(update.getProcessedAt()).isAfterOrEqualTo(before);
  }
}

