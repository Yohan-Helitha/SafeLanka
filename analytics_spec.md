# Savidu Herath – Analytics Module (UC04) Backend Guide

SE3070 A02 · LankaGuard · Oct 4, 2026

Savidu owns the analytics module: the DMC officer generates a post-event report for a disaster event, optionally filtered by districts and a time window, with four sections (alert timeline, citizens reached, shelter occupancy over time, resource distribution by district and owner). A section without data says so with a reason instead of showing zeros. Reports are saved and exported as PDF or CSV. Savidu also builds the shared reference data and the seed data every module's demo depends on.

## 1. Overview

| Item | Detail |
| --- | --- |
| Use case | UC04 Generate Disaster Response Analysis Report (Group 12 design, revised) |
| Package | `lk.dmc.disaster.analytics` (plus `section`, `query`, `export` subpackages) |
| Migration files | `V5_0_1__analytics.sql`; group duty: `V1_0_2__seed_reference.sql`, `V6_0_3__seed_event_history.sql` |
| Frontend screens that call it | Analysis (DMC and district), Analysis report with PDF and CSV export |
| Critique points fixed | C-24 filters and export added, C-27 report saved to its own entity (not the Warning lifeline) and "citizens reached" computed from real data, C-31 analytics screens added; uses C-09 deliveries and C-13 occupancy logs |
| Provides to others | Nothing; analytics only reads |
| Uses from others | Read-only SQL on Yohan's, Arani's and Lakni's tables (no imports of their Java classes); shared `ReferenceData`, `UserDirectory`, `ActingUser`, `Clock` |

**Why you can start on day 1:** your module depends on data, not on other members' code. Your history seed gives the closed "Kalu Flood May 2026" event data for every section, so you can build and test against it before anyone else finishes.

**Deliverables for the marks (50 individual)**

- Working endpoints in Section 4 and every flow in Section 2, matching the revised UC04 scenario and sequence diagram.
- Clean layered code with Template Method / Builder for sections and Strategy for exporters, SOLID throughout (20 marks).
- Unit tests with at least 80% line and branch coverage on `lk.dmc.disaster.analytics` (20 marks).
- Screenshots of the full Kalu report, a report with an unavailable section, an opened PDF, and the coverage page.

**Shared-kernel duty (group work, do first):** `shared.reference` (entities, repositories, `ReferenceData`, `UserDirectory`, `ReferenceController`), the reference seed `V1_0_2`, the test helper `TestIds` and `docs/SEED.md`. Everyone's `ActingUserFilter` and validation depend on `UserDirectory` and `ReferenceData`, so merge on Sunday 04 October. On Wednesday you also write `V6_0_3__seed_event_history.sql` (Section 5).

**Backup duty:** if Lakni falls behind on Tuesday evening, you take the shelter occupancy endpoints (`GET /api/shelters`, suggestions, `PATCH .../occupancy`) inside her response module, following her guide.

**Day plan**

| Day | Work |
| --- | --- |
| Sun 04 Oct | Shared reference module, V1\_0\_2 seed, TestIds, SEED.md with tests; merge to `main` |
| Mon 05 Oct | Domain, `SectionResult`, read-only queries with Testcontainers tests (against your history seed in the test) |
| Tue 06 Oct | Four sections, `ReportBuilder`, `AnalyticsService` |
| Wed 07 Oct | Exporters, controller, `V6_0_3` history seed; end-to-end run with the team |
| Thu 08 Oct | Coverage above 80%, screenshots, your report sections |
| Fri 09 Oct | Buffer; final tag and submission by 11:59 PM |

## 2. Features and flows

**Features**

- List disaster events with status, dates, districts, warning count and linked report count.
- Generate a report for an event, optionally narrowed to some of its districts and a time window inside the event.
- Four sections, each computed independently; a missing dataset makes that section "unavailable" with a reason, and the others still generate.
- Save every generated report with its filters, author and time; list saved reports; open one.
- Export a saved report as PDF (OpenPDF) or CSV (Apache Commons CSV).
- DISTRICT\_OFFICER can read and export saved reports; only DMC\_OFFICER can generate.

**Main flow (revised UC04)**

1. DMC officer opens Analysis; system lists events.
2. Officer chooses an event and optionally narrows districts and the time window.
3. Officer generates; system checks the event and filters.
4. System builds the alert timeline from the event's warnings and the first verified report linked to its hazards.
5. System computes citizens targeted and reached from notification deliveries, by channel and district.
6. System builds occupancy series and peaks from occupancy logs.
7. System totals allocated and distributed relief by district, item and owner organisation type.
8. System saves the report and returns it; officer reviews it on screen.
9. Officer exports PDF or CSV.

**Alternative flows**

- **A1 Missing dataset:** a section with no rows is listed in `unavailableSections` with a reason (for example "No notification deliveries were recorded for this event.") and is `null` in `sections`; the others are still generated.
- **A2 Filters:** districts and the time window narrow every section.
- **A3 Saved report:** officers reopen and re-export earlier reports without regenerating.

**Exception flows**

- **E1 Unknown event:** 404 and nothing saved.
- **E2 Bad filters:** start after end, window outside the event, or a district not in the event → 422.
- **E3 Unknown export format:** 400.
- **E4 Wrong role:** a district officer trying to generate → 403.

## 3. Components

Every class lives under `lk.dmc.disaster.analytics`. The module exports nothing; `ModularityTests` must show it depends only on `shared`.

| Package | Class | Responsibility |
| --- | --- | --- |
| domain | `DisasterReport` | JPA entity; `filters`, `sections`, `unavailableSections` stored as JSONB (`@JdbcTypeCode(SqlTypes.JSON)` on records or `JsonNode`); static `generated(...)` |
| domain | `ReportContext` | Record: eventId, districtIds, from, to |
| domain | `SectionKey` | Enum ALERT\_TIMELINE, CITIZENS\_REACHED, SHELTER\_OCCUPANCY, RESOURCE\_DISTRIBUTION (report order) |
| domain | `AnalyticsRules` | Delivery rate 2 decimals, amber peak ratio 0.90, export file name pattern |
| section | `ReportSection` | Interface: `SectionKey key()`, `SectionResult build(ReportContext ctx)` |
| section | `SectionResult` | Record (key, available, unavailableReason, data) with `available(...)` and `unavailable(...)` |
| section | `AlertTimelineSection`, `CitizensReachedSection`, `ShelterOccupancySection`, `ResourceDistributionSection` | One class per section; each returns data or a reason |
| section | `AlertTimeline`, `CitizensReached`, `ShelterOccupancy`, `ResourceDistribution` | Section data records (match the frontend types) |
| query | `WarningTimelineQuery` | Warnings of the event in the window covering the districts (direct or via basin), escalation chain |
| query | `ReportTimingQuery` | Earliest `reviewed_at` of VERIFIED reports linked via `hazard_evidence` to the event's hazards |
| query | `DeliveryStatsQuery` | Unique targeted and reached citizens; counts by channel and by district |
| query | `OccupancyQuery` | Occupancy points per shelter in the districts and window |
| query | `DistributionQuery` | Allocated and distributed by district + item, by organisation type, by item |
| query | `EventSummaryQuery` | Warning and linked report counts per event |
| export | `ReportExporter` | Interface: `ExportFormat format()`, `ExportedFile export(DisasterReportView)` |
| export | `PdfReportExporter`, `CsvReportExporter` | OpenPDF document; one CSV with a `section` column |
| export | `ExportService`, `ExportedFile`, `ExportFormat` | Picks the exporter by format |
| application | `ReportBuilder` | Runs every `ReportSection` in `SectionKey` order; collects data and unavailable reasons |
| application | `AnalyticsService` | List events, generate (validate event and filters, build, save), list saved, get |
| application | `AnalyticsMapper` | Entity → `DisasterReportResponse` |
| persistence | `DisasterReportRepository` | Saved reports by event, newest first |
| web | `AnalyticsController` | Endpoints in Section 4; export returns a file with Content-Disposition |

All `query` classes use `JdbcClient` with `@Transactional(readOnly = true)` and return records. They never import another module's entities or repositories.

**Patterns and principles to name in the report**

| Pattern / principle | Where |
| --- | --- |
| Template Method / Builder | `ReportBuilder` assembles a report from `ReportSection` parts in a fixed order |
| Strategy | `ReportExporter` with PDF and CSV implementations, chosen by `ExportService` |
| Open / Closed | A new section or export format is one new class, injected as `List<…>` |
| Query object | One read-only query class per dataset |
| Single Responsibility | Sections compute, queries read, exporters format, the service orchestrates |
| Dependency Inversion | `AnalyticsService` depends on `List<ReportSection>`, `ReferenceData`, `UserDirectory` and `Clock` |
| Modular boundaries | No dependency on other modules' Java types; proven by `ModularityTests` |

## 4. API, data sources and sections

Shared envelope and error format; `X-Acting-User` on every request.

| Method and path | Roles | Request | Success | Errors |
| --- | --- | --- | --- | --- |
| `GET /api/analytics/events?status` | DMC\_OFFICER, DISTRICT\_OFFICER | – | 200 `[{ id, name, hazardTypeId, status, startedAt, endedAt, districtIds, warningCount, reportCount }]` | – |
| `POST /api/analytics/reports` | DMC\_OFFICER | `{ eventId, districtIds?, from?, to? }` | 201 `DisasterReportResponse` | 400, 403, 404 (nothing saved), 422 |
| `GET /api/analytics/reports?eventId&page&size` | DMC\_OFFICER, DISTRICT\_OFFICER | – | 200 page of `{ id, eventId, eventName, generatedAt, generatedByName, unavailableCount }` | – |
| `GET /api/analytics/reports/{id}` | DMC\_OFFICER, DISTRICT\_OFFICER | – | 200 `DisasterReportResponse` | 404 |
| \`GET /api/analytics/reports/{id}/export?format=PDF | CSV\` | DMC\_OFFICER, DISTRICT\_OFFICER | – | 200 file (`application/pdf` or `text/csv`), `Content-Disposition: attachment; filename="disaster-report-kalu-flood-may-2026-20261004.pdf"` |

```json
// DisasterReportResponse
{ "id", "eventId", "eventName": "Kalu Flood May 2026",
  "filters": { "districtIds": ["…RAT", "…KAL"], "from": null, "to": null },
  "generatedAt", "generatedBy": { "id", "fullName": "Nimal Perera" },
  "sections": {
    "alertTimeline": { "entries": [{ "warningId", "level": "WATCH", "status": "ESCALATED", "issuedAt",
                       "supersedesId": null, "resolvedDistrictIds": [] }],
                       "firstVerifiedReportAt", "firstWarningAt", "reportToWarningMinutes": 85 },
    "citizensReached": { "uniqueCitizensTargeted": 17, "uniqueCitizensReached": 17, "deliveryRate": 1.0,
                         "byChannel": [{ "channel": "SMS", "delivered": 47, "failed": 4 }],
                         "byDistrict": [{ "districtId", "districtName": "Ratnapura", "targeted": 8, "reached": 8 }] },
    "shelterOccupancy": { "series": [{ "shelterId", "shelterName", "districtId", "capacity": 300,
                            "points": [{ "recordedAt", "occupancy": 160 }] }],
                          "peaks": [{ "shelterId", "peakOccupancy": 290, "capacity": 300, "peakRatio": 0.97, "peakAt" }] },
    "resourceDistribution": { "byDistrict": [{ "districtId", "districtName", "itemCode": "DRY_RATION", "unit": "packs",
                                "allocated": 250, "distributed": 250 }],
                              "byOrganisationType": [{ "type": "GOVERNMENT", "distributed": 250 }],
                              "byItem": [{ "itemCode", "unit", "distributed" }] } },
  "unavailableSections": [{ "key": "SHELTER_OCCUPANCY", "reason": "No shelter occupancy was logged for this event." }] }
```

**Tables you read (read-only `JdbcClient` SQL)**

| Owner | Tables |
| --- | --- |
| Group | `disaster_events`, `event_districts`, `districts`, `river_basins`, `district_river_basins`, `relief_items`, `organisations` |
| Yohan (reports) | `hazard_reports` (status, reviewed\_at) |
| Arani (warnings) | `hazards`, `hazard_evidence`, `warnings`, `warning_target_areas`, `notification_deliveries` |
| Lakni (response) | `shelters`, `occupancy_logs`, `resource_allocations`, `relief_distributions`, `relief_stocks` |

**Section definitions** (for the event, its selected districts and the window)

| Section | Computed as | Unavailable when |
| --- | --- | --- |
| Alert timeline | Warnings with `event_id` = event, `issued_at` in window, whose target districts (direct, or via basin) intersect the selected districts, ordered by `issued_at`. `firstVerifiedReportAt` = earliest `reviewed_at` of VERIFIED reports linked through `hazard_evidence` to hazards of the event. `reportToWarningMinutes` = first warning − first verified report, null if either is missing | no warnings |
| Citizens reached | From `notification_deliveries` of those warnings where the delivery's citizen district is selected: targeted = distinct citizens; reached = distinct citizens with at least one DELIVERED; rate = reached / targeted (2 dp); counts by channel; targeted and reached by district | no deliveries |
| Shelter occupancy | `occupancy_logs` with `event_id` = event, shelter in selected districts, in window: series per shelter; peak = highest occupancy (earliest on ties), ratio to capacity | no logs |
| Resource distribution | `resource_allocations` of the event with `allocated_at` in window and shelter in selected districts; allocated sum and distributed sum (from `relief_distributions`) by district + item; distributed by organisation type of the stock owner; distributed by item | no allocations |

**Contracts you consume**

| Type | Methods used | Why |
| --- | --- | --- |
| `ReferenceData` | `event(id)`, `districts()` | Validate the event and filters; district names |
| `UserDirectory` | `require(id)` | `generatedBy` name |
| `ActingUser`, `Clock` | – | Author and `generatedAt` |

## 5. Data, seed duties and rules

You write only `disaster_reports`. Everything else is read-only for you, except the seed files below, which are group data you author.

**`V5_0_1__analytics.sql`**

```sql
-- Owner: analytics module (UC04) – Savidu Herath. Reads every other table read-only; writes only this one.
CREATE TABLE disaster_reports (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id              UUID        NOT NULL REFERENCES disaster_events (id),
    filters               JSONB       NOT NULL DEFAULT '{}'::jsonb,   -- {districtIds, from, to}
    sections              JSONB       NOT NULL,                        -- the four section payloads
    unavailable_sections  JSONB       NOT NULL DEFAULT '[]'::jsonb,   -- [{key, reason}]
    generated_by          UUID        NOT NULL REFERENCES users (id),
    generated_at          TIMESTAMPTZ NOT NULL,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_disaster_reports_event ON disaster_reports (event_id, generated_at DESC);
```

**Seed duty 1 – `V1_0_2__seed_reference.sql` (Sunday):** copy it exactly from Section 8 of the Project Initialisation Guide: 5 districts, 2 river basins, 3 hazard types (Drought inactive), 9 organisations, 4 relief items, 9 named users and 40 demo residents, the two events. Record every UUID in `docs/SEED.md` and as constants in `src/test/java/lk/dmc/disaster/support/TestIds.java` (for example `TestIds.DISTRICT_CMB`, `TestIds.USER_DMC_OFFICER`, `TestIds.EVENT_KALU_MAY`).

**Seed duty 2 – `V6_0_3__seed_event_history.sql` (Wednesday):** the event data the demo and your sections need. It runs after Lakni's `V6_0_1` and Yohan's `V6_0_2`, so it can reference their rows (agree the verified report UUIDs with Yohan).

| Event | Rows to insert |
| --- | --- |
| Kalu Flood May 2026 (CLOSED, Ratnapura + Kalutara) | Hazard: FLOOD, Kalu basin, source SENSOR (Kalu at Ratnapura), RESOLVED, detected 2026-05-14 00:10 UTC; `hazard_evidence` linking Yohan's verified Ratnapura report (reviewed 00:05 UTC); readings for the Ratnapura gauge rising above 7.5 m; warnings WATCH (01:30 UTC, ESCALATED) → WARNING (14 May 20:30, ESCALATED, supersedes) → EVACUATE (15 May 14:30, EXPIRED, supersedes), each targeting the Kalu basin; `notification_deliveries` for every citizen and volunteer in Ratnapura and Kalutara for each warning on PUSH and SMS, plus AUDIBLE for WARNING and EVACUATE, with about 1 in 12 SMS FAILED ("Simulated gateway timeout"); `occupancy_logs` for Ratnapura Sivali Central College every 12 hours from 14 May 22:00 UTC: 20, 85, 160, 240, 285, 290, 260, 180, 110, 60, 20, 0; two allocations to that shelter (250 dry ration packs from DMC Ratnapura stock, 180 bottles of water from Air Force stock), both DISTRIBUTED with matching distribution rows (one marked recorded offline); two COMPLETED rescue assignments with status logs |
| Kelani Flood October 2026 (ACTIVE, Colombo + Gampaha) | Hazards: Kelani flood (SENSOR, Nagalagam Street, WARNED, severity 3), Gampaha flood (MANUAL, MONITORING), Kegalle landslide (REPORT, UNDER\_ASSESSMENT) with `hazard_evidence` to Yohan's verified reports; one ACTIVE WARNING for the Kelani basin issued about 20 hours before the demo, with deliveries; Navy team DISPATCHED with a PENDING\_ACK assignment to Wellampitiya (12 people, to Kolonnawa Maha Vidyalaya); one UNASSIGNED assignment at Sedawatte; one COMPLETED Army assignment; allocation of 200 bottles of water to Wellampitiya Community Hall with 120 distributed (reduce Army stock to 600); allocation of 150 dry ration packs to Kolonnawa; earlier occupancy points dated before the opening logs already in V6\_0\_1 (don't repeat those); recent `activity_logs` rows for Colombo and Gampaha |

Use relative times for the active event (for example `now() - interval '20 hours'`) so the demo always looks current. Keep the numbers identical to the frontend mock data so screenshots match either way.

**Business rules (`AnalyticsRules`)**

| Rule | Value |
| --- | --- |
| Generate | DMC\_OFFICER only; event must exist; districts must belong to the event (default: all); window defaults to the event's start and end (or now) and must lie inside it; `from` ≤ `to` |
| Sections | built in `SectionKey` order; one failing or empty section never stops the others; never fill missing data with zeros |
| Delivery rate | reached / targeted, rounded to 2 decimals |
| Peaks | highest occupancy per shelter; earliest time on ties; ratio to capacity, 2 decimals |
| Export | PDF and CSV only; file name `disaster-report-<event-slug>-<yyyyMMdd>.<ext>`; CSV columns `section,label,value,extra` |
| Persistence | every generated report is saved with filters, sections, unavailable sections, author and time |

## 6. Testing and definition of done

Target: at least 80% line and branch coverage on every package under `lk.dmc.disaster.analytics`. Run `./mvnw verify -Dtest='lk.dmc.disaster.analytics.**.*Test'`. Query tests run against Testcontainers PostgreSQL with all Flyway migrations, so your seed is also tested.

| Test class | Positive | Negative | Edge | Error |
| --- | --- | --- | --- | --- |
| `AlertTimelineSectionTest` (mocked queries) | entries in order with escalation links; 85 min report-to-warning | no warnings → unavailable with reason | no verified report → minutes null | – |
| `CitizensReachedSectionTest` | targeted, reached, by channel, by district | no deliveries → unavailable | rate rounding (2 of 3 = 0.67); citizen with only FAILED counted targeted not reached | – |
| `ShelterOccupancySectionTest` | series and peaks | no logs → unavailable | tie on peak picks earliest; ratio at 0.90 | – |
| `ResourceDistributionSectionTest` | totals by district + item, by owner type, by item | no allocations → unavailable | allocation with no distributions counts 0 distributed | – |
| `ReportBuilderTest` | runs sections in key order | – | one unavailable section doesn't stop others | a section throwing is reported, not swallowed silently |
| `AnalyticsServiceTest` | saves report with author and Clock time; default districts and window | district officer generating (403); district not in event (422); from > to (422); window outside event (422) | narrowed filters passed to context | unknown event → 404 and `save` never called |
| `ExportServiceTest` | picks exporter by format | unknown format (400) | – | unknown report (404) |
| `PdfReportExporterTest` | bytes start with `%PDF` and contain the event name | – | unavailable sections printed as "Data unavailable" | – |
| `CsvReportExporterTest` | header plus expected rows | – | values with commas quoted | – |
| Query tests (`@JdbcTest` / `@DataJpaTest` + Testcontainers) | each query against the Kalu seed returns the expected counts (3 warnings, 8 + 9 targeted, peak 290, 250 + 180 distributed) | other event's rows excluded | window boundaries inclusive | – |
| `AnalyticsControllerTest` (`@WebMvcTest`) | 201, 200, export headers and content type | 400, 403 | paging | 404 / 422 envelopes |
| `ReferenceDataImplTest`, `UserDirectoryImplTest` (shared duty) | lookups, basins to districts, citizens in areas (distinct) | unknown ids | inactive hazard type rejects categories | `require` throws 404 |

**Definition of done**

- [ ] Shared reference module, V1\_0\_2 seed, TestIds and SEED.md merged on Sunday; everyone's filter and validation work against them
- [ ] All 5 endpoints work with the frontend (`VITE_USE_MOCKS=false`): event list, generate, saved list, report view, PDF and CSV downloads open
- [ ] Kalu report shows every section with the expected numbers; a narrow window on the Kelani event shows an unavailable section with its reason
- [ ] V6\_0\_3 history seed makes every other member's screens look like the frontend mock data
- [ ] No imports of other modules' classes (ModularityTests green)
- [ ] Coverage at least 80% line and branch on every analytics package (and on `shared.reference`)
- [ ] Javadoc on public classes and methods in `application`, `section`, `export`
- [ ] `docs/api/analytics.yaml` matches Swagger UI
- [ ] Screenshots and coverage page saved; revised UC04 scenario and sequence diagram match the code (DisasterReport lifeline, not Warning)
- [ ] Every AI prompt you used copied into the report appendix

## 7. Prompts

Use these in an AI coding agent at the repository root, after Yohan has pushed the initialised repository. Send Prompt 0 once, Prompt 1 once, then the step prompts one at a time. Review every diff and be ready to explain it. Copy each prompt into the report appendix.

### Prompt 0 – Shared duty: reference data, user directory and seed

```text
I am Savidu Herath. For my group I am implementing the reference part of the LankaGuard shared kernel
(package lk.dmc.disaster.shared.reference, inside the OPEN shared module). Lakni provides BaseEntity;
Yohan provides the shared exceptions. Implement and unit-test:
- JPA entities and Spring Data repositories mapped exactly to V1_0_1__shared_reference.sql: District,
  RiverBasin (ManyToMany via district_river_basins), HazardType (report_categories TEXT[] as List<String>
  with @JdbcTypeCode(SqlTypes.ARRAY)), Organisation, ReliefItem, DisasterEvent (+ event_districts),
  AppUser (table users).
- Records: DistrictView, RiverBasinView, HazardTypeView, OrganisationView, ReliefItemView,
  DisasterEventView, UserView(id, fullName, role, districtId, riverBasinId, organisationId, rescueTeamId),
  CitizenContact(userId, phone, preferredLanguage, districtId, riverBasinId).
- interface ReferenceData { districts(); riverBasins(); hazardTypes(boolean activeOnly); hazardType(UUID);
  organisations(Optional<OrganisationType>); reliefItems(); event(UUID); events(Optional<EventStatus>);
  districtExists(UUID); hazardTypeAcceptsCategory(UUID, String) (true only when active and listed);
  districtsInBasins(Set<UUID>); basinsOfDistrict(UUID) } and its implementation (readOnly transactions).
- interface UserDirectory { Optional<UserView> findById(UUID); UserView require(UUID) (NotFoundException);
  List<CitizenContact> findCitizensInAreas(Set<UUID> districtIds, Set<UUID> basinIds) (CITIZEN or
  VOLUNTEER, district in districtIds OR basin in basinIds, distinct); long countCitizensInAreas(...) }
  and its implementation.
- ReferenceController (no X-Acting-User needed): GET /api/reference/districts, /river-basins,
  /hazard-types?activeOnly, /organisations?type, /relief-items, /events?status, /users?role (never return
  NIC or phone).
- V1_0_2__seed_reference.sql copied exactly from the Project Initialisation Guide; docs/SEED.md listing
  every seeded UUID; src/test/java/lk/dmc/disaster/support/TestIds.java with constants for them.
- Tests: unit tests with mocked repositories, plus one Testcontainers test that runs all migrations and
  checks counts (5 districts, 2 basins, 3 hazard types, 9 organisations, 4 items, 49 users, 2 events).
Rules: constructor injection, Javadoc, Spotless, ./mvnw verify green, coverage >= 80% on this package.
Commit as "shared: add reference data and user directory" and "db: add reference seed".
```

### Prompt 1 – Module context (send once, with Sections 2–6 of this document pasted below it)

```text
I am Savidu Herath, owner of the analytics module (UC04 Generate Disaster Response Analysis Report) in
this Spring Boot repository. Read docs/CONTRIBUTING.md and docs/ARCHITECTURE.md if they exist. My full
specification is pasted below.

Stack: Java 21, Spring Boot 4.x, Spring Web, Data JPA, JdbcClient, Validation, Spring Modulith 2.x,
PostgreSQL 16 with Flyway (ddl-auto=validate), MapStruct, Lombok (entities: @Getter and protected no-arg
constructor only, never @Data), OpenPDF, Apache Commons CSV, springdoc, JUnit 5, Mockito, AssertJ,
MockMvc, Testcontainers, JaCoCo, Spotless.

Rules:
1. Package lk.dmc.disaster.analytics; internal web, application, domain, persistence, section, query,
   export. I depend on NO other module's Java types; only lk.dmc.disaster.shared. Other modules' data is
   read with read-only JdbcClient SQL in the query package, returning records.
2. Layers: web -> application -> section/query/export/domain/persistence. Controller calls one service
   method and returns ApiResponse (or the file for export).
3. I write only disaster_reports. Sections, filters and unavailable reasons are stored as JSONB.
4. Sections implement ReportSection and are injected as List<ReportSection>; ReportBuilder runs them in
   SectionKey order. Exporters implement ReportExporter and are injected as List<ReportExporter>.
   A missing dataset returns SectionResult.unavailable(reason) — never zeros.
5. SOLID, constructor injection of interfaces, injected Clock, records, constants in AnalyticsRules,
   @Transactional(readOnly = true) on queries, Javadoc on public types and methods, @RequiresRole
   (generate: DMC_OFFICER; read and export: DMC_OFFICER and DISTRICT_OFFICER).
6. Tests per the test plan; method_condition_expectedResult names; sections tested with mocked queries;
   queries tested with Testcontainers against the real migrations and seed; fixed Clock; coverage >= 80%
   line and branch on every analytics package.
7. Change only lk/dmc/disaster/analytics/**, its tests, V5_0_1__analytics.sql, V6_0_3__seed_event_history.sql,
   docs/modules/analytics.md and docs/api/analytics.yaml. For shared changes, stop and write the request.

Work one step at a time when I ask: plan in 3-5 lines naming the pattern or principle, code, tests,
./mvnw verify, show coverage, stop. First summarise my module in 10 lines and wait for "go".

--- SPECIFICATION (Sections 2-6 of my guide) ---
```

### Prompt 2 – Step prompts (send one at a time)

```text
Step 1: Write V6_0_3__seed_event_history.sql exactly as the seed table describes (ask me for Yohan's
verified report UUIDs if they are not in docs/SEED.md), then the domain: DisasterReport (JSONB), ReportContext,
SectionKey, AnalyticsRules, SectionResult and the four section data records; DisasterReportRepository.

Step 2: Query package: WarningTimelineQuery, ReportTimingQuery, DeliveryStatsQuery, OccupancyQuery,
DistributionQuery, EventSummaryQuery with JdbcClient, each with a Testcontainers test asserting the Kalu
seed numbers.

Step 3: The four ReportSection implementations with mocked-query unit tests covering available,
unavailable and edge cases.

Step 4: ReportBuilder and AnalyticsService (list events, generate with validation, list saved, get) with
tests, including "unknown event saves nothing".

Step 5: Export package: ReportExporter, PdfReportExporter (OpenPDF), CsvReportExporter (Commons CSV),
ExportService, with tests.

Step 6: AnalyticsMapper, response records and AnalyticsController exactly as in the API table, export
with Content-Disposition, @WebMvcTest tests, springdoc annotations.
```

### Prompt 3 – Integration with the frontend

```text
My analytics endpoints run on http://localhost:8080/api with all migrations applied. In frontend/,
compare src/services/analytics/analytics.http.ts and src/types/analytics.ts with my controller and make
the http implementation match exactly; do not edit screens. Set VITE_USE_MOCKS=false and walk through:
as Nimal Perera generate the Kalu Flood May 2026 report (all four sections, 85 min report-to-warning,
peak 290/300), download PDF and CSV and open them; generate for the Kelani event with a two-hour window
(at least one section unavailable with its reason); as Kasun Jayawardena open the saved report (no
generate button) and export it. Also check the other modules' screens show the seeded history. Report
pass or fail per step with the network response for failures.
```

### Prompt 4 – Close coverage gaps

```text
Run ./mvnw verify and read target/site/jacoco/jacoco.xml for lk.dmc.disaster.analytics.* and
lk.dmc.disaster.shared.reference. List every class below 80% line or branch coverage with the uncovered
lines, add meaningful tests with real assertions for those branches only, re-run, and show the new
per-package numbers.
```
