package lk.dmc.disaster.response.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lk.dmc.disaster.response.dto.request.CreateHeadcountUpdateRequest;
import lk.dmc.disaster.response.dto.request.OccupancyUpdateRequest;
import lk.dmc.disaster.response.dto.response.ShelterDto;
import lk.dmc.disaster.response.dto.response.ShelterHeadcountUpdateDto;
import lk.dmc.disaster.response.dto.response.ShelterSuggestionDto;
import lk.dmc.disaster.response.entity.ActivityLog;
import lk.dmc.disaster.response.entity.ActivityType;
import lk.dmc.disaster.response.entity.HeadcountUpdateStatus;
import lk.dmc.disaster.response.entity.OccupancyLog;
import lk.dmc.disaster.response.entity.Shelter;
import lk.dmc.disaster.response.entity.ShelterHeadcountUpdate;
import lk.dmc.disaster.response.entity.ShelterStatus;
import lk.dmc.disaster.response.repository.ActivityLogRepository;
import lk.dmc.disaster.response.repository.OccupancyLogRepository;
import lk.dmc.disaster.response.repository.ShelterHeadcountUpdateRepository;
import lk.dmc.disaster.response.repository.ShelterRepository;
import lk.dmc.disaster.response.validation.DistrictSheltersScreenValidator;
import lk.dmc.disaster.shared.actor.ActingUserContext;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DistrictSheltersServiceImpl implements DistrictSheltersService {

  private final ShelterRepository shelters;
  private final ShelterHeadcountUpdateRepository headcountUpdates;
  private final OccupancyLogRepository occupancyLogs;
  private final ActivityLogRepository activityLogs;
  private final ActingUserContext actingUser;
  private final DistrictSheltersScreenValidator validator;

  public DistrictSheltersServiceImpl(
      ShelterRepository shelters,
      ShelterHeadcountUpdateRepository headcountUpdates,
      OccupancyLogRepository occupancyLogs,
      ActivityLogRepository activityLogs,
      ActingUserContext actingUser,
      DistrictSheltersScreenValidator validator) {
    this.shelters = shelters;
    this.headcountUpdates = headcountUpdates;
    this.occupancyLogs = occupancyLogs;
    this.activityLogs = activityLogs;
    this.actingUser = actingUser;
    this.validator = validator;
  }

  @Override
  @Transactional(readOnly = true)
  public List<ShelterDto> getShelters(UUID districtId, Boolean availableOnly) {
    if (districtId == null) {
      if (availableOnly != null && availableOnly) {
        return shelters.findAll().stream()
            .filter(s -> s.getStatus() == ShelterStatus.OPEN)
            .map(ShelterDto::from)
            .toList();
      }
      return shelters.findAll().stream().map(ShelterDto::from).toList();
    }

    if (availableOnly != null && availableOnly) {
      return shelters.findByDistrictIdAndStatus(districtId, ShelterStatus.OPEN).stream()
          .map(ShelterDto::from)
          .toList();
    }
    return shelters.findByDistrictId(districtId).stream().map(ShelterDto::from).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<ShelterSuggestionDto> getSuggestions(UUID districtId, int people) {
    return shelters
        .findByDistrictIdAndStatusAndCurrentOccupancyLessThan(
            districtId, ShelterStatus.OPEN, Integer.MAX_VALUE)
        .stream()
        .filter(s -> s.getAvailableCapacity() >= people)
        .sorted((a, b) -> Integer.compare(b.getAvailableCapacity(), a.getAvailableCapacity()))
        .map(s -> ShelterSuggestionDto.from(s, 0))
        .toList();
  }

  @Override
  public ShelterDto updateOccupancy(UUID shelterId, int occupancy) {
    Shelter shelter =
        shelters
            .findById(shelterId)
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Shelter not found"));

    var user = actingUser.require();
    validator.validateOccupancyUpdate(shelter, new OccupancyUpdateRequest(occupancy), user);

    int prevOccupancy = shelter.getCurrentOccupancy();
    int delta = occupancy - prevOccupancy;

    shelter.updateOccupancy(occupancy);
    Shelter saved = shelters.save(shelter);

    occupancyLogs.save(
        OccupancyLog.create(
            shelter.getId(), null, occupancy, delta, user.id(), Instant.now()));

    activityLogs.save(
        ActivityLog.create(
            shelter.getDistrictId(),
            null,
            ActivityType.SHELTER,
            "Shelter headcount updated: "
                + shelter.getName()
                + " now at "
                + occupancy
                + "/"
                + shelter.getCapacity()
                + " occupants",
            Instant.now()));

    return ShelterDto.from(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ShelterHeadcountUpdateDto> getHeadcountUpdates(
      UUID districtId, HeadcountUpdateStatus status) {
    List<ShelterHeadcountUpdate> list;
    if (districtId != null && status != null) {
      list = headcountUpdates.findByDistrictIdAndStatusOrderByReportedAtDesc(districtId, status);
    } else if (districtId != null) {
      list = headcountUpdates.findByDistrictIdOrderByReportedAtDesc(districtId);
    } else {
      list = headcountUpdates.findAll();
    }

    Map<UUID, Shelter> shelterMap =
        shelters.findAll().stream().collect(Collectors.toMap(Shelter::getId, s -> s));

    return list.stream()
        .map(u -> ShelterHeadcountUpdateDto.from(u, shelterMap.get(u.getShelterId())))
        .toList();
  }

  @Override
  public ShelterDto applyHeadcountUpdate(UUID updateId, Integer customOccupancy) {
    ShelterHeadcountUpdate update =
        headcountUpdates
            .findById(updateId)
            .orElseThrow(
                () -> new AppException(ErrorCode.NOT_FOUND, "Headcount update not found"));

    if (update.getStatus() != HeadcountUpdateStatus.PENDING) {
      throw new AppException(
          ErrorCode.CONFLICT, "Headcount update has already been processed or dismissed");
    }

    Shelter shelter =
        shelters
            .findById(update.getShelterId())
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Shelter not found"));

    var user = actingUser.require();
    int finalOccupancy =
        customOccupancy != null ? customOccupancy : update.getReportedOccupancy();

    validator.validateOccupancyUpdate(
        shelter, new OccupancyUpdateRequest(finalOccupancy), user);

    int delta = finalOccupancy - shelter.getCurrentOccupancy();
    shelter.updateOccupancy(finalOccupancy);
    Shelter saved = shelters.save(shelter);

    update.markApplied(user.id());
    headcountUpdates.save(update);

    occupancyLogs.save(
        OccupancyLog.create(
            shelter.getId(), null, finalOccupancy, delta, user.id(), Instant.now()));

    activityLogs.save(
        ActivityLog.create(
            shelter.getDistrictId(),
            null,
            ActivityType.SHELTER,
            "Headcount verified & applied: "
                + shelter.getName()
                + " set to "
                + finalOccupancy
                + "/"
                + shelter.getCapacity()
                + " (reported by "
                + update.getReportedByName()
                + ")",
            Instant.now()));

    return ShelterDto.from(saved);
  }

  @Override
  public ShelterHeadcountUpdateDto dismissHeadcountUpdate(UUID updateId) {
    ShelterHeadcountUpdate update =
        headcountUpdates
            .findById(updateId)
            .orElseThrow(
                () -> new AppException(ErrorCode.NOT_FOUND, "Headcount update not found"));

    var user = actingUser.require();
    update.markDismissed(user.id());
    ShelterHeadcountUpdate saved = headcountUpdates.save(update);

    Shelter shelter = shelters.findById(update.getShelterId()).orElse(null);
    return ShelterHeadcountUpdateDto.from(saved, shelter);
  }

  @Override
  public ShelterHeadcountUpdateDto createHeadcountUpdate(
      CreateHeadcountUpdateRequest request) {
    Shelter shelter =
        shelters
            .findById(request.shelterId())
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Shelter not found"));

    var user = actingUser.current().orElse(null);
    String reporterName =
        request.reportedByName() != null && !request.reportedByName().isBlank()
            ? request.reportedByName()
            : user != null ? user.role().name() : "Field Report";
    String reporterRole =
        request.reportedByRole() != null && !request.reportedByRole().isBlank()
            ? request.reportedByRole()
            : user != null ? user.role().name() : "FIELD_OFFICER";

    ShelterHeadcountUpdate update =
        ShelterHeadcountUpdate.create(
            shelter.getId(),
            shelter.getDistrictId(),
            request.reportedOccupancy(),
            shelter.getCurrentOccupancy(),
            reporterName,
            reporterRole,
            request.message());

    ShelterHeadcountUpdate saved = headcountUpdates.save(update);

    activityLogs.save(
        ActivityLog.create(
            shelter.getDistrictId(),
            null,
            ActivityType.SHELTER,
            "Incoming headcount update: "
                + request.reportedOccupancy()
                + " occupants reported for "
                + shelter.getName(),
            Instant.now()));

    return ShelterHeadcountUpdateDto.from(saved, shelter);
  }
}
