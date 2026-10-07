package lk.dmc.disaster.shared.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import lk.dmc.disaster.shared.error.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalFileStorageTest {

  private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2, 3};
  private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 1, 2};

  @TempDir Path root;

  private LocalFileStorage storage;

  @BeforeEach
  void setUp() {
    storage = new LocalFileStorage(root);
  }

  @Test
  void store_jpegIsKeptUnderTheFolderAndReportsItsSize() {
    StoredFile stored = storage.store("reports", "image/jpeg", JPEG);

    assertThat(stored.path()).startsWith("reports/").endsWith(".jpg");
    assertThat(stored.contentType()).isEqualTo("image/jpeg");
    assertThat(stored.sizeBytes()).isEqualTo(JPEG.length);
    assertThat(Files.exists(root.resolve(stored.path()))).isTrue();
  }

  @Test
  void store_pngGetsPngExtension() {
    assertThat(storage.store("reports", "image/png", PNG).path()).endsWith(".png");
  }

  @Test
  void store_sameContentTwiceGivesDifferentPaths() {
    assertThat(storage.store("reports", "image/jpeg", JPEG).path())
        .isNotEqualTo(storage.store("reports", "image/jpeg", JPEG).path());
  }

  @Test
  void load_returnsWhatWasStored() {
    StoredFile stored = storage.store("reports", "image/png", PNG);

    assertThat(storage.load(stored.path())).isEqualTo(PNG);
  }

  @Test
  void store_exactlyFiveMegabytesIsAccepted() {
    byte[] content = Arrays.copyOf(JPEG, ImageRules.MAX_BYTES);

    assertThat(storage.store("reports", "image/jpeg", content).sizeBytes())
        .isEqualTo(ImageRules.MAX_BYTES);
  }

  @Test
  void store_overFiveMegabytesIsRejected() {
    byte[] content = Arrays.copyOf(JPEG, ImageRules.MAX_BYTES + 1);

    assertThatThrownBy(() -> storage.store("reports", "image/jpeg", content))
        .isInstanceOfSatisfying(
            AppException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.VALIDATION_ERROR))
        .hasMessageContaining("5 MB");
  }

  @Test
  void store_otherContentTypeIsRejected() {
    assertThatThrownBy(() -> storage.store("reports", "image/gif", JPEG))
        .isInstanceOf(AppException.class)
        .hasMessageContaining("JPEG or PNG");
  }

  @Test
  void store_missingContentTypeIsRejected() {
    assertThatThrownBy(() -> storage.store("reports", null, JPEG))
        .isInstanceOf(AppException.class)
        .hasMessageContaining("JPEG or PNG");
  }

  @Test
  void store_emptyFileIsRejected() {
    assertThatThrownBy(() -> storage.store("reports", "image/jpeg", new byte[0]))
        .isInstanceOf(AppException.class)
        .hasMessageContaining("empty");
  }

  @Test
  void store_contentThatDoesNotMatchTheDeclaredTypeIsRejected() {
    assertThatThrownBy(() -> storage.store("reports", "image/png", JPEG))
        .isInstanceOf(AppException.class)
        .hasMessageContaining("does not match");
    assertThatThrownBy(() -> storage.store("reports", "image/jpeg", new byte[] {1}))
        .isInstanceOf(AppException.class);
  }

  @Test
  void store_unsafeFolderNameIsRejected() {
    assertThatThrownBy(() -> storage.store("../etc", "image/jpeg", JPEG))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void load_missingFileIsNotFound() {
    assertThatThrownBy(() -> storage.load("reports/none.jpg"))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void load_pathOutsideTheRootIsNotFound() {
    assertThatThrownBy(() -> storage.load("../outside.jpg")).isInstanceOf(NotFoundException.class);
  }

  @Test
  void delete_removesTheStoredFile() {
    StoredFile stored = storage.store("reports", "image/jpeg", JPEG);

    storage.delete(stored.path());

    assertThat(Files.exists(root.resolve(stored.path()))).isFalse();
    assertThatThrownBy(() -> storage.load(stored.path())).isInstanceOf(NotFoundException.class);
  }

  @Test
  void delete_missingFileIsIgnored() {
    storage.delete("reports/none.jpg");
  }

  @Test
  void delete_pathOutsideTheRootIsIgnoredAndTheFileSurvives() throws Exception {
    Path outside = Files.createTempFile("outside", ".jpg");

    storage.delete("../" + outside.getFileName());

    assertThat(Files.exists(outside)).isTrue();
    Files.delete(outside);
  }
}
