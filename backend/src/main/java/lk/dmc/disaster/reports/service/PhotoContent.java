package lk.dmc.disaster.reports.service;

/** A stored photo ready to be sent back with its content type. */
public record PhotoContent(byte[] bytes, String contentType) {}
