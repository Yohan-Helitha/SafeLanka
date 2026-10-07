package lk.dmc.disaster.shared.reference;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
/** View for DisasterEvent. */
public record DisasterEventView(UUID id, String name, UUID hazardTypeId, EventStatus status, OffsetDateTime startedAt, OffsetDateTime endedAt, Set<UUID> districtIds) {
    public DisasterEventView(DisasterEvent e) {
        this(e.getId(), e.getName(), e.getHazardTypeId(), e.getStatus(), e.getStartedAt(), e.getEndedAt(), e.getDistricts().stream().map(District::getId).collect(Collectors.toSet()));
    }
}
