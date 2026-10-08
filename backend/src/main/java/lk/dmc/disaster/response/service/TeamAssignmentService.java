package lk.dmc.disaster.response.service;

import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.response.dto.response.AssignmentDto;
import org.springframework.data.domain.Page;

public interface TeamAssignmentService {

  AssignmentDto createAssignment(CreateAssignmentCommand command);

  AssignmentDto assignTeam(UUID assignmentId, UUID teamId);

  AssignmentDto cancelAssignment(UUID assignmentId, String reason);

  AssignmentDto respond(UUID assignmentId, boolean accept, String declineReason);

  AssignmentDto getAssignment(UUID id);

  AssignmentDto getMyAssignment();

  List<AssignmentDto> listAssignments(UUID districtId, String status);

  Page<AssignmentDto> getAssignments(UUID districtId, String status, int page, int size);

  record CreateAssignmentCommand(
      UUID eventId,
      UUID warningId,
      UUID districtId,
      double latitude,
      double longitude,
      String locationText,
      String task,
      short priority,
      int peopleEstimated,
      UUID destinationShelterId,
      UUID teamId,
      UUID createdBy) {}
}
