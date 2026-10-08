package lk.dmc.disaster.response.repository;

import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.response.entity.AllocationStatus;
import lk.dmc.disaster.response.entity.ResourceAllocation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceAllocationRepository extends JpaRepository<ResourceAllocation, UUID> {

  List<ResourceAllocation> findByShelterId(UUID shelterId);

  List<ResourceAllocation> findByEventId(UUID eventId);

  Page<ResourceAllocation> findByShelterIdOrEventId(
      UUID shelterId, UUID eventId, Pageable pageable);

  List<ResourceAllocation> findByStatusIn(List<AllocationStatus> statuses);
}
