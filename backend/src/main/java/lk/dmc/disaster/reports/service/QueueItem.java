package lk.dmc.disaster.reports.service;

import lk.dmc.disaster.reports.entity.HazardReport;

/** One row of the officer queue. */
public record QueueItem(HazardReport report, boolean hasPhoto, boolean possibleDuplicate) {}
