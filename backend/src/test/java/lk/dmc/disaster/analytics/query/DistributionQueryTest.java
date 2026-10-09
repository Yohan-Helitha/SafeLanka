package lk.dmc.disaster.analytics.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.TestcontainersConfiguration;
import lk.dmc.disaster.analytics.entity.ReportContext;
import lk.dmc.disaster.analytics.entity.ResourceDistribution.DistrictDistribution;
import lk.dmc.disaster.analytics.entity.ResourceDistribution.ItemDistribution;
import lk.dmc.disaster.analytics.entity.ResourceDistribution.OrganisationTypeDistribution;
import lk.dmc.disaster.support.TestIds;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/** Kalu seed: relief allocated at the Ratnapura shelter, from government and armed-forces stock. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class DistributionQueryTest {

  @Autowired DistributionQuery query;

  private static ReportContext kalu(Set<UUID> districts, Instant from, Instant to) {
    return new ReportContext(TestIds.event(2), districts, from, to, TestIds.user(1));
  }

  @Test
  void byDistrict_listsEachItemOfTheShelterDistrict() {
    var rows = query.getByDistrict(kalu(null, null, null));

    assertThat(rows).isNotEmpty();
    assertThat(rows).extracting(DistrictDistribution::districtName).containsOnly("Ratnapura");
    assertThat(rows).allSatisfy(r -> assertThat(r.allocated()).isPositive());
    assertThat(rows).allSatisfy(r -> assertThat(r.distributed()).isBetween(0L, r.allocated()));
  }

  @Test
  void byOrganisationType_splitsBetweenGovernmentAndArmedForces() {
    var rows = query.getByOrganisationType(kalu(null, null, null));

    assertThat(rows).extracting(OrganisationTypeDistribution::type).contains("GOVERNMENT");
    assertThat(rows).isSortedAccordingTo((a, b) -> a.type().compareTo(b.type()));
  }

  @Test
  void byItem_matchesTheDistrictTotalsOfDistributedStock() {
    var byItem = query.getByItem(kalu(null, null, null));
    var byDistrict = query.getByDistrict(kalu(null, null, null));

    long fromItems = byItem.stream().mapToLong(ItemDistribution::distributed).sum();
    long fromDistricts = byDistrict.stream().mapToLong(DistrictDistribution::distributed).sum();
    assertThat(fromItems).isEqualTo(fromDistricts);
  }

  @Test
  void ratnapuraNarrowing_keepsEverything_andKalutaraLeavesNothing() {
    var all = query.getByDistrict(kalu(null, null, null));

    assertThat(query.getByDistrict(kalu(Set.of(TestIds.district(4)), null, null))).hasSameSizeAs(all);
    var kalutara = kalu(Set.of(TestIds.district(3)), null, null);
    assertThat(query.getByDistrict(kalutara)).isEmpty();
    assertThat(query.getByOrganisationType(kalutara)).isEmpty();
    assertThat(query.getByItem(kalutara)).isEmpty();
  }

  @Test
  void aWindowAfterTheEvent_hasNothing() {
    var later = kalu(null, Instant.parse("2027-01-01T00:00:00Z"), null);

    assertThat(query.getByDistrict(later)).isEmpty();
    assertThat(query.getByOrganisationType(later)).isEmpty();
    assertThat(query.getByItem(later)).isEmpty();
  }

  @Test
  void aWindowEndingBeforeTheEvent_hasNothing() {
    assertThat(query.getByDistrict(kalu(null, null, Instant.parse("2026-01-01T00:00:00Z")))).isEmpty();
  }
}
