package lk.dmc.disaster.response.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lk.dmc.disaster.response.entity.AssignmentStatus;
import lk.dmc.disaster.response.entity.RescueAssignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RescueAssignmentRepository extends JpaRepository<RescueAssignment, UUID> {

  @Query(
      """
      SELECT a FROM RescueAssignment a
      WHERE (:districtId IS NULL
         OR a.teamId IN (SELECT t.id FROM RescueTeam t WHERE t.districtId = :districtId)
         OR a.destinationShelterId IN (SELECT s.id FROM Shelter s WHERE s.districtId = :districtId))
  """)
  List<RescueAssignment> findByDistrictId(@Param("districtId") UUID districtId);

  @Query(
      """
      SELECT a FROM RescueAssignment a
      WHERE (:districtId IS NULL
         OR a.teamId IN (SELECT t.id FROM RescueTeam t WHERE t.districtId = :districtId)
         OR a.destinationShelterId IN (SELECT s.id FROM Shelter s WHERE s.districtId = :districtId))
  """)
  Page<RescueAssignment> findByDistrictId(@Param("districtId") UUID districtId, Pageable pageable);

  @Query(
      """
      SELECT a FROM RescueAssignment a
      WHERE a.status = :status
        AND (:districtId IS NULL
         OR a.teamId IN (SELECT t.id FROM RescueTeam t WHERE t.districtId = :districtId)
         OR a.destinationShelterId IN (SELECT s.id FROM Shelter s WHERE s.districtId = :districtId))
  """)
  Page<RescueAssignment> findByDistrictIdAndStatus(
      @Param("districtId") UUID districtId,
      @Param("status") AssignmentStatus status,
      Pageable pageable);

  @Query(
      """
      SELECT a FROM RescueAssignment a
      WHERE a.status = :status
        AND (:districtId IS NULL
         OR a.teamId IN (SELECT t.id FROM RescueTeam t WHERE t.districtId = :districtId)
         OR a.destinationShelterId IN (SELECT s.id FROM Shelter s WHERE s.districtId = :districtId))
  """)
  List<RescueAssignment> findByDistrictIdAndStatus(
      @Param("districtId") UUID districtId, @Param("status") AssignmentStatus status);

  Optional<RescueAssignment> findByIdAndTeamId(UUID id, UUID teamId);

  List<RescueAssignment> findByTeamIdAndStatusIn(UUID teamId, List<AssignmentStatus> statuses);
}
