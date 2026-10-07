package lk.dmc.disaster.shared.storage;

/** Stores and serves uploaded images. Callers depend on this, not on where bytes are kept. */
public interface FileStorage {

  /**
   * Keeps a JPEG or PNG of at most 5 MB under {@code folder}; anything else is a 400.
   *
   * @return the stored file's relative path, content type and size
   */
  StoredFile store(String folder, String contentType, byte[] content);

  /** Bytes of a file returned by {@link #store}; a 404 when it is missing. */
  byte[] load(String path);

  /** Removes a stored file, for example after a failed save. Never throws: a missing file is fine. */
  void delete(String path);
}
