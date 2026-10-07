package lk.dmc.disaster.shared.reference;

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
