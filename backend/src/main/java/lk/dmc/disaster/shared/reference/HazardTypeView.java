package lk.dmc.disaster.shared.reference;
import java.util.List;
import java.util.UUID;
/** View for HazardType. */
public record HazardTypeView(UUID id, String code, String name, OnsetSpeed onsetSpeed, List<String> reportCategories, boolean active) {
    public HazardTypeView(HazardType h) {
        this(h.getId(), h.getCode(), h.getName(), h.getOnsetSpeed(), h.getReportCategories(), h.isActive());
    }
}
