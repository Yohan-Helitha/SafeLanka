package lk.dmc.disaster.analytics.export;

/** A downloadable report: its name, content type and bytes. */
public record ExportedFile(String filename, String contentType, byte[] content) {}
