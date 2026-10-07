# Warnings module (UC01 Assess Hazard and Issue Public Warning)

Owner: Arani Inothma. Package `lk.dmc.disaster.warnings`. Migration `V3_0_1__warnings.sql`.
API: [`docs/api/warnings.yaml`](../api/warnings.yaml).

DMC duty officers assess hazards using verified ground reports and simulated river-gauge readings,
then issue, edit, escalate or cancel public warnings for districts or river basins. A warning goes to
every registered citizen over app push, SMS and an audible alarm. A failing channel never stops the
others.

## Package layout

| Package | Holds |
| --- | --- |
| root | The public contract other modules use: `ActiveWarningQuery`, `ActiveWarningSummary`, `WarningPublishedEvent`, `WarningEscalatedEvent`, `WarningCancelledEvent` |
| `controller` | REST endpoints only. No business rules, no repositories |
| `dto` | Request and response records, with Bean Validation and Swagger examples |
| `mapper` | Service results to response shapes (`HazardMapper`, `WarningMapper`) |
| `service` | Use cases and transactions |
| `entity` | JPA entities, enums, the two state machines, `WarningRules` and small value objects |
| `repository` | Spring Data repositories and optional-filter specifications |
| `integration` | Notification channels and gateway simulators, and the ports and JDBC adapters to other modules |

## Main flow

1. `GET /api/hazards` lists open hazards, most severe first, with evidence count and latest gauge reading.
2. `GET /api/hazards/{id}` shows the gauge chart, verified reports and earlier warnings.
3. `GET /api/warnings/audience` shows how many people a chosen set of areas reaches.
4. `POST /api/warnings` (with `confirm: true`) saves the warning ACTIVE, marks the hazard WARNED,
   creates one delivery per person per enabled channel, and publishes `WarningPublishedEvent`.
5. `GET /api/warnings/{id}` and `/deliveries` show the result, including any failed channel.

Alternative and exception flows (A1 to A5, E1 to E4 of the revised UC01) map to: hazard status
`PATCH`, escalate, edit, cancel, basin targets, the sensor simulator (dev profile), and the 400, 409
and 422 responses listed in the API file.

## Design: patterns and principles

| Pattern or principle | Where |
| --- | --- |
| Strategy | `NotificationChannel`, one class per channel, injected as `List<NotificationChannel>` |
| Adapter | The simulators stand in for FCM, an SMS provider and sirens. A real gateway is one new class |
| State | `WarningStatusMachine`, `HazardStatusMachine` over one shared `StatusMachine` |
| Observer | The publication service publishes three warning events; `HazardEvidenceLinker` is called when a report is verified |
| Single responsibility | Warning rules in `Warning`, pre-checks in `PublishPreconditions`, sending in `NotificationDispatchService`, ordering of steps in `WarningPublicationService`, read side in the `*QueryService` classes |
| Open / Closed | A channel decides which levels it handles (`supports(level)`), so adding one changes no existing class |
| Interface segregation | `ActiveWarningQuery` has two methods; each port (`CitizenDirectory`, `AreaReference`, `VerifiedReports`, `HazardTypeDirectory`, `GatewayFailureSwitch`) is small |
| Dependency inversion | Services depend on those ports, on `NotificationChannel` and on `Clock`, never on concrete gateways or other modules |

Code-smell choices: value objects (`WarningContent`, `WarningTarget`, `WarningDraft`, `HazardArea`)
keep parameter lists short and make invalid data unrepresentable; commands carry the officer's
confirmation instead of a boolean argument; every limit lives in `WarningRules`; collections that
leave an entity are copies.

## Decisions worth knowing

- **Local ports instead of other modules' classes.** Reports and the user directory belong to other
  members. The module reads what it needs through small ports with read-only JDBC adapters, so it
  works without their code. When their queries are merged, each adapter is replaced by a short class
  that calls the real query.
- **Views are built inside the transaction.** `spring.jpa.open-in-view` is `false`, so
  `WarningView` copies a warning's areas, evidence, resolved districts and delivery totals while the
  session is open.
- **Hand-written mappers** instead of MapStruct: the mapping is nested and needs the hazard type
  code lookup.
- **Simulation is dev-only.** `SimulationController` has `@Profile("dev")`. The simulator opens a
  hazard at the alert level, raises its severity at major flood level, and never issues a warning.

## Testing

Unit tests need no database (they run with `./mvnw test`; the three database tests below are
optional). Line and branch coverage by package, measured with JaCoCo on those unit
tests alone:

| Package | Line | Branch |
| --- | --- | --- |
| root | 100% | n/a |
| controller | 100% | n/a |
| dto | 100% | 100% |
| entity | 98.8% | 100% |
| integration | 100% | 96.7% |
| mapper | 100% | 100% |
| repository | 100% | 100% |
| service | 100% | 98.9% |

Database tests (real PostgreSQL with the Flyway schema and seed data; the project does not use Docker, so run them against a local PostgreSQL with `-Dapp.test.use-testcontainers=false`) check the SQL
itself: `WarningRepositoryTest`, `HazardRepositoryTest` and `JdbcDirectoriesTest`.

## Open items

- `@ApplicationModuleListener` on the reports module's verified-report event: the logic and tests
  are in `HazardEvidenceLinker`; the listener is a thin wrapper to add when that event is merged.
- Replace the JDBC adapters with the real reports and user queries once they are merged.
- Optionally run the database tests and `ModularityTests` against a local PostgreSQL; keep the screenshots and
  coverage page for the report.
