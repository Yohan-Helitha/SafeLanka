package lk.dmc.disaster.warnings.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.shared.domain.WarningLevel;
import lk.dmc.disaster.warnings.dto.HazardDetail;
import lk.dmc.disaster.warnings.dto.HazardListItem;
import lk.dmc.disaster.warnings.entity.Hazard;
import lk.dmc.disaster.warnings.entity.HazardArea;
import lk.dmc.disaster.warnings.entity.Sensor;
import lk.dmc.disaster.warnings.entity.SensorReading;
import lk.dmc.disaster.warnings.entity.TargetType;
import lk.dmc.disaster.warnings.entity.Warning;
import lk.dmc.disaster.warnings.entity.WarningContent;
import lk.dmc.disaster.warnings.entity.WarningDraft;
import lk.dmc.disaster.warnings.entity.WarningTarget;
import lk.dmc.disaster.warnings.integration.HazardTypeDirectory;
import lk.dmc.disaster.warnings.integration.VerifiedReportSummary;
import lk.dmc.disaster.warnings.service.GaugeHistory;
import lk.dmc.disaster.warnings.service.GaugeReading;
import lk.dmc.disaster.warnings.service.HazardDetailView;
import lk.dmc.disaster.warnings.service.HazardListEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class HazardMapperTest {

  private static final Instant NOW = Instant.parse("2026-10-06T10:00:00Z");
  private static final UUID TYPE = UUID.randomUUID();
  private static final UUID DISTRICT = UUID.randomUUID();

  @Mock private HazardTypeDirectory hazardTypes;

  private HazardMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new HazardMapper(hazardTypes);
    when(hazardTypes.codesById()).thenReturn(Map.of(TYPE, "FLOOD"));
  }

  private static Sensor sensor() {
    Sensor sensor = BeanUtils.instantiateClass(Sensor.class);
    ReflectionTestUtils.setField(sensor, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(sensor, "code", "KELANI-HANWELLA");
    ReflectionTestUtils.setField(sensor, "name", "Kelani at Hanwella");
    ReflectionTestUtils.setField(sensor, "alertLevel", new BigDecimal("1.20"));
    ReflectionTestUtils.setField(sensor, "majorFloodLevel", new BigDecimal("2.50"));
    ReflectionTestUtils.setField(sensor, "unit", "m");
    return sensor;
  }

  private static Hazard hazard() {
    return Hazard.manual(
        TYPE,
        3,
        new HazardArea(DISTRICT, null),
        "Kelani river is rising near Hanwella.",
        null,
        NOW);
  }

  @Test
  void toListItems_putsTheTypeCodeOnEveryItem() {
    List<HazardListItem> items =
        mapper.toListItems(
            List.of(
                new HazardListEntry(hazard(), 2, null), new HazardListEntry(hazard(), 0, null)));

    assertThat(items).extracting(HazardListItem::hazardTypeCode).containsExactly("FLOOD", "FLOOD");
    assertThat(items).extracting(HazardListItem::verifiedReportCount).containsExactly(2L, 0L);
  }

  @Test
  void toListItems_unknownTypeId_hasANullCode() {
    Hazard other =
        Hazard.manual(
            UUID.randomUUID(),
            2,
            new HazardArea(DISTRICT, null),
            "Landslide risk on the Kegalle road.",
            null,
            NOW);

    assertThat(
            mapper
                .toListItems(List.of(new HazardListEntry(other, 0, null)))
                .get(0)
                .hazardTypeCode())
        .isNull();
  }

  @Test
  void toListItems_latestReading_carriesUnitAndWhetherItIsAboveAlert() {
    Sensor sensor = sensor();
    GaugeReading above =
        new GaugeReading(sensor, SensorReading.record(sensor.getId(), new BigDecimal("1.74"), NOW));
    GaugeReading below =
        new GaugeReading(sensor, SensorReading.record(sensor.getId(), new BigDecimal("0.90"), NOW));

    List<HazardListItem> items =
        mapper.toListItems(
            List.of(
                new HazardListEntry(hazard(), 0, above), new HazardListEntry(hazard(), 0, below)));

    assertThat(items.get(0).latestReading().aboveAlert()).isTrue();
    assertThat(items.get(0).latestReading().unit()).isEqualTo("m");
    assertThat(items.get(0).latestReading().value()).isEqualByComparingTo("1.74");
    assertThat(items.get(1).latestReading().aboveAlert()).isFalse();
  }

  @Test
  void toDetail_withGaugeEvidenceAndWarnings_mapsEverySection() {
    Sensor sensor = sensor();
    SensorReading reading = SensorReading.record(sensor.getId(), new BigDecimal("1.74"), NOW);
    UUID report = UUID.randomUUID();
    Warning warning =
        Warning.publish(
            new WarningDraft(
                UUID.randomUUID(),
                null,
                WarningLevel.WATCH,
                new WarningTarget(TargetType.DISTRICT, Set.of(DISTRICT), Set.of()),
                new WarningContent(
                    "Kelani flood", "Water is rising fast.", "Kelani flood: move now.", "Leave."),
                Set.of()),
            UUID.randomUUID(),
            NOW);
    HazardDetailView view =
        new HazardDetailView(
            new HazardListEntry(hazard(), 1, new GaugeReading(sensor, reading)),
            List.of(
                new VerifiedReportSummary(
                    report, "RPT-2026-0001", "FLOOD", "Water over the road.", DISTRICT, NOW)),
            new GaugeHistory(sensor, List.of(reading)),
            List.of(warning));

    HazardDetail detail = mapper.toDetail(view);

    assertThat(detail.hazardTypeCode()).isEqualTo("FLOOD");
    assertThat(detail.evidence()).hasSize(1);
    assertThat(detail.evidence().get(0).referenceNo()).isEqualTo("RPT-2026-0001");
    assertThat(detail.sensor().code()).isEqualTo("KELANI-HANWELLA");
    assertThat(detail.sensor().readings()).hasSize(1);
    assertThat(detail.sensor().majorFloodLevel()).isEqualByComparingTo("2.50");
    assertThat(detail.warnings().get(0).id()).isEqualTo(warning.getId());
    assertThat(detail.warnings().get(0).level()).isEqualTo(WarningLevel.WATCH);
    assertThat(detail.latestReading().aboveAlert()).isTrue();
  }

  @Test
  void toDetail_withoutGauge_hasNullSensor() {
    HazardDetail detail =
        mapper.toDetail(
            new HazardDetailView(
                new HazardListEntry(hazard(), 0, null), List.of(), null, List.of()));

    assertThat(detail.sensor()).isNull();
    assertThat(detail.latestReading()).isNull();
    assertThat(detail.evidence()).isEmpty();
  }
}
