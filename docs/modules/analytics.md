# Analytics module (UC04) – disaster report generation

Owner: Savidu Herath · package `lk.dmc.disaster.analytics` · folders `controller / dto / entity / mapper / repository / service`, plus the module-specific `section / query / export`.

## What it does

A DMC officer picks a disaster event (and optionally districts and a time window) and generates a **disaster report** with four sections. The report is saved, can be listed and reopened, and exported as PDF or CSV. District officers can read and export reports; only DMC officers generate them.

| Section | Key (API) | What it shows | Source |
| --- | --- | --- | --- |
| Alert timeline | `alertTimeline` | warnings in time order, supersedes chain, minutes from first verified report to first warning | `warnings`, `warning_target_areas`, `hazard_reports` |
| Citizens reached | `citizensReached` | unique citizens targeted / reached, by district and by channel | `notification_deliveries` |
| Shelter occupancy | `shelterOccupancy` | occupancy series and peak per shelter (earliest reading wins a tie) | `occupancy_logs`, `shelters` |
| Resource distribution | `resourceDistribution` | allocated and distributed relief by district, organisation type and item | `resource_allocations`, `relief_distributions` |

A section with no data is **unavailable with a reason** (`unavailableSections`), never a block of zeros. A section that throws is logged and reported unavailable as well, so one failure never loses the whole report.

## Endpoints

| Method and path | Roles | Result |
| --- | --- | --- |
| `GET /api/analytics/events?status=` | DMC, District | events that can be reported on (`/api/analytics/disaster-events` is the same endpoint under a name ad blockers leave alone) |
| `POST /api/analytics/reports` | DMC | 201 generated report; 400 invalid body; 404 unknown event; 422 districts outside the event or a bad time window |
| `GET /api/analytics/reports` | DMC, District | paged summaries, newest first |
| `GET /api/analytics/reports/{id}` | DMC, District | one report; 404 if unknown |
| `GET /api/analytics/reports/{id}/export?format=PDF\|CSV` | DMC, District | file download; 400 for any other format |

The full contract is in [`docs/api/analytics.yaml`](../api/analytics.yaml).

### Rules applied when generating

- Districts default to those of the event; every chosen district must belong to the event.
- The window defaults to event start … event end (or now for an active event); `from` must not be after `to`, and both must lie inside the event, with one minute of tolerance.
- Saved sections are stored as JSONB keyed by the enum name; the API always returns camelCase keys in report order.

### Export

- File name: `disaster-report-<event-slug>-<yyyyMMdd>.<pdf|csv>`, the date being the generation date in UTC.
- CSV columns: `section,label,value,extra`. Unavailable sections appear as a row with value `Data unavailable` and the reason in `extra`.
- PDF: header with event name, generated-by name and time, filters, then one block per section.

## Layout

```
analytics/
  controller/  AnalyticsController
  dto/         GenerateReportRequest, DisasterReportResponse, ReportSummaryResponse
  entity/      DisasterReport (JPA), SectionKey, AnalyticsRules, ReportContext, section result records
  mapper/      AnalyticsMapper
  repository/  DisasterReportRepository
  service/     AnalyticsService(+Impl), ReportBuilder
  section/     one ReportSection per report section (Template/Strategy)
  query/       read-only JdbcClient queries; ContextFilters builds the shared window/district SQL
  export/      ExportService, ReportExporter (PDF, CSV), ExportFormat, ExportedFile
```

Design: `ReportBuilder` runs every `ReportSection` bean in key order (Template Method); exporters are Strategies selected by `ExportFormat`; other modules are reached only through `shared.reference` (`ReferenceData`, `UserDirectory`) and SQL reads, never their Java types.

## Frontend

`frontend/src/services/analytics/analytics.http.ts` maps the API (camelCase section keys, `generatedBy.fullName`) to the page model. Run with `VITE_USE_MOCKS=false`.

## How to verify

From `backend/` (point the tests at a database; the query tests expect the Kalu Flood seed):

```
./mvnw verify "-Dapp.test.use-testcontainers=false" "-Dtest=lk.dmc.disaster.analytics.**.*Test,lk.dmc.disaster.shared.reference.*Test" "-Dspotless.check.skip=true"
```

Coverage report: `backend/target/site/jacoco/index.html` (≥ 80% line and branch per analytics package and `shared.reference`). `ModularityTests` must stay green.

Format only this module: `./mvnw spotless:apply "-DspotlessFiles=.*lk/dmc/disaster/analytics/.*"`.
