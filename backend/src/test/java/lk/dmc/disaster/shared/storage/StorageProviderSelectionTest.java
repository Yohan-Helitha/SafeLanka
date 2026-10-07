package lk.dmc.disaster.shared.storage;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** Which storage the application uses is decided by configuration alone. */
class StorageProviderSelectionTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withUserConfiguration(LocalFileStorage.class, SupabaseFileStorage.class)
          .withPropertyValues("app.storage.root=target/test-uploads");

  @Test
  void localDiskIsTheDefaultSoNoKeyIsNeeded() {
    runner.run(
        context -> {
          assertThat(context).hasSingleBean(FileStorage.class);
          assertThat(context.getBean(FileStorage.class)).isInstanceOf(LocalFileStorage.class);
        });
  }

  @Test
  void supabaseIsUsedWhenSelected() {
    runner
        .withPropertyValues(
            "app.storage.provider=supabase",
            "app.storage.supabase.url=https://abc.supabase.co/rest/v1/",
            "app.storage.supabase.service-key=sb_secret_test",
            "app.storage.supabase.bucket=evidence")
        .run(
            context -> {
              assertThat(context).hasSingleBean(FileStorage.class);
              assertThat(context.getBean(FileStorage.class))
                  .isInstanceOf(SupabaseFileStorage.class);
            });
  }

  @Test
  void selectingSupabaseWithoutACredentialStopsTheApplicationAtStartup() {
    runner
        .withPropertyValues("app.storage.provider=supabase")
        .run(
            context -> {
              assertThat(context).hasFailed();
              assertThat(context.getStartupFailure())
                  .hasRootCauseInstanceOf(IllegalStateException.class)
                  .hasRootCauseMessage(
                      "STORAGE_PROVIDER=supabase needs SUPABASE_URL and SUPABASE_SERVICE_KEY in backend/.env");
            });
  }

  @Test
  void anExplicitLocalSelectionUsesTheDisk() {
    runner
        .withPropertyValues("app.storage.provider=local")
        .run(context -> assertThat(context.getBean(FileStorage.class)).isInstanceOf(LocalFileStorage.class));
  }
}
