# SafeLanka

Smart Early-Warning Disaster Management System (SE3070 A02): a Smart Disaster Early-Warning and Emergency Coordination platform for Sri Lanka's Disaster Management Centre (DMC).

Citizens submit hazard reports, DMC officers verify them and issue multi-channel warnings, response teams coordinate rescue, shelters and relief, and analytics summarise each disaster event. External systems (SMS/push gateways, river gauges) are simulated behind interfaces.

## Tech stack

| Layer | Technology |
| --- | --- |
| Backend | Java 17 (LTS), Spring Boot 4.1, Spring Modulith 2.1 (modular monolith), Spring Data JPA, Flyway |
| Database | PostgreSQL 16 (Docker), pgAdmin 4 |
| API docs | springdoc-openapi 3 (Swagger UI) |
| Libraries | MapStruct, Lombok, OpenPDF, Commons CSV |
| Quality | Spotless (google-java-format), JaCoCo, Checkstyle, Testcontainers, GitHub Actions CI |
| Frontend | React 19, TypeScript, Vite, React Router, TanStack Query, Recharts, idb |

## Prerequisites

- JDK 17 or newer (the build targets Java 17 LTS; point `JAVA_HOME` at it)
- PostgreSQL 14+: either Docker Desktop **or** a native install (see [Running without Docker](#running-without-docker))
- Node.js 20 or newer
- Git (on Windows use Git Bash; use `mvnw.cmd` instead of `./mvnw` in PowerShell)

## Run

```bash
# 1. Database (PostgreSQL on :5432, pgAdmin on :5050) - or use a native PostgreSQL, see below
docker compose up -d
cp backend/.env.example backend/.env && cp frontend/.env.example frontend/.env.local   # first time only

# 2. Backend
cd backend
./mvnw spotless:apply
./mvnw verify
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 3. Frontend (new terminal)
cd frontend
npm install
npm run dev
```

| Service | URL |
| --- | --- |
| API health | http://localhost:8080/actuator/health |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Frontend | http://localhost:5173 |
| pgAdmin | http://localhost:5050 (`admin@dmc.lk` / `admin`; server host `postgres`, user and password `disaster`) |

Local database credentials (`disaster` / `disaster`) are for development only. Reset the database with `docker compose down -v && docker compose up -d`.

If port 8080 is already used on your machine (for example by a local Tomcat service), start the backend with `-Dspring-boot.run.arguments=--server.port=8081`.

## Secrets and configuration

Real keys never go in Git. Placeholders live in committed `*.example` files; copy them and fill in real values.

| Side | Template (committed) | Your copy (git-ignored) | Contents |
| --- | --- | --- | --- |
| Backend | `backend/.env.example` | `backend/.env` | DB credentials, CORS origins, SMS gateway, FCM, weather and river-gauge keys, test DB |
| Frontend | `frontend/.env.example` | `frontend/.env.local` | API base URL, public maps key, VAPID public key |

- The backend reads `backend/.env` automatically (`spring.config.import` in `application.yml`) when started from `backend/`. Each value is also exposed as `app.integrations.*` configuration, and every one has a safe empty/dev default, so the app runs without them.
- Every `VITE_*` variable is bundled into the browser. Put only public keys in the frontend; private keys belong in `backend/.env`.
- `frontend/.env.development` holds only non-secret defaults and is tracked.

## Running without Docker

Docker is optional; the backend only needs a PostgreSQL instance.

1. Install PostgreSQL 14+ natively (for example `winget install PostgreSQL.PostgreSQL.16`).
2. Create the user and databases: `psql -U postgres -f backend/db-setup.sql` (edit the password first).
3. Put the same credentials in `backend/.env` (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`).
4. Run the backend as usual; Flyway creates all tables and seed data on startup.
5. Run tests against your local server instead of Testcontainers:

```bash
./mvnw verify -Dapp.test.use-testcontainers=false   # uses TEST_DB_* (default database disaster_test)
```

To save disk space while still using Docker, start only the database: `docker compose up -d postgres` (skips the pgAdmin image) and use any native SQL client instead.

## Architecture

The backend is a Spring Boot modular monolith: one deployable API, four member-owned modules plus a group-owned shared kernel. Module boundaries are enforced by `ModularityTests`. Modules talk to each other only through contract types in their root package or Spring Modulith events.

Layers per module: `web` (controllers) → `application` (use cases) → `domain` (entities) → `persistence` (repositories); the module root package holds the public contract.

| Module | Use case | Owner | Writes tables |
| --- | --- | --- | --- |
| reports | UC02 Submit and verify hazard report | Helitha | hazard_reports, report_photos |
| warnings | UC01 Early warnings | Inothma | hazards, hazard_evidence, sensor_readings, warnings, warning_target_areas, warning_reports, notification_deliveries, channel_settings |
| response | UC03 Emergency response | Ranepura | rescue_teams, rescue_assignments, team_status_logs, shelters, occupancy_logs, relief_stocks, resource_allocations, relief_distributions, activity_logs |
| analytics | UC04 Analytics and reporting | Herath | disaster_reports (read-only SQL on other modules' tables) |

`shared/` (API envelope, errors, acting user, reference data, storage) and the V1 / V6_0_1 migrations are owned by the whole group.

## Authentication

Real accounts with Spring Security and JWT. Citizens sign up with a mobile number (email optional) and verify it with an SMS code; everyone logs in with mobile number or email and a password.

| Piece | How it works |
| --- | --- |
| Access token | JWT (HS256), 15 minutes, carries role and district. Kept in memory by the frontend, never in localStorage |
| Refresh token | 7 days, rotating, httpOnly + SameSite=Strict cookie on `/api/auth`, stored hashed in the database. Reusing an old one revokes all of that user's tokens |
| Passwords | BCrypt; 8 to 64 characters with a letter and a number |
| Phone verification | 6-digit code, 5 minutes, 3 tries, 60 s resend cooldown. SMS is mocked: the code is logged and returned only in the `dev` profile |
| Lockout | 5 failed logins lock the account for 15 minutes |
| Bridge to modules | The JWT filter fills the request's `ActingUser`; modules and `@RequiresRole` never see how someone logged in |
| URL rules | `auth/security/AccessRules` is the coarse role matrix; `@RequiresRole` on controller methods is the precise rule |

Endpoints (`/api/auth`): `signup`, `verify-phone`, `resend-code`, `login`, `refresh`, `logout`, `me`.

**Demo accounts** (seeded by `V1_0_3__auth_accounts.sql`, password `Demo@1234` for all):

| Role | Sign in with |
| --- | --- |
| DMC duty officer | nimal.perera@dmc.lk |
| District officer (Colombo / Ratnapura) | kasun.jayawardena@dmc.lk / sanduni.wickramasinghe@dmc.lk |
| Shelter coordinator | dilani.gunasekara@dmc.lk |
| Rescue team member (Army / Navy) | asanka.bandara@dmc.lk / mahesh.kumara@dmc.lk, or 077 100 0007 / 077 100 0008 |
| Citizen | 077 100 0004 (Ruwan), 077 100 0009 (Priya) |
| Volunteer | 077 100 0005 |

Staff accounts exist only through the seed migration; creating staff or granting roles is out of scope.

**SMS (Text.lk):** the `SmsGateway` interface in `shared/sms` has two implementations chosen by `SMS_PROVIDER`: `mock` (default: nothing is sent, the code is logged at DEBUG and shown on screen in dev) and `textlk` (real SMS through Text.lk API v3). Set `SMS_PROVIDER=textlk`, `SMS_TEXTLK_API_KEY` and `SMS_SENDER_ID` (`TextLKDemo` works on any account; your own name needs Text.lk approval) in `backend/.env`. The test profile always uses the mock. With the real gateway the code is never returned to the browser. If the gateway is down, sign-up answers 503 and leaves no half-created account. A new provider (or the warnings SMS channel) only needs a new `SmsGateway` implementation.

**Required secret:** set `JWT_SECRET` in `backend/.env` (at least 32 random characters). The app refuses to start without it.

**Demo fallback:** if login ever breaks during a demo, start the backend with `-Dspring-boot.run.profiles=dev,demo-auth` and the frontend with `VITE_AUTH_MODE=demo`. The role switcher and the `X-Acting-User` header come back and login is bypassed. Never use this outside a demo.

Running the frontend against the real backend: set `VITE_USE_MOCKS=false` and `VITE_PROXY_TARGET` (the backend URL) in `frontend/.env.local`. The Vite dev server proxies `/api`, which keeps the refresh cookie same-origin, also from a phone on the LAN.

## Repository layout

```
.
├─ docker-compose.yml        PostgreSQL + pgAdmin
├─ .github/workflows/ci.yml  backend verify + frontend lint/test
├─ docs/                     architecture, API and module guides
├─ backend/                  Spring Boot (Maven) – lk.dmc.disaster.*
│  └─ src/main/resources/db/migration/   Flyway V1_0_1 … V6_0_1 + V1_0_3 auth (33 tables + seed data)
└─ frontend/                 React + TypeScript + Vite + Tailwind (see frontend/README.md)
   └─ src/{components,constants,context,hooks,navigation,screens,services,theme,types,utils}
      each with reports / warnings / response / analytics module folders

## Frontend

All 25 screens for the five roles are built in the Command dark theme and run on an in-browser mock database that matches the Flyway seed, so the UI works without the backend. Set `VITE_USE_MOCKS=false` in `frontend/.env.local` to call the Spring Boot API instead. Pick a seeded person on the landing screen (no login); details, routes and folder rules are in [frontend/README.md](frontend/README.md).

## Database

Flyway is the single source of schema; Hibernate only validates (`ddl-auto: validate`). One migration file per owner so ownership is visible in Git history. Never edit a merged migration; add a new one. Out-of-order migrations are allowed (`spring.flyway.out-of-order`), so owners can add their versioned files in any order. The Spring Modulith `event_publication` table is created by Modulith itself.

Seed data (fixed UUIDs): 5 districts, 2 river basins, 9 named users + 40 demo residents, 2 disaster events, 6 sensors, 6 shelters, 6 rescue teams, 11 relief stock rows.

## Workflow

- Branch from `main` as `feature/<module>-<topic>`; open a pull request; CI (`backend` check) must pass before merge.
- Run `./mvnw spotless:apply` before committing; `verify` fails on unformatted code.
- JaCoCo's 80% coverage gate is report-only for now (`jacoco.haltOnFailure=false` in `backend/pom.xml`); switch it to `true` on 06 Oct.
- Each member owns their module package, its test folder and one migration file.

## Setup status

- [x] Backend generated, dependencies and build plugins configured
- [x] Configuration (`application*.yml`), bootstrap code, module declarations
- [x] Migrations V1–V6 and V1_0_3 (auth) applied (8 migrations), `./mvnw verify` passes
- [x] Login, sign-up with SMS verification, JWT + rotating refresh cookie, role-based URL rules
- [x] Secret templates (`backend/.env.example`, `frontend/.env.example`) and Docker-free run/test path
- [x] Frontend: 25 screens, services layer with mock and HTTP implementations, offline outbox
- [ ] GitHub repository settings: collaborators and `main` branch protection
