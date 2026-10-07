package lk.dmc.disaster.shared.storage;

import static org.assertj.core.api.Assertions.assertThat;

import lk.dmc.disaster.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * The whole application starts with the Supabase provider selected. Fake credentials are enough:
 * nothing is sent until a photo is stored.
 */
@SpringBootTest(
    properties = {
      "app.storage.provider=supabase",
      "app.storage.supabase.url=https://example.supabase.co/rest/v1/",
      "app.storage.supabase.service-key=sb_secret_fake"
    })
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class SupabaseWiringTest {

  @Autowired FileStorage fileStorage;

  @Test
  void theApplicationStartsAndUsesSupabaseStorage() {
    assertThat(fileStorage).isInstanceOf(SupabaseFileStorage.class);
  }
}
