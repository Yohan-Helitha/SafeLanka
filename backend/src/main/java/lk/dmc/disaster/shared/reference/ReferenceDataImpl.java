package lk.dmc.disaster.shared.reference;

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
