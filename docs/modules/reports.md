# Reports module (UC02) – 10-step implementation guide

Owner: Yohan Helitha · package `lk.dmc.disaster.reports` · folders `controller / service / entity / repository`.

Ground rules for every step

- One step = one commit. Each step ends with `./mvnw verify` green (Spotless check, tests, ModularityTests) and stops so you can commit.
- Tests ship in the same step as the code they cover (positive, negative, edge, error).
- Only reports code, shared-kernel pieces that reports consumes, and this document change. Other modules' APIs (warnings, response, analytics) are **not** implemented; their contracts are only referenced.
- SOLID: one use case per service, small interfaces, constructor injection, injected `Clock`, no `Instant.now()`.
- Format only your files: `./mvnw spotless:apply "-DspotlessFiles=.*lk/dmc/disaster/(reports|shared)/.*"`.

| # | Step | Main deliverables | Tests |
| --- | --- | --- | --- |
| 1 | Shared kernel: envelope, errors, geo | `ApiResponse.page` + `PageMeta`, typed exceptions (`NotFound`, `Conflict`, `InvalidStateTransition`, `BusinessRule`, `ForbiddenRole`), `GeoPoint`, `GeoDistance` | `ApiResponseTest`, `TypedExceptionsTest`, `GeoPointTest`, `GeoDistanceTest` |
| 2 | Shared kernel: ports and adapters | `ReferenceData`, `UserDirectory`, `FileStorage` interfaces + JDBC / local-disk implementations | adapter tests (`@SpringBootTest` on seed data, temp-dir storage) |
| 3 | Domain | `ReportStatus`, `RejectionReason`, `ReportRules`, `ReportStatusMachine`, `HazardReport`, `ReportPhoto` | `ReportStatusMachineTest`, `HazardReportTest` |
| 4 | Persistence | `HazardReportRepository`, `ReportPhotoRepository`, `ReportSpecifications`, `ReferenceNumberGenerator` | `HazardReportRepositoryTest` |
| 5 | Submission use case | contract events/records, `SubmitReportCommand`, `ReportSubmissionService` (idempotency, photo, PENDING) | `ReportSubmissionServiceTest` |
| 6 | Verification use case | `ReviewDecisionCommand`, `ReportVerificationService`, `ReportVerifiedEvent` / `ReportRejectedEvent` publishing | `ReportVerificationServiceTest` |
| 7 | Duplicates and queries | `DuplicateDetector`, `ReportQueryService`, `VerifiedReportQuery` + `VerifiedReportQueryImpl` | `DuplicateDetectorTest`, `ReportQueryServiceTest`, `VerifiedReportQueryImplTest` |
| 8 | Web layer | request/response records, `ReportMapper`, `ReportController`, `ReportReviewController`, springdoc | `ReportControllerTest`, `ReportReviewControllerTest` |
| 9 | Seed data and end-to-end | `V6_0_2__seed_demo_reports.sql`, URL access rules, lifecycle integration test | `ReportLifecycleIT` |
| 10 | Quality gate and docs | coverage ≥ 80% per package, Javadoc, `docs/api/reports.yaml`, frontend check with `VITE_USE_MOCKS=false` | coverage fixes, `ModularityTests` |

## Step details

1. **Shared envelope, errors, geo.** Reports needs paged responses, expressive exceptions and Sri Lanka geometry. All three are pure code with no Spring wiring, so they are safest to land first. Existing `AppException` and `ErrorCode` stay as they are; the typed exceptions only extend them.
2. **Ports and adapters.** `ReferenceData` (hazard type / district checks), `UserDirectory` (reporter name and role), `FileStorage` (photo store and load). Interfaces live in `shared` so reports depends on abstractions (DIP); adapters read the existing `hazard_types`, `districts`, `users` tables and `app.storage.root`.
3. **Domain.** `HazardReport` has a static `submit`, and `verify`, `reject`, `requestInfo` that call `ReportStatusMachine`. No public state setters. Maps exactly to `V2_0_1__reports.sql`.
4. **Persistence.** Spring Data repository with `findByClientRef`, paged reporter query, duplicate-candidate query; Specification for the queue filters; reference numbers from `report_reference_seq`.
5. **Submission.** Validates through `ReferenceData` and `GeoPoint`, enforces the 5-minute clock skew, returns the existing report for the same `clientRef` and reporter (409 for another reporter), stores the photo, saves PENDING.
6. **Verification.** Reviewer cannot be the reporter (422), illegal transitions give 409, events are published inside the transaction.
7. **Duplicates and queries.** 500 m / 2 h duplicate rule, officer queue (oldest first), reporter list (newest first), detail with reporter and duplicates, and the read-only contract warnings consumes.
8. **Web.** Exactly the 8 endpoints of the spec; controllers call one service method and return `ApiResponse`.
9. **Seed and end-to-end.** 12 demo reports, role rules for `/api/reports/**`, and one test that walks submit → verify → event.
10. **Gate and docs.** JaCoCo per package, Javadoc on public API of `service` and the root package, OpenAPI file matches Swagger, screenshots for the report.

## Status and how to verify

All ten steps are implemented. `docs/api/reports.yaml` is the OpenAPI description of the eight endpoints, generated from the running Swagger UI.

Run the module's tests and coverage from `backend/` (PowerShell: put the `-D` options in quotes). With Docker off, point the tests at the shared database; they roll back or clean up after themselves:

```
./mvnw verify "-Dapp.test.use-testcontainers=false" "-Dtest=lk.dmc.disaster.reports.**.*Test" "-Dspotless.check.skip=true"
```

Open `backend/target/site/jacoco/index.html`. The Supabase session pooler allows about 15 connections for the whole team, so avoid several people running the full suite at the same moment; the test profile keeps each Spring context's pool small (`application-test.yml`).

Known limitations: the seed has no photos; the Duplicate flag in the officer queue costs one query per open report on the page.

### Photo storage

Photos are stored through the `FileStorage` interface. `STORAGE_PROVIDER` in `backend/.env` selects the implementation:

| Value | Where photos go | Needs |
| --- | --- | --- |
| `local` (default) | `./uploads` on the machine running the backend; other machines cannot see them | nothing |
| `supabase` | the private Supabase Storage bucket `evidence`, folder `reports/`, shared by the whole team | `SUPABASE_URL`, `SUPABASE_SERVICE_KEY` (secret key, backend only), `SUPABASE_STORAGE_BUCKET` |

The bucket stays private. The backend uploads with the secret key and serves photos only through `GET /api/reports/{id}/photo` after the role check, so only the reporter and DMC officers can see them. The table `report_photos.file_path` holds the object path (`reports/<uuid>.jpg`). A photo whose report could not be saved is deleted again. Tests always use local storage.

### Offline sync

Two requests with the same `clientRef` at the same moment create exactly one report: the loser answers with the winner's report (HTTP 200) and removes its own uploaded photo.
