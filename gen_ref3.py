import os

base_dir = r"c:\Users\Administrator\Desktop\3 Year\2nd sem\CSSE\Project\SafeLanka\backend\src\main\java\lk\dmc\disaster\shared\reference"

files = {
    "District.java": """package lk.dmc.disaster.shared.reference;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.Set;
import lk.dmc.disaster.shared.domain.BaseEntity;

/** District entity. */
@Entity
@Table(name = "districts")
public class District extends BaseEntity {
    private String code;
    private String name;
    private String province;
    
    @ManyToMany
    @JoinTable(
        name = "district_river_basins",
        joinColumns = @JoinColumn(name = "district_id"),
        inverseJoinColumns = @JoinColumn(name = "river_basin_id")
    )
    private Set<RiverBasin> riverBasins;
    
    protected District() {}

    public String getCode() { return code; }
    public String getName() { return name; }
    public String getProvince() { return province; }
    public Set<RiverBasin> getRiverBasins() { return riverBasins; }
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
        return districtRepository.findAll().stream()
                .filter(d -> d.getRiverBasins().stream().anyMatch(b -> basinIds.contains(b.getId())))
                .map(DistrictView::new)
                .toList();
    }

    @Override
    public List<RiverBasinView> basinsOfDistrict(UUID districtId) {
        return districtRepository.findById(districtId)
                .map(d -> d.getRiverBasins().stream().map(RiverBasinView::new).toList())
                .orElse(List.of());
    }
}
""",
    "UserDirectory.java": """package lk.dmc.disaster.shared.reference;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.shared.error.NotFoundException;

/** User directory interface. */
public interface UserDirectory {
    Optional<UserView> findById(UUID id);
    
    default UserView require(UUID id) {
        return findById(id).orElseThrow(() -> new NotFoundException("User not found"));
    }
    
    List<CitizenContact> findCitizensInAreas(Set<UUID> districtIds, Set<UUID> basinIds);
    long countCitizensInAreas(Set<UUID> districtIds, Set<UUID> basinIds);
}
""",
    "UserDirectoryImpl.java": """package lk.dmc.disaster.shared.reference;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.shared.domain.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserDirectoryImpl implements UserDirectory {

    private final AppUserRepository appUserRepository;

    public UserDirectoryImpl(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    public Optional<UserView> findById(UUID id) {
        return appUserRepository.findById(id).map(UserView::new);
    }

    @Override
    public List<CitizenContact> findCitizensInAreas(Set<UUID> districtIds, Set<UUID> basinIds) {
        return appUserRepository.findCitizensInAreas(districtIds, basinIds, Set.of(Role.CITIZEN, Role.VOLUNTEER))
                .stream().map(CitizenContact::new).toList();
    }

    @Override
    public long countCitizensInAreas(Set<UUID> districtIds, Set<UUID> basinIds) {
        return appUserRepository.countCitizensInAreas(districtIds, basinIds, Set.of(Role.CITIZEN, Role.VOLUNTEER));
    }
}
""",
    "ReferenceController.java": """package lk.dmc.disaster.shared.reference;

import java.util.List;
import java.util.Optional;
import lk.dmc.disaster.shared.domain.Role;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reference")
public class ReferenceController {

    private final ReferenceData referenceData;
    private final AppUserRepository appUserRepository;

    public ReferenceController(ReferenceData referenceData, AppUserRepository appUserRepository) {
        this.referenceData = referenceData;
        this.appUserRepository = appUserRepository;
    }

    @GetMapping("/districts")
    public List<DistrictView> getDistricts() {
        return referenceData.districts();
    }

    @GetMapping("/river-basins")
    public List<RiverBasinView> getRiverBasins() {
        return referenceData.riverBasins();
    }

    @GetMapping("/hazard-types")
    public List<HazardTypeView> getHazardTypes(@RequestParam(defaultValue = "false") boolean activeOnly) {
        return referenceData.hazardTypes(activeOnly);
    }

    @GetMapping("/organisations")
    public List<OrganisationView> getOrganisations(@RequestParam Optional<OrganisationType> type) {
        return referenceData.organisations(type);
    }

    @GetMapping("/relief-items")
    public List<ReliefItemView> getReliefItems() {
        return referenceData.reliefItems();
    }

    @GetMapping("/events")
    public List<DisasterEventView> getEvents(@RequestParam Optional<EventStatus> status) {
        return referenceData.events(status);
    }

    @GetMapping("/users")
    public List<UserView> getUsers(@RequestParam Role role) {
        return appUserRepository.findByRole(role).stream().map(UserView::new).toList();
    }
}
"""
}

for name, content in files.items():
    with open(os.path.join(base_dir, name), 'w') as f:
        f.write(content)

print("Generated gen_ref3 updates.")
