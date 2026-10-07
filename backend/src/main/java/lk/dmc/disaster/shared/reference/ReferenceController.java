package lk.dmc.disaster.shared.reference;

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
