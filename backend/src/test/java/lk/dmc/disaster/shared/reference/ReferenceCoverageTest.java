package lk.dmc.disaster.shared.reference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.TestcontainersConfiguration;
import lk.dmc.disaster.shared.error.NotFoundException;
import lk.dmc.disaster.support.TestIds;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** The reference endpoints and lookups against the seed, in one Spring context. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ReferenceCoverageTest {

  @Autowired MockMvc mvc;
  @Autowired ReferenceData reference;
  @Autowired UserDirectory users;

  @Test
  void endpoints_returnTheSeededLists() throws Exception {
    mvc.perform(get("/api/reference/districts"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").exists());
    mvc.perform(get("/api/reference/hazard-types"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").exists());
    mvc.perform(get("/api/reference/hazard-types").param("activeOnly", "true"))
        .andExpect(status().isOk());
    mvc.perform(get("/api/reference/organisations")).andExpect(status().isOk());
    mvc.perform(get("/api/reference/organisations").param("type", "GOVERNMENT"))
        .andExpect(status().isOk());
    mvc.perform(get("/api/reference/relief-items")).andExpect(status().isOk());
    mvc.perform(get("/api/reference/events")).andExpect(status().isOk());
    mvc.perform(get("/api/reference/events").param("status", "ACTIVE")).andExpect(status().isOk());
    mvc.perform(get("/api/reference/users").param("role", "DMC_OFFICER"))
        .andExpect(status().isOk());
  }

  @Test
  void lists_areNotEmpty_andFiltersNarrow() {
    assertThat(reference.districts()).isNotEmpty();
    assertThat(reference.riverBasins()).isNotEmpty();
    assertThat(reference.reliefItems()).isNotEmpty();
    assertThat(reference.organisations(Optional.empty())).isNotEmpty();
    var government = reference.organisations(Optional.of(OrganisationType.GOVERNMENT));
    assertThat(government).hasSizeLessThanOrEqualTo(reference.organisations(Optional.empty()).size());
    assertThat(reference.hazardTypes(true)).hasSizeLessThanOrEqualTo(reference.hazardTypes(false).size());
  }

  @Test
  void events_filterByStatus_andFindById() {
    var active = reference.events(Optional.of(EventStatus.ACTIVE));
    var closed = reference.events(Optional.of(EventStatus.CLOSED));

    assertThat(active).isNotEmpty();
    assertThat(closed).isNotEmpty();
    assertThat(reference.events(Optional.empty())).hasSize(active.size() + closed.size());
    assertThat(reference.event(TestIds.event(2)).id()).isEqualTo(TestIds.event(2));
    assertThatThrownBy(() -> reference.event(UUID.randomUUID())).isInstanceOf(NotFoundException.class);
  }

  @Test
  void hazardTypes_lookUpAndAcceptTheirCategories() {
    var type = reference.hazardTypes(true).get(0);

    assertThat(reference.hazardType(type.id()).id()).isEqualTo(type.id());
    assertThatThrownBy(() -> reference.hazardType(UUID.randomUUID()))
        .isInstanceOf(NotFoundException.class);
    assertThat(reference.hazardTypeAcceptsCategory(type.id(), "NO_SUCH_CATEGORY")).isFalse();
    assertThat(reference.hazardTypeAcceptsCategory(UUID.randomUUID(), "FLOOD")).isFalse();
  }

  @Test
  void districtsAndBasins_areLinkedBothWays() {
    assertThat(reference.districtExists(TestIds.district(4))).isTrue();
    assertThat(reference.districtExists(UUID.randomUUID())).isFalse();

    var basinsOfRatnapura = reference.basinsOfDistrict(TestIds.district(4));
    assertThat(basinsOfRatnapura).isNotEmpty();
    Set<UUID> ids = Set.of(basinsOfRatnapura.get(0).id());
    assertThat(reference.districtsInBasins(ids)).extracting(DistrictView::id).contains(TestIds.district(4));
    assertThat(reference.districtsInBasins(Set.of())).isEmpty();
    assertThat(reference.basinsOfDistrict(UUID.randomUUID())).isEmpty();
  }

  @Test
  void userDirectory_findsRequiresAndCountsCitizens() {
    UUID author = TestIds.user(1);

    assertThat(users.findById(author)).isPresent();
    assertThat(users.require(author).id()).isEqualTo(author);
    assertThat(users.findById(UUID.randomUUID())).isEmpty();
    assertThatThrownBy(() -> users.require(UUID.randomUUID())).isInstanceOf(NotFoundException.class);

    Set<UUID> districts = Set.of(TestIds.district(4), TestIds.district(3));
    long counted = users.countCitizensInAreas(districts, Set.of());
    assertThat(users.findCitizensInAreas(districts, Set.of())).hasSize((int) counted);
    assertThat(users.countCitizensInAreas(Set.of(), Set.of())).isZero();
  }
}
