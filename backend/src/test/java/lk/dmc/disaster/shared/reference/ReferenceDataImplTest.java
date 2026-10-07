package lk.dmc.disaster.shared.reference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lk.dmc.disaster.shared.error.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReferenceDataImplTest {

    @Mock private DistrictRepository districtRepository;
    @Mock private RiverBasinRepository riverBasinRepository;
    @Mock private HazardTypeRepository hazardTypeRepository;
    @Mock private OrganisationRepository organisationRepository;
    @Mock private ReliefItemRepository reliefItemRepository;
    @Mock private DisasterEventRepository disasterEventRepository;

    @InjectMocks private ReferenceDataImpl referenceData;

    @Test
    void testDistrictExists() {
        UUID id = UUID.randomUUID();
        when(districtRepository.existsById(id)).thenReturn(true);
        assertThat(referenceData.districtExists(id)).isTrue();
    }

    @Test
    void testHazardTypeNotFound() {
        UUID id = UUID.randomUUID();
        when(hazardTypeRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> referenceData.hazardType(id));
    }
}
