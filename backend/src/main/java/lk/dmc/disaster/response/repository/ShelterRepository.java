package lk.dmc.disaster.response.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lk.dmc.disaster.response.entity.Shelter;
import lk.dmc.disaster.response.entity.ShelterStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShelterRepository extends JpaRepository<Shelter, UUID> {

  List<Shelter> findByDistrictId(UUID districtId);

  List<Shelter> findByDistrictIdAndStatus(UUID districtId, ShelterStatus status);

  Optional<Shelter> findByIdAndDistrictId(UUID id, UUID districtId);

  List<Shelter> findByDistrictIdAndStatusAndCurrentOccupancyLessThan(
      UUID districtId, ShelterStatus status, int currentOccupancy);
}
