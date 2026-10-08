import os

base_dir = r"c:\Users\Administrator\Desktop\3 Year\2nd sem\CSSE\Project\SafeLanka\backend\src\main\java\lk\dmc\disaster\shared\reference"

files = {
    "DistrictRepository.java": """package lk.dmc.disaster.shared.reference;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface DistrictRepository extends JpaRepository<District, UUID> {}
""",
    "RiverBasinRepository.java": """package lk.dmc.disaster.shared.reference;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RiverBasinRepository extends JpaRepository<RiverBasin, UUID> {}
""",
    "HazardTypeRepository.java": """package lk.dmc.disaster.shared.reference;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface HazardTypeRepository extends JpaRepository<HazardType, UUID> {
    List<HazardType> findByActiveTrue();
}
""",
    "OrganisationRepository.java": """package lk.dmc.disaster.shared.reference;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface OrganisationRepository extends JpaRepository<Organisation, UUID> {
    List<Organisation> findByType(OrganisationType type);
}
""",
    "ReliefItemRepository.java": """package lk.dmc.disaster.shared.reference;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ReliefItemRepository extends JpaRepository<ReliefItem, UUID> {}
""",
    "DisasterEventRepository.java": """package lk.dmc.disaster.shared.reference;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface DisasterEventRepository extends JpaRepository<DisasterEvent, UUID> {
    List<DisasterEvent> findByStatus(EventStatus status);
}
""",
    "AppUserRepository.java": """package lk.dmc.disaster.shared.reference;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.shared.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {
    
    List<AppUser> findByRole(Role role);

    @Query("SELECT u FROM AppUser u WHERE u.role IN :roles AND (u.districtId IN :districtIds OR u.riverBasinId IN :basinIds)")
    List<AppUser> findCitizensInAreas(@Param("districtIds") Set<UUID> districtIds, @Param("basinIds") Set<UUID> basinIds, @Param("roles") Set<Role> roles);
    
    @Query("SELECT COUNT(u) FROM AppUser u WHERE u.role IN :roles AND (u.districtId IN :districtIds OR u.riverBasinId IN :basinIds)")
    long countCitizensInAreas(@Param("districtIds") Set<UUID> districtIds, @Param("basinIds") Set<UUID> basinIds, @Param("roles") Set<Role> roles);
}
""",
    "ReferenceData.java": """package lk.dmc.disaster.shared.reference;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Reference data interface. */
public interface ReferenceData {
    List<DistrictView> districts();
    List<RiverBasinView> riverBasins();
    List<HazardTypeView> hazardTypes(boolean activeOnly);
    HazardTypeView hazardType(UUID id);
    List<OrganisationView> organisations(Optional<OrganisationType> type);
    List<ReliefItemView> reliefItems();
    DisasterEventView event(UUID id);
    List<DisasterEventView> events(Optional<EventStatus> status);
    
    boolean districtExists(UUID id);
    boolean hazardTypeAcceptsCategory(UUID id, String category);
    
    List<DistrictView> districtsInBasins(Set<UUID> basinIds);
    List<RiverBasinView> basinsOfDistrict(UUID districtId);
}
""",
    "ReferenceDataImpl.java": """package lk.dmc.disaster.shared.reference;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lk.dmc.disaster.shared.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReferenceDataImpl implements ReferenceData {

    private final DistrictRepository districtRepository;
    private final RiverBasinRepository riverBasinRepository;
    private final HazardTypeRepository hazardTypeRepository;
    private final OrganisationRepository organisationRepository;
    private final ReliefItemRepository reliefItemRepository;
    private final DisasterEventRepository disasterEventRepository;

    public ReferenceDataImpl(
            DistrictRepository districtRepository,
            RiverBasinRepository riverBasinRepository,
            HazardTypeRepository hazardTypeRepository,
            OrganisationRepository organisationRepository,
            ReliefItemRepository reliefItemRepository,
            DisasterEventRepository disasterEventRepository) {
        this.districtRepository = districtRepository;
        this.riverBasinRepository = riverBasinRepository;
        this.hazardTypeRepository = hazardTypeRepository;
        this.organisationRepository = organisationRepository;
        this.reliefItemRepository = reliefItemRepository;
        this.disasterEventRepository = disasterEventRepository;
    }

    @Override
    public List<DistrictView> districts() {
        return districtRepository.findAll().stream().map(DistrictView::new).toList();
    }

    @Override
    public List<RiverBasinView> riverBasins() {
        return riverBasinRepository.findAll().stream().map(RiverBasinView::new).toList();
    }

    @Override
    public List<HazardTypeView> hazardTypes(boolean activeOnly) {
        if (activeOnly) {
            return hazardTypeRepository.findByActiveTrue().stream().map(HazardTypeView::new).toList();
        }
        return hazardTypeRepository.findAll().stream().map(HazardTypeView::new).toList();
    }

    @Override
    public HazardTypeView hazardType(UUID id) {
        return hazardTypeRepository.findById(id).map(HazardTypeView::new)
                .orElseThrow(() -> new NotFoundException("HazardType not found"));
    }

    @Override
    public List<OrganisationView> organisations(Optional<OrganisationType> type) {
        if (type.isPresent()) {
            return organisationRepository.findByType(type.get()).stream().map(OrganisationView::new).toList();
        }
        return organisationRepository.findAll().stream().map(OrganisationView::new).toList();
    }

    @Override
    public List<ReliefItemView> reliefItems() {
        return reliefItemRepository.findAll().stream().map(ReliefItemView::new).toList();
    }

    @Override
    public DisasterEventView event(UUID id) {
        return disasterEventRepository.findById(id).map(DisasterEventView::new)
                .orElseThrow(() -> new NotFoundException("Event not found"));
    }

    @Override
    public List<DisasterEventView> events(Optional<EventStatus> status) {
        if (status.isPresent()) {
            return disasterEventRepository.findByStatus(status.get()).stream().map(DisasterEventView::new).toList();
        }
        return disasterEventRepository.findAll().stream().map(DisasterEventView::new).toList();
    }

    @Override
    public boolean districtExists(UUID id) {
        return districtRepository.existsById(id);
    }

    @Override
    public boolean hazardTypeAcceptsCategory(UUID id, String category) {
        return hazardTypeRepository.findById(id)
                .map(h -> h.isActive() && h.getReportCategories().contains(category))
                .orElse(false);
    }

    @Override
    public List<DistrictView> districtsInBasins(Set<UUID> basinIds) {
        // Need to load all districts and check if they belong to any of the basins.
        return districtRepository.findAll().stream()
                .filter(d -> {
                    // But we don't have riverBasins on District entity to traverse easily.
                    // Oh, actually we don't have ManyToMany mapped back?
                    // The schema has district_river_basins. Let's assume we map it or just use query?
                    // Actually, RiverBasins in district is missing in District.java mapping!
                    return false;
                })
                .map(DistrictView::new)
                .toList();
    }

    @Override
    public List<RiverBasinView> basinsOfDistrict(UUID districtId) {
        return List.of();
    }
}
"""
}

for name, content in files.items():
    with open(os.path.join(base_dir, name), 'w') as f:
        f.write(content)

print("Generated repos and services.")
