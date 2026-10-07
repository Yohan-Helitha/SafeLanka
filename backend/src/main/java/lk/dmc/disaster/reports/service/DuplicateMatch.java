package lk.dmc.disaster.reports.service;

import lk.dmc.disaster.reports.entity.HazardReport;

/** Another report that looks like the same event, and how far away it is. */
public record DuplicateMatch(HazardReport report, double distanceMetres) {}
