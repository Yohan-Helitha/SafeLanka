package lk.dmc.disaster.response.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lk.dmc.disaster.response.dto.response.RescueTeamDto;
import lk.dmc.disaster.response.entity.RescueTeam;
import lk.dmc.disaster.response.entity.RescueTeamStatus;
import lk.dmc.disaster.response.entity.TeamStatusLog;
import lk.dmc.disaster.response.entity.TeamType;
import lk.dmc.disaster.response.repository.RescueTeamRepository;
import lk.dmc.disaster.response.repository.TeamStatusLogRepository;
import lk.dmc.disaster.response.validation.TeamAssignmentScreenValidator;
import lk.dmc.disaster.shared.actor.ActingUser;
import lk.dmc.disaster.shared.actor.ActingUserContext;
import lk.dmc.disaster.shared.domain.Role;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RescueTeamsServiceImplTest {

  @Mock private RescueTeamRepository teams;
  @Mock private TeamStatusLogRepository statusLogs;
  @Mock private ActingUserContext actingUser;
  @Mock private TeamAssignmentScreenValidator validator;

  private RescueTeamsServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new RescueTeamsServiceImpl(teams, statusLogs, actingUser, validator);
  }

  @Test
  void getTeams_withDistrictId_filtersByDistrictAndAvailability() {
    UUID districtId = UUID.randomUUID();
    RescueTeam t1 = RescueTeam.create("Team 1", UUID.randomUUID(), districtId, TeamType.BOAT, 6);
    RescueTeam t2 = RescueTeam.create("Team 2", UUID.randomUUID(), districtId, TeamType.MEDICAL, 4);

    when(teams.findByDistrictIdAndStatus(districtId, RescueTeamStatus.AVAILABLE))
        .thenReturn(List.of(t1));
    when(teams.findByDistrictId(districtId)).thenReturn(List.of(t1, t2));

    List<RescueTeamDto> availableOnly = service.getTeams(districtId, true);
    assertThat(availableOnly).hasSize(1);
    assertThat(availableOnly.get(0).name()).isEqualTo("Team 1");

    List<RescueTeamDto> allDistrict = service.getTeams(districtId, false);
    assertThat(allDistrict).hasSize(2);
  }

  @Test
  void getTeams_withoutDistrictId_returnsAllOrAvailableTeams() {
    RescueTeam t1 =
        RescueTeam.create("Team 1", UUID.randomUUID(), UUID.randomUUID(), TeamType.BOAT, 6);
    RescueTeam t2 =
        RescueTeam.create("Team 2", UUID.randomUUID(), UUID.randomUUID(), TeamType.MEDICAL, 4);
    t2.markDispatched();

    when(teams.findAll()).thenReturn(List.of(t1, t2));

    List<RescueTeamDto> availableOnly = service.getTeams(null, true);
    assertThat(availableOnly).hasSize(1);
    assertThat(availableOnly.get(0).name()).isEqualTo("Team 1");

    List<RescueTeamDto> all = service.getTeams(null, false);
    assertThat(all).hasSize(2);
  }

  @Test
  void updateTeamStatus_teamNotFound_throwsNotFound() {
    UUID teamId = UUID.randomUUID();
    when(teams.findById(teamId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.updateTeamStatus(teamId, "ACTIVE", Instant.now(), false))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo(ErrorCode.NOT_FOUND);
  }

  @Test
  void updateTeamStatus_valid_updatesTeamLogsStatusAndReturnsDto() {
    UUID teamId = UUID.randomUUID();
    RescueTeam team =
        RescueTeam.create("Team 1", UUID.randomUUID(), UUID.randomUUID(), TeamType.BOAT, 6);
    when(teams.findById(teamId)).thenReturn(Optional.of(team));
    when(teams.save(any(RescueTeam.class))).thenAnswer(inv -> inv.getArgument(0));

    UUID userId = UUID.randomUUID();
    ActingUser user = new ActingUser(userId, Role.RESCUE_MEMBER, null, null, teamId);
    when(actingUser.require()).thenReturn(user);
    when(actingUser.current()).thenReturn(Optional.of(user));

    Instant changedAt = Instant.now().minusSeconds(10);
    RescueTeamDto result = service.updateTeamStatus(teamId, "ACTIVE", changedAt, true);

    assertThat(result).isNotNull();
    assertThat(result.status()).isEqualTo(RescueTeamStatus.ACTIVE);
    assertThat(team.getStatus()).isEqualTo(RescueTeamStatus.ACTIVE);

    verify(validator).validateStatusUpdate(eq(team), any(), eq(user));
    verify(teams).save(team);

    ArgumentCaptor<TeamStatusLog> logCaptor = ArgumentCaptor.forClass(TeamStatusLog.class);
    verify(statusLogs).save(logCaptor.capture());
    TeamStatusLog savedLog = logCaptor.getValue();
    assertThat(savedLog.getTeamId()).isEqualTo(teamId);
    assertThat(savedLog.getFromStatus()).isEqualTo("AVAILABLE");
    assertThat(savedLog.getToStatus()).isEqualTo("ACTIVE");
    assertThat(savedLog.getChangedBy()).isEqualTo(userId);
    assertThat(savedLog.getChangedAt()).isEqualTo(changedAt);
    assertThat(savedLog.isRecordedOffline()).isTrue();

    // Test with null changedAt and null actingUser
    RescueTeamsServiceImpl serviceNoUser =
        new RescueTeamsServiceImpl(teams, statusLogs, null, validator);
    RescueTeamDto result2 = serviceNoUser.updateTeamStatus(teamId, "AVAILABLE", null, false);
    assertThat(result2.status()).isEqualTo(RescueTeamStatus.AVAILABLE);
  }

  @Test
  void getTeams_nullAvailability() {
    UUID districtId = UUID.randomUUID();
    RescueTeam t = RescueTeam.create("Team", UUID.randomUUID(), districtId, TeamType.BOAT, 5);
    when(teams.findByDistrictId(districtId)).thenReturn(List.of(t));
    when(teams.findAll()).thenReturn(List.of(t));

    assertThat(service.getTeams(districtId, null)).hasSize(1);
    assertThat(service.getTeams(null, null)).hasSize(1);
  }
}
