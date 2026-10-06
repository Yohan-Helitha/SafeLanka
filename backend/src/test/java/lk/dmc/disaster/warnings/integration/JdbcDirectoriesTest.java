package lk.dmc.disaster.warnings.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Runs the two JDBC adapters against the real schema and seed data (Testcontainers). */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class JdbcDirectoriesTest {

  private static final UUID COLOMBO = UUID.fromString("00000000-0000-0000-0001-000000000001");
  private static final UUID GAMPAHA = UUID.fromString("00000000-0000-0000-0001-000000000002");
  private static final UUID KEGALLE = UUID.fromString("00000000-0000-0000-0001-000000000005");
  private static final UUID RATNAPURA = UUID.fromString("00000000-0000-0000-0001-000000000004");
  private static final UUID KELANI = UUID.fromString("00000000-0000-0000-0002-000000000001");
  private static final UUID KALU = UUID.fromString("00000000-0000-0000-0002-000000000002");

  @Autowired private CitizenDirectory citizens;
  @Autowired private AreaReference areas;
  @Autowired private JdbcClient jdbc;

  private UUID inKelaniBasinAndColombo;

  @BeforeEach
  void setUp() {
    inKelaniBasinAndColombo = insertUser("CITIZEN", "ACTIVE", COLOMBO, KELANI);
  }

  private UUID insertUser(String role, String status, UUID district, UUID basin) {
    UUID id = UUID.randomUUID();
    jdbc.sql(
            """
            insert into users (id, role, full_name, district_id, river_basin_id, status)
            values (:id, :role, 'Test Person', :district, :basin, :status)
            """)
        .param("id", id)
        .param("role", role)
        .param("district", district)
        .param("basin", basin)
        .param("status", status)
        .update();
    return id;
  }

  @Test
  void districtsInBasins_kelani_returnsItsThreeDistricts() {
    assertThat(areas.districtsInBasins(Set.of(KELANI)))
        .containsExactlyInAnyOrder(COLOMBO, GAMPAHA, KEGALLE);
  }

  @Test
  void districtsInBasins_emptyInput_returnsEmpty() {
    assertThat(areas.districtsInBasins(List.of())).isEmpty();
  }

  @Test
  void findCitizenIds_personMatchingDistrictAndBasin_isListedOnce() {
    List<UUID> ids = citizens.findCitizenIdsInAreas(Set.of(COLOMBO), Set.of(KELANI));

    assertThat(ids).containsOnlyOnce(inKelaniBasinAndColombo);
  }

  @Test
  void findCitizenIds_basinOnly_findsPeopleRegisteredToTheBasin() {
    assertThat(citizens.findCitizenIdsInAreas(Set.of(), Set.of(KELANI)))
        .contains(inKelaniBasinAndColombo);
  }

  @Test
  void findCitizenIds_otherAreas_doesNotIncludeThem() {
    assertThat(citizens.findCitizenIdsInAreas(Set.of(RATNAPURA), Set.of(KALU)))
        .doesNotContain(inKelaniBasinAndColombo);
  }

  @Test
  void findCitizenIds_disabledAccountsAndOfficers_areExcluded() {
    UUID disabled = insertUser("CITIZEN", "DISABLED", COLOMBO, KELANI);
    UUID officer = insertUser("DMC_OFFICER", "ACTIVE", COLOMBO, KELANI);
    UUID volunteer = insertUser("VOLUNTEER", "ACTIVE", COLOMBO, KELANI);

    List<UUID> ids = citizens.findCitizenIdsInAreas(Set.of(COLOMBO), Set.of());

    assertThat(ids).contains(volunteer).doesNotContain(disabled, officer);
  }

  @Test
  void countAndFind_alwaysAgree() {
    List<UUID> ids = citizens.findCitizenIdsInAreas(Set.of(COLOMBO, GAMPAHA), Set.of(KELANI));

    assertThat(citizens.countCitizensInAreas(Set.of(COLOMBO, GAMPAHA), Set.of(KELANI)))
        .isEqualTo(ids.size());
  }

  @Test
  void noAreas_returnsNobody() {
    assertThat(citizens.findCitizenIdsInAreas(Set.of(), Set.of())).isEmpty();
    assertThat(citizens.countCitizensInAreas(Set.of(), Set.of())).isZero();
  }
}
