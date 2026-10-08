package lk.dmc.disaster.response.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class RescueTeamTest {

  @Test
  void create_initializesWithExpectedDefaults() {
    UUID orgId = UUID.randomUUID();
    UUID districtId = UUID.randomUUID();

    RescueTeam team = RescueTeam.create("Colombo Alpha", orgId, districtId, TeamType.BOAT, 6);

    assertThat(team.getId()).isNotNull();
    assertThat(team.getName()).isEqualTo("Colombo Alpha");
    assertThat(team.getOrganisationId()).isEqualTo(orgId);
    assertThat(team.getDistrictId()).isEqualTo(districtId);
    assertThat(team.getTeamType()).isEqualTo(TeamType.BOAT);
    assertThat(team.getCapacity()).isEqualTo(6);
    assertThat(team.getStatus()).isEqualTo(RescueTeamStatus.AVAILABLE);
    assertThat(team.getLastStatusAt()).isNotNull();
    assertThat(team.getVersion()).isEqualTo(0L);
  }

  @Test
  void statusTransitions_updateStatusAndTimestamp() {
    RescueTeam team =
        RescueTeam.create("Galle Bravo", UUID.randomUUID(), UUID.randomUUID(), TeamType.MEDICAL, 4);

    team.markDispatched();
    assertThat(team.getStatus()).isEqualTo(RescueTeamStatus.DISPATCHED);

    team.markEnRoute();
    assertThat(team.getStatus()).isEqualTo(RescueTeamStatus.EN_ROUTE);

    team.markActive();
    assertThat(team.getStatus()).isEqualTo(RescueTeamStatus.ACTIVE);

    team.markCompleted();
    assertThat(team.getStatus()).isEqualTo(RescueTeamStatus.AVAILABLE);

    team.markOfflineUnknown();
    assertThat(team.getStatus()).isEqualTo(RescueTeamStatus.OFFLINE_UNKNOWN);

    team.updateStatus(RescueTeamStatus.ACTIVE);
    assertThat(team.getStatus()).isEqualTo(RescueTeamStatus.ACTIVE);
  }
}
