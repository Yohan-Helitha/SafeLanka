package lk.dmc.disaster.analytics.export;

import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;

/** The file formats a saved report can be downloaded in. */
public enum ExportFormat {
  PDF("application/pdf", "pdf"),
  CSV("text/csv", "csv");

  private final String contentType;
  private final String extension;

  ExportFormat(String contentType, String extension) {
    this.contentType = contentType;
    this.extension = extension;
  }

  /** The HTTP content type of files in this format. */
  public String contentType() {
    return contentType;
  }

  /** The file extension, without the dot. */
  public String extension() {
    return extension;
  }

  /**
   * Reads the {@code format} request parameter, ignoring case.
   *
   * @throws AppException (400) for anything but PDF or CSV
   */
  public static ExportFormat parse(String value) {
    for (ExportFormat format : values()) {
      if (format.name().equalsIgnoreCase(value)) {
        return format;
      }
    }
    throw new AppException(ErrorCode.VALIDATION_ERROR, "Unsupported export format. Use PDF or CSV.");
  }
}
