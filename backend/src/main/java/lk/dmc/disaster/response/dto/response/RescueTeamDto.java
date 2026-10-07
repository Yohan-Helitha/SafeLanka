package lk.dmc.disaster.response.dto.response;

import java.util.UUID;
import lk.dmc.disaster.response.entity.RescueTeam;
import lk.dmc.disaster.response.entity.RescueTeamStatus;
import lk.dmc.disaster.response.entity.TeamType;

public record RescueTeamDto(
    UUID id,
    String name,
    UUID organisationId,
    UUID districtId,
    TeamType teamType,
    int capacity,
    RescueTeamStatus status,
    java.time.Instant lastStatusAt,
    long version) {

  public static RescueTeamDto from(RescueTeam team) {
    return new RescueTeamDto(
        team.getId(), team.getName(), team.getOrganisationId(), team.getDistrictId(),
        team.getTeamType(), team.getCapacity(), team.getStatus(),
        team.getLastStatusAt(), team.getVersion());
  }
}
