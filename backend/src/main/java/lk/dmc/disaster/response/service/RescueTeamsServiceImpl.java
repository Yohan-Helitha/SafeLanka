package lk.dmc.disaster.response.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.response.dto.response.RescueTeamDto;
import lk.dmc.disaster.response.entity.RescueTeam;
import lk.dmc.disaster.response.entity.RescueTeamStatus;
import lk.dmc.disaster.response.entity.TeamStatusLog;
import lk.dmc.disaster.response.repository.RescueTeamRepository;
import lk.dmc.disaster.response.repository.TeamStatusLogRepository;
import lk.dmc.disaster.response.validation.TeamAssignmentScreenValidator;
import lk.dmc.disaster.shared.actor.ActingUser;
import lk.dmc.disaster.shared.actor.ActingUserContext;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RescueTeamsServiceImpl implements RescueTeamsService {

  private final RescueTeamRepository teams;
  private final TeamStatusLogRepository statusLogs;
  private final ActingUserContext actingUser;
  private final TeamAssignmentScreenValidator validator;

  public RescueTeamsServiceImpl(
      RescueTeamRepository teams,
      TeamStatusLogRepository statusLogs,
      ActingUserContext actingUser,
      TeamAssignmentScreenValidator validator) {
    this.teams = teams;
    this.statusLogs = statusLogs;
    this.actingUser = actingUser;
    this.validator = validator;
  }

  @Override
  @Transactional(readOnly = true)
  public List<RescueTeamDto> getTeams(UUID districtId, Boolean available) {
    if (districtId != null) {
      if (Boolean.TRUE.equals(available)) {
        return teams.findByDistrictIdAndStatus(districtId, RescueTeamStatus.AVAILABLE).stream()
            .map(RescueTeamDto::from)
            .toList();
      }
      return teams.findByDistrictId(districtId).stream().map(RescueTeamDto::from).toList();
    }

    if (Boolean.TRUE.equals(available)) {
      return teams.findAll().stream()
          .filter(t -> t.getStatus() == RescueTeamStatus.AVAILABLE)
          .map(RescueTeamDto::from)
          .toList();
    }
    return teams.findAll().stream().map(RescueTeamDto::from).toList();
  }

  @Override
  public RescueTeamDto updateTeamStatus(
      UUID teamId, String toStatus, Instant changedAt, boolean recordedOffline) {
    RescueTeam team =
        teams
            .findById(teamId)
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Team not found"));

    var user = actingUser != null ? actingUser.require() : null;
    validator.validateStatusUpdate(
        team,
        new lk.dmc.disaster.response.dto.request.TeamStatusUpdateRequest(
            toStatus, null, changedAt, recordedOffline),
        user);

    RescueTeamStatus targetStatus = RescueTeamStatus.valueOf(toStatus);
    String fromStatus = team.getStatus().name();
    team.updateStatus(targetStatus);
    RescueTeam saved = teams.save(team);

    UUID changedBy =
        actingUser != null ? actingUser.current().map(ActingUser::id).orElse(null) : null;

    TeamStatusLog log =
        TeamStatusLog.create(
            teamId,
            null,
            fromStatus,
            targetStatus.name(),
            changedBy,
            changedAt != null ? changedAt : Instant.now(),
            recordedOffline,
            null);
    statusLogs.save(log);

    return RescueTeamDto.from(saved);
  }
}
