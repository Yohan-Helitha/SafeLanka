package lk.dmc.disaster.reports.service;

/** A photo as received from the client, before it is stored. */
public record PhotoUpload(String contentType, byte[] content) {}
