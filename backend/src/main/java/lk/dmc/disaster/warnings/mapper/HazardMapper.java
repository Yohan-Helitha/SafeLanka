package lk.dmc.disaster.warnings.mapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import lk.dmc.disaster.warnings.dto.EvidenceResponse;
import lk.dmc.disaster.warnings.dto.GaugeResponse;
import lk.dmc.disaster.warnings.dto.HazardDetail;
import lk.dmc.disaster.warnings.dto.HazardListItem;
import lk.dmc.disaster.warnings.dto.HazardWarningRef;
import lk.dmc.disaster.warnings.dto.LatestReadingResponse;
import lk.dmc.disaster.warnings.dto.ReadingResponse;
import lk.dmc.disaster.warnings.entity.Hazard;
import lk.dmc.disaster.warnings.entity.Sensor;
import lk.dmc.disaster.warnings.entity.Warning;
import lk.dmc.disaster.warnings.integration.HazardTypeDirectory;
import lk.dmc.disaster.warnings.integration.VerifiedReportSummary;
import lk.dmc.disaster.warnings.service.GaugeHistory;
import lk.dmc.disaster.warnings.service.GaugeReading;
import lk.dmc.disaster.warnings.service.HazardDetailView;
import lk.dmc.disaster.warnings.service.HazardListEntry;
import org.springframework.stereotype.Component;

/** Turns hazard service results into response shapes. Holds no business rules. */
@Component
public class HazardMapper {

  private final HazardTypeDirectory hazardTypes;

  public HazardMapper(HazardTypeDirectory hazardTypes) {
    this.hazardTypes = hazardTypes;
  }

  /** The hazard list, looking the type codes up once for the whole list. */
  public List<HazardListItem> toListItems(List<HazardListEntry> entries) {
    Map<UUID, String> codes = hazardTypes.codesById();
    return entries.stream().map(entry -> toListItem(entry, codes)).toList();
  }

  public HazardDetail toDetail(HazardDetailView view) {
    HazardListItem item = toListItem(view.summary(), hazardTypes.codesById());
    return new HazardDetail(
        item.id(),
        item.hazardTypeId(),
        item.hazardTypeCode(),
        item.severity(),
        item.districtId(),
        item.riverBasinId(),
        item.description(),
        item.source(),
        item.status(),
        item.detectedAt(),
        item.verifiedReportCount(),
        item.latestReading(),
        view.evidence().stream().map(HazardMapper::toEvidence).toList(),
        toGauge(view.gauge()),
        view.warnings().stream().map(HazardMapper::toWarningRef).toList());
  }

  private static HazardListItem toListItem(HazardListEntry entry, Map<UUID, String> codes) {
    Hazard hazard = entry.hazard();
    return new HazardListItem(
        hazard.getId(),
        hazard.getHazardTypeId(),
        codes.get(hazard.getHazardTypeId()),
        hazard.getSeverity(),
        hazard.getDistrictId(),
        hazard.getRiverBasinId(),
        hazard.getDescription(),
        hazard.getSource(),
        hazard.getStatus(),
        hazard.getDetectedAt(),
        entry.verifiedReportCount(),
        toLatestReading(entry.latestReading()));
  }

  private static LatestReadingResponse toLatestReading(GaugeReading gauge) {
    if (gauge == null) {
      return null;
    }
    return new LatestReadingResponse(
        gauge.reading().getValue(),
        gauge.sensor().getUnit(),
        gauge.aboveAlert(),
        gauge.reading().getRecordedAt());
  }

  private static GaugeResponse toGauge(GaugeHistory history) {
    if (history == null) {
      return null;
    }
    Sensor sensor = history.sensor();
    return new GaugeResponse(
        sensor.getId(),
        sensor.getCode(),
        sensor.getName(),
        sensor.getAlertLevel(),
        sensor.getMajorFloodLevel(),
        sensor.getUnit(),
        history.readings().stream().map(ReadingResponse::from).toList());
  }

  private static EvidenceResponse toEvidence(VerifiedReportSummary report) {
    return new EvidenceResponse(
        report.id(),
        report.referenceNo(),
        report.category(),
        report.description(),
        report.districtId(),
        report.capturedAt());
  }

  private static HazardWarningRef toWarningRef(Warning warning) {
    return new HazardWarningRef(
        warning.getId(), warning.getLevel(), warning.getStatus(), warning.getIssuedAt());
  }
}
