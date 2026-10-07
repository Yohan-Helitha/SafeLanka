package lk.dmc.disaster.shared.storage;

/** Where an uploaded file was kept: a path relative to the storage root. */
public record StoredFile(String path, String contentType, long sizeBytes) {}
