package lk.dmc.disaster.warnings.repository;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.TestcontainersConfiguration;
import lk.dmc.disaster.shared.domain.WarningLevel;
import lk.dmc.disaster.warnings.entity.Hazard;
import lk.dmc.disaster.warnings.entity.HazardArea;
import lk.dmc.disaster.warnings.entity.TargetType;
import lk.dmc.disaster.warnings.entity.Warning;
import lk.dmc.disaster.warnings.entity.WarningContent;
import lk.dmc.disaster.warnings.entity.WarningDraft;
import lk.dmc.disaster.warnings.entity.WarningTarget;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs against a real PostgreSQL (local, or Testcontainers when Docker is available) with the
 * Flyway schema and seed data, so CHECK and UNIQUE constraints are exercised for real. Each test
 * rolls back.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
abstract class RepositoryTestSupport {

  protected static final Instant NOW = Instant.parse("2026-10-06T10:00:00Z");

  @Autowired protected JdbcClient jdbc;
  @Autowired protected HazardRepository hazards;
  @Autowired protected WarningRepository warnings;

  protected UUID firstId(String table) {
    return ids(table, 1).get(0);
  }

  protected List<UUID> ids(String table, int count) {
    return jdbc.sql("select id from " + table + " order by id limit " + count)
        .query(UUID.class)
        .list();
  }

  protected UUID userWithRole(String role, int offset) {
    return jdbc.sql("select id from users where role = :role order by id limit 1 offset :offset")
        .param("role", role)
        .param("offset", offset)
        .query(UUID.class)
        .single();
  }

  /**
   * Inserts a minimal hazard report, the reports module's table, so evidence rows can point at it.
   */
  protected UUID insertReport() {
    UUID id = UUID.randomUUID();
    jdbc.sql(
            """
            insert into hazard_reports (id, reference_no, reporter_id, hazard_type_id, category,
              description, latitude, longitude, district_id, client_ref, captured_at, synced_at)
            values (:id, :ref, :reporter, :type, 'FLOOD', 'Water over the road near the bridge.',
              6.9, 79.9, :district, :clientRef, now(), now())
            """)
        .param("id", id)
        .param("ref", "T-" + id.toString().substring(0, 8))
        .param("reporter", userWithRole("CITIZEN", 0))
        .param("type", firstId("hazard_types"))
        .param("district", firstId("districts"))
        .param("clientRef", UUID.randomUUID())
        .update();
    return id;
  }

  protected Hazard savedHazard(UUID typeId, HazardArea area, Instant detectedAt) {
    return hazards.saveAndFlush(
        Hazard.manual(
            typeId, 3, area, "Water level rising near the river bank.", null, detectedAt));
  }

  protected Warning savedWarning(Hazard hazard, WarningTarget target) {
    WarningContent content =
        new WarningContent(
            "Kelani flood", "Water is rising fast.", "Kelani flood: move now.", "Leave homes.");
    WarningDraft draft =
        new WarningDraft(hazard.getId(), null, WarningLevel.WARNING, target, content, Set.of());
    return warnings.saveAndFlush(Warning.publish(draft, userWithRole("DMC_OFFICER", 0), NOW));
  }

  protected static WarningTarget districtTarget(UUID... districts) {
    return new WarningTarget(TargetType.DISTRICT, Set.of(districts), Set.of());
  }

  protected static WarningTarget basinTarget(UUID... basins) {
    return new WarningTarget(TargetType.RIVER_BASIN, Set.of(), Set.of(basins));
  }
}
