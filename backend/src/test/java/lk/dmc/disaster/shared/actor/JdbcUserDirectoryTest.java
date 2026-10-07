package lk.dmc.disaster.shared.actor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import lk.dmc.disaster.TestcontainersConfiguration;
import lk.dmc.disaster.shared.domain.Role;
import lk.dmc.disaster.shared.error.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/** Against the seeded users. */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class JdbcUserDirectoryTest {

  private static final UUID RUWAN = UUID.fromString("00000000-0000-0000-0006-000000000004");
  private static final UUID NIMAL = UUID.fromString("00000000-0000-0000-0006-000000000001");

  @Autowired UserDirectory users;

  @Test
  void find_returnsNameAndRole() {
    UserSummary ruwan = users.find(RUWAN).orElseThrow();

    assertThat(ruwan.fullName()).isEqualTo("Ruwan Fernando");
    assertThat(ruwan.role()).isEqualTo(Role.CITIZEN);
  }

  @Test
  void find_officerHasOfficerRole() {
    assertThat(users.require(NIMAL).role()).isEqualTo(Role.DMC_OFFICER);
  }

  @Test
  void find_unknownIdIsEmpty() {
    assertThat(users.find(UUID.randomUUID())).isEmpty();
  }

  @Test
  void require_unknownIdIsNotFound() {
    assertThatThrownBy(() -> users.require(UUID.randomUUID()))
        .isInstanceOf(NotFoundException.class);
  }
}
