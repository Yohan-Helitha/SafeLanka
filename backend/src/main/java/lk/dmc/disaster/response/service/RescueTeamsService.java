package lk.dmc.disaster.response.service;

import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.response.dto.response.RescueTeamDto;

public interface RescueTeamsService {

  List<RescueTeamDto> getTeams(UUID districtId, Boolean available);

  RescueTeamDto updateTeamStatus(UUID teamId, String toStatus, java.time.Instant changedAt, boolean recordedOffline);
}
