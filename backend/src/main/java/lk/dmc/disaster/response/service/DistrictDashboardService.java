package lk.dmc.disaster.response.service;

import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.response.dto.response.AllocationDto;
import lk.dmc.disaster.response.dto.response.AssignmentDto;
import lk.dmc.disaster.response.dto.response.DistrictDashboard;
import lk.dmc.disaster.response.dto.response.ReliefStockDto;
import lk.dmc.disaster.response.dto.response.RescueTeamDto;
import lk.dmc.disaster.response.dto.response.ShelterDto;
import lk.dmc.disaster.response.dto.response.ShelterSuggestionDto;
import org.springframework.data.domain.Page;

public interface DistrictDashboardService {

  DistrictDashboard getDashboard(UUID districtId);

  List<RescueTeamDto> getTeams(UUID districtId, Boolean available);

  Page<AssignmentDto> getAssignments(UUID districtId, String status, int page, int size);

  AssignmentDto getAssignment(UUID id);

  AssignmentDto getMyAssignment();

  List<ShelterDto> getShelters(UUID districtId, Boolean availableOnly);

  List<ShelterSuggestionDto> getShelterSuggestions(UUID districtId, int people);

  List<ReliefStockDto> getStocks(UUID districtId, UUID itemId);

  Page<AllocationDto> getAllocations(UUID shelterId, UUID eventId, int page, int size);
}
