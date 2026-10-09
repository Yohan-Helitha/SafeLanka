package lk.dmc.disaster.response.repository;

import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.response.entity.HeadcountUpdateStatus;
import lk.dmc.disaster.response.entity.ShelterHeadcountUpdate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShelterHeadcountUpdateRepository
    extends JpaRepository<ShelterHeadcountUpdate, UUID> {

  List<ShelterHeadcountUpdate> findByDistrictIdOrderByReportedAtDesc(UUID districtId);

  List<ShelterHeadcountUpdate> findByDistrictIdAndStatusOrderByReportedAtDesc(
      UUID districtId, HeadcountUpdateStatus status);

  List<ShelterHeadcountUpdate> findByShelterIdOrderByReportedAtDesc(UUID shelterId);
}

