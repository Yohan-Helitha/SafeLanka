package lk.dmc.disaster.shared.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.UUID;
import lk.dmc.disaster.shared.error.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * {@link FileStorage} on the local disk under {@code app.storage.root}. The default; files exist
 * only on the machine that stored them.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
class LocalFileStorage implements FileStorage {

  private final Path root;

  LocalFileStorage(@Value("${app.storage.root}") Path root) {
    this.root = root.toAbsolutePath().normalize();
  }

  @Override
  public StoredFile store(String folder, String contentType, byte[] content) {
    ImageRules.validate(folder, contentType, content);
    String relative = folder + "/" + UUID.randomUUID() + "." + ImageRules.extensionOf(contentType);
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
    Path file = inside(path);
    try {
      return Files.readAllBytes(file);
    } catch (NoSuchFileException e) {
      throw new NotFoundException("File not found.");
    } catch (IOException e) {
      throw new UncheckedIOException("Could not read the file.", e);
    }
  }

  @Override
  public void delete(String path) {
    try {
      Files.deleteIfExists(inside(path));
    } catch (IOException | NotFoundException e) {
      log.warn("Could not delete stored file {}", path);
    }
  }

  private Path inside(String path) {
    Path file = root.resolve(path).normalize();
    if (!file.startsWith(root)) {
      throw new NotFoundException("File not found.");
    }
    return file;
  }
}
