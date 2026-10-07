package lk.dmc.disaster.shared.storage;

import java.util.Map;
import java.util.regex.Pattern;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;

/** What an uploaded image must look like, whichever storage keeps it. */
final class ImageRules {

  static final int MAX_BYTES = 5 * 1024 * 1024;

  private static final Pattern FOLDER = Pattern.compile("[a-z0-9-]{1,30}");
  private static final Map<String, String> EXTENSIONS =
      Map.of("image/jpeg", "jpg", "image/png", "png");
  private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
  private static final byte[] PNG_MAGIC = {(byte) 0x89, 'P', 'N', 'G'};

  private ImageRules() {}

  /** Throws a 400 for anything but a non-empty JPEG or PNG of at most 5 MB. */
  static void validate(String folder, String contentType, byte[] content) {
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

  /** File extension for a content type that passed {@link #validate}. */
  static String extensionOf(String contentType) {
    return EXTENSIONS.get(contentType);
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
