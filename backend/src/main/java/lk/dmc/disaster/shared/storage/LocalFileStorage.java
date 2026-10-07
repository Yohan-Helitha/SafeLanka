package lk.dmc.disaster.shared.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import lk.dmc.disaster.shared.error.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** {@link FileStorage} on the local disk under {@code app.storage.root}. */
@Component
class LocalFileStorage implements FileStorage {

  static final int MAX_BYTES = 5 * 1024 * 1024;

  private static final Pattern FOLDER = Pattern.compile("[a-z0-9-]{1,30}");
  private static final Map<String, String> EXTENSIONS =
      Map.of("image/jpeg", "jpg", "image/png", "png");
  private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
  private static final byte[] PNG_MAGIC = {(byte) 0x89, 'P', 'N', 'G'};

  private final Path root;

  LocalFileStorage(@Value("${app.storage.root}") Path root) {
    this.root = root.toAbsolutePath().normalize();
  }

  @Override
  public StoredFile store(String folder, String contentType, byte[] content) {
    validate(folder, contentType, content);
    String relative = folder + "/" + UUID.randomUUID() + "." + EXTENSIONS.get(contentType);
    try {
      Path target = root.resolve(relative);
      Files.createDirectories(target.getParent());
      Files.write(target, content);
    } catch (IOException e) {
      throw new UncheckedIOException("Could not store the file.", e);
    }
    return new StoredFile(relative, contentType, content.length);
  }

  @Override
  public byte[] load(String path) {
    Path file = root.resolve(path).normalize();
    if (!file.startsWith(root)) {
      throw new NotFoundException("File not found.");
    }
    try {
      return Files.readAllBytes(file);
    } catch (NoSuchFileException e) {
      throw new NotFoundException("File not found.");
    } catch (IOException e) {
      throw new UncheckedIOException("Could not read the file.", e);
    }
  }

  private static void validate(String folder, String contentType, byte[] content) {
    if (!FOLDER.matcher(folder).matches()) {
      throw new IllegalArgumentException("Invalid storage folder: " + folder);
    }
    if (contentType == null || !EXTENSIONS.containsKey(contentType)) {
      throw invalid("The photo must be a JPEG or PNG image.");
    }
    if (content.length == 0) {
      throw invalid("The photo is empty.");
    }
    if (content.length > MAX_BYTES) {
      throw invalid("The photo must be at most 5 MB.");
    }
    byte[] magic = "image/png".equals(contentType) ? PNG_MAGIC : JPEG_MAGIC;
    if (!startsWith(content, magic)) {
      throw invalid("The file content does not match its image type.");
    }
  }

  private static boolean startsWith(byte[] content, byte[] prefix) {
    if (content.length < prefix.length) {
      return false;
    }
    for (int i = 0; i < prefix.length; i++) {
      if (content[i] != prefix[i]) {
        return false;
      }
    }
    return true;
  }

  private static AppException invalid(String message) {
    return new AppException(ErrorCode.VALIDATION_ERROR, message);
  }
}
