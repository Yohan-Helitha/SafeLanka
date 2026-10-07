package lk.dmc.disaster.warnings.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Checks the SQL each JDBC adapter builds, how it binds its parameters and how it maps rows, with a
 * stand-in JdbcClient so no database is needed. The real queries run against PostgreSQL in {@link
 * JdbcDirectoriesTest}.
 */
class JdbcAdaptersTest {

  private static final UUID COLOMBO = UUID.randomUUID();
  private static final UUID GAMPAHA = UUID.randomUUID();
  private static final UUID KELANI = UUID.randomUUID();

  private JdbcClient jdbc;
  private JdbcClient.StatementSpec statement;

  @BeforeEach
  void setUp() {
    jdbc = mock(JdbcClient.class);
    statement = mock(JdbcClient.StatementSpec.class, RETURNS_SELF);
    when(jdbc.sql(anyString())).thenReturn(statement);
  }

  @SuppressWarnings("unchecked")
  private static <T> JdbcClient.MappedQuerySpec<T> resultOf(List<T> rows) {
    JdbcClient.MappedQuerySpec<T> result =
        mock(JdbcClient.MappedQuerySpec.class, CALLS_REAL_METHODS);
    doReturn(rows).when(result).list();
    return result;
  }

  private <T> void queryByClassReturns(Class<T> type, List<T> rows) {
    doReturn(resultOf(rows)).when(statement).query(type);
  }

  /**
   * Makes a row-mapper query return one row, built by the adapter's own mapper from the result set.
   */
  private void queryByMapperReturnsOneRowFrom(ResultSet resultSet) {
    doAnswer(
            call -> {
              RowMapper<?> mapper = call.getArgument(0);
              return resultOf(List.of(mapper.mapRow(resultSet, 0)));
            })
        .when(statement)
        .query(any(RowMapper.class));
  }

  private String sqlSent() {
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(jdbc).sql(sql.capture());
    return sql.getValue();
  }

  // ---- JdbcCitizenDirectory ----------------------------------------------------------------

  @Test
  void citizens_districtsOnly_filterOnDistrictAndBindOnlyThat() {
    UUID person = UUID.randomUUID();
    queryByClassReturns(UUID.class, List.of(person));

    List<UUID> ids =
        new JdbcCitizenDirectory(jdbc).findCitizenIdsInAreas(Set.of(COLOMBO), Set.of());

    assertThat(ids).containsExactly(person);
    assertThat(sqlSent())
        .contains("district_id in (:districtIds)")
        .doesNotContain("river_basin_id")
        .contains("status <> 'DISABLED'")
        .endsWith("order by id");
    verify(statement).param("districtIds", Set.of(COLOMBO));
  }

  @Test
  void citizens_basinsOnly_filterOnBasinAndBindOnlyThat() {
    queryByClassReturns(UUID.class, List.of());

    new JdbcCitizenDirectory(jdbc).findCitizenIdsInAreas(Set.of(), Set.of(KELANI));

    assertThat(sqlSent())
        .contains("river_basin_id in (:basinIds)")
        .doesNotContain("district_id in");
    verify(statement).param("basinIds", Set.of(KELANI));
  }

  @Test
  void citizens_districtsAndBasins_matchEitherOne() {
    queryByClassReturns(UUID.class, List.of());

    new JdbcCitizenDirectory(jdbc).findCitizenIdsInAreas(Set.of(COLOMBO), Set.of(KELANI));

    assertThat(sqlSent())
        .contains("district_id in (:districtIds) or river_basin_id in (:basinIds)");
  }

  @Test
  void citizens_countUsesTheSameFilterAsTheList() {
    JdbcClient.MappedQuerySpec<Long> count = resultOf(List.of(7L));
    doReturn(count).when(statement).query(Long.class);

    long total =
        new JdbcCitizenDirectory(jdbc).countCitizensInAreas(Set.of(COLOMBO), Set.of(KELANI));

    assertThat(total).isEqualTo(7);
    assertThat(sqlSent())
        .startsWith("select count(*) from users")
        .contains("district_id in (:districtIds) or river_basin_id in (:basinIds)");
  }

  @Test
  void citizens_noAreas_returnNothingWithoutAskingTheDatabase() {
    JdbcCitizenDirectory directory = new JdbcCitizenDirectory(jdbc);

    assertThat(directory.findCitizenIdsInAreas(Set.of(), Set.of())).isEmpty();
    assertThat(directory.countCitizensInAreas(Set.of(), Set.of())).isZero();
    assertThat(directory.districtsOf(Set.of())).isEmpty();
    verifyNoInteractions(jdbc);
  }

  @Test
  void citizens_districtsOf_mapsEachPersonToTheirDistrict() throws Exception {
    UUID person = UUID.randomUUID();
    ResultSet row = mock(ResultSet.class);
    when(row.getObject("id", UUID.class)).thenReturn(person);
    when(row.getObject("district_id", UUID.class)).thenReturn(COLOMBO);
    queryByMapperReturnsOneRowFrom(row);

    Map<UUID, UUID> districts = new JdbcCitizenDirectory(jdbc).districtsOf(Set.of(person));

    assertThat(districts).containsExactly(Map.entry(person, COLOMBO));
    verify(statement).param(eq("ids"), eq(Set.of(person)));
  }

  // ---- JdbcAreaReference -------------------------------------------------------------------

  @Test
  void areas_districtsInBasins_returnsTheDistinctDistricts() {
    queryByClassReturns(UUID.class, List.of(COLOMBO, GAMPAHA));

    Set<UUID> districts = new JdbcAreaReference(jdbc).districtsInBasins(Set.of(KELANI));

    assertThat(districts).containsExactlyInAnyOrder(COLOMBO, GAMPAHA);
    assertThat(sqlSent())
        .contains("district_river_basins")
        .contains("river_basin_id in (:basinIds)");
  }

  @Test
  void areas_districtsInBasins_noBasins_doesNotAskTheDatabase() {
    assertThat(new JdbcAreaReference(jdbc).districtsInBasins(Set.of())).isEmpty();

    verifyNoInteractions(jdbc);
  }

  @Test
  void areas_basinsOfDistrict_returnsEveryBasinOfTheDistrict() {
    queryByClassReturns(UUID.class, List.of(KELANI));

    assertThat(new JdbcAreaReference(jdbc).basinsOfDistrict(COLOMBO)).containsExactly(KELANI);
    verify(statement).param("id", COLOMBO);
  }

  @Test
  void areas_exists_isTrueOnlyWhenTheCountIsAboveZero() {
    doReturn(resultOf(List.of(1L))).when(statement).query(Long.class);
    assertThat(new JdbcAreaReference(jdbc).districtExists(COLOMBO)).isTrue();

    doReturn(resultOf(List.of(0L))).when(statement).query(Long.class);
    assertThat(new JdbcAreaReference(jdbc).riverBasinExists(KELANI)).isFalse();
  }

  // ---- JdbcHazardTypeDirectory -------------------------------------------------------------

  @Test
  void hazardTypes_codesById_mapsEachTypeToItsCode() throws Exception {
    UUID flood = UUID.randomUUID();
    ResultSet row = mock(ResultSet.class);
    when(row.getObject("id", UUID.class)).thenReturn(flood);
    when(row.getString("code")).thenReturn("FLOOD");
    queryByMapperReturnsOneRowFrom(row);

    assertThat(new JdbcHazardTypeDirectory(jdbc).codesById())
        .containsExactly(Map.entry(flood, "FLOOD"));
  }
}
