package lk.dmc.disaster.shared.reference;

import static org.assertj.core.api.Assertions.assertThat;

import lk.dmc.disaster.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class ReferenceDataIntegrationTest {

    @Autowired private DistrictRepository districtRepository;
    @Autowired private RiverBasinRepository riverBasinRepository;
    @Autowired private HazardTypeRepository hazardTypeRepository;
    @Autowired private OrganisationRepository organisationRepository;
    @Autowired private ReliefItemRepository reliefItemRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private DisasterEventRepository disasterEventRepository;

    @Test
    void verifySeedDataCounts() {
        assertThat(districtRepository.count()).isEqualTo(5);
        assertThat(riverBasinRepository.count()).isEqualTo(2);
        assertThat(hazardTypeRepository.count()).isEqualTo(3);
        assertThat(organisationRepository.count()).isEqualTo(9);
        assertThat(reliefItemRepository.count()).isEqualTo(4);
        assertThat(appUserRepository.count()).isEqualTo(49);
        assertThat(disasterEventRepository.count()).isEqualTo(2);
    }
}
