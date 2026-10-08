package lk.dmc.disaster.shared.reference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.shared.domain.Role;
import lk.dmc.disaster.shared.error.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserDirectoryImplTest {

    @Mock private AppUserRepository appUserRepository;

    @InjectMocks private UserDirectoryImpl userDirectory;

    @Test
    void testRequireThrowsNotFound() {
        UUID id = UUID.randomUUID();
        when(appUserRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> userDirectory.require(id));
    }
}
