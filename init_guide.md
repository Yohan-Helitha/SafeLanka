# SE3070 A02 – Project Initialisation Guide

Oct 4, 2026 · @Yohan

Following the 14 steps below takes the repository from empty to a running Spring Boot API with all 30 tables, seed data, module boundaries and CI in about 2 hours. Every file to create is given in full in Sections 4–9. After this, each member starts their module from a green build.

## 1. Initialisation steps

Spring Boot 3.3 is end-of-life, and Spring Initializr now generates the 4.x line, so this guide uses the Initializr default (Spring Boot 4.x, Spring Modulith 2.x, springdoc 3.x). Java stays at 21.

- [ ] **1. Check prerequisites.** `java -version` shows 21; `docker --version` works and Docker Desktop is running; `node -v` is 20 or newer; Git is installed. On Windows use Git Bash for the commands below and `mvnw.cmd` instead of `./mvnw`.
- [ ] **2. Create the repository.** On GitHub create private repo `sl-disaster-coordination` with a README, add the three teammates as collaborators, then `git clone` it.
- [ ] **3. Create the docs and CI folders** with the last mkdir line in Section 3; the backend packages come after Step 6.
- [ ] **4. Add root files** from Section 4: `docker-compose.yml`, `.gitignore`, `.editorconfig`, `.github/workflows/ci.yml`.
- [ ] **5. Start the database:** `docker compose up -d`, then `docker compose ps` shows postgres as healthy. pgAdmin is at http://localhost:5050.
- [ ] **6. Generate the backend** with the Spring Initializr command in Section 5 and unzip it into `backend/`.
- [ ] **7. Create the backend packages (Section 3), then edit `backend/pom.xml`** with the additions in Section 5.
- [ ] **8. Add configuration** files from Section 6 to `backend/src/main/resources/`.
- [ ] **9. Add bootstrap code** from Section 7: package-info files, `JpaAuditingConfig`, `ModularityTests`.
- [ ] **10. Add migrations** V1 to V6 from Section 8 to `backend/src/main/resources/db/migration/`.
- [ ] **11. Build and run:** `cd backend && ./mvnw verify`, then `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`. Tick the checklist in Section 9.
- [ ] **12. Scaffold the frontend** with the commands in Section 9.
- [ ] **13. Commit and push** (Section 9), then protect `main` in GitHub settings (require a pull request and the CI check).
- [ ] **14. Optional:** run the Backend Initialisation Prompt to generate the shared kernel and module guides. Start the message with: "The repository skeleton, pom.xml, configuration and migrations V1–V6 already exist; keep them and complete the remaining parts."

## 2. Architecture

The backend is a Spring Boot modular monolith: one deployable API, four member-owned modules plus a group-owned shared kernel, and boundaries that a test enforces.

&#91;embedded content: system architecture · clients, Spring Boot modules, database, mocks\]

Every request passes the shared filters into exactly one module. Modules reach each other only through contract types in their root package or through Spring Modulith events; external systems (notification gateways, river gauges) are simulated behind interfaces, as the assignment FAQ allows.

| Layer | Package | Responsibility |
| --- | --- | --- |
| Web « boundary » | `<module>.web` | `@RestController` + request / response records; no business rules |
| Application « control » | `<module>.application` | Use-case services, `@Transactional`, events, provided-contract implementations |
| Domain « entity » | `<module>.domain` | JPA entities, enums, state machines, `<Module>Rules` constants |
| Persistence | `<module>.persistence` | Spring Data repositories (analytics: read-only JdbcClient queries) |
| Contract | `<module>` root package | Interfaces, records and events other modules may use |

| Module | Owner | Writes tables | Talks to |
| --- | --- | --- | --- |
| reports (UC02) | Helitha | hazard\_reports, report\_photos | publishes `ReportVerifiedEvent`; provides `VerifiedReportQuery` |
| warnings (UC01) | Inothma | hazards, hazard\_evidence, sensor\_readings, warnings, warning\_target\_areas, warning\_reports, notification\_deliveries, channel\_settings | listens to `ReportVerifiedEvent`; calls `VerifiedReportQuery`; publishes `Warning*Event`; provides `ActiveWarningQuery` |
| response (UC03) | Ranepura | rescue\_teams, rescue\_assignments, team\_status\_logs, shelters, occupancy\_logs, relief\_stocks, resource\_allocations, relief\_distributions, activity\_logs | listens to `Warning*Event`; calls `ActiveWarningQuery` |
| analytics (UC04) | Herath | disaster\_reports | read-only SQL on the other modules' tables |

## 3. Project folder structure

One repository holds the Maven backend, the Vite frontend and the shared docs. Each member owns one module package, its test folder and one migration file; `shared/` and the V1 / V6\_0\_1 migrations are owned by the group.

```
sl-disaster-coordination/
├─ README.md
├─ docker-compose.yml
├─ .gitignore  .editorconfig
├─ .github/workflows/ci.yml
├─ docs/
│  ├─ ARCHITECTURE.md  CONTRIBUTING.md  DECISIONS.md  SEED.md
│  ├─ api/        conventions.md, shared.yaml, reports.yaml, warnings.yaml, response.yaml, analytics.yaml
│  └─ modules/    reports.md, warnings.md, response.md, analytics.md
│
├─ backend/
│  ├─ pom.xml  mvnw  mvnw.cmd  .mvn/
│  └─ src/
│     ├─ main/java/lk/dmc/disaster/
│     │  ├─ DisasterApplication.java
│     │  ├─ shared/                      GROUP (open module)
│     │  │  ├─ api/          ApiResponse, PageMeta, ErrorBody
│     │  │  ├─ error/        ErrorCode, AppException + subclasses, GlobalExceptionHandler
│     │  │  ├─ actor/        ActingUser, ActingUserFilter, RequiresRole, RoleInterceptor
│     │  │  ├─ domain/       BaseEntity, GeoPoint, GeoDistance, shared enums
│     │  │  ├─ reference/    District, RiverBasin, HazardType, Organisation, ReliefItem,
│     │  │  │                DisasterEvent, AppUser, ReferenceData, UserDirectory, ReferenceController
│     │  │  ├─ storage/      FileStorage, LocalFileStorage
│     │  │  └─ config/       JpaAuditingConfig, ClockConfig, OpenApiConfig, WebConfig, CorsConfig
│     │  ├─ reports/                     UC02 – Helitha
│     │  │  ├─ (root)        VerifiedReportQuery, VerifiedReportFilter, VerifiedReportSummary,
│     │  │  │                ReportVerifiedEvent, ReportRejectedEvent
│     │  │  └─ web/  application/  domain/  persistence/
│     │  ├─ warnings/                    UC01 – Inothma
│     │  │  ├─ (root)        ActiveWarningQuery, ActiveWarningSummary,
│     │  │  │                WarningPublishedEvent, WarningEscalatedEvent, WarningCancelledEvent
│     │  │  ├─ web/  application/  domain/  persistence/
│     │  │  ├─ channel/      NotificationChannel + Push / SMS / Audible simulators
│     │  │  └─ simulation/   SensorFeedSimulator, SimulationController (dev profile)
│     │  ├─ response/                    UC03 – Ranepura
│     │  │  ├─ (root)        AssignmentStatusChangedEvent, ShelterOccupancyChangedEvent
│     │  │  └─ web/  application/  domain/  persistence/
│     │  └─ analytics/                   UC04 – Herath
│     │     ├─ web/  application/  domain/  persistence/
│     │     ├─ section/      ReportSection + 4 implementations
│     │     ├─ query/        read-only JdbcClient queries
│     │     └─ export/       PdfReportExporter, CsvReportExporter
│     ├─ main/resources/
│     │  ├─ application.yml  application-dev.yml  application-test.yml
│     │  └─ db/migration/
│     │     ├─ V1_0_1__shared_reference.sql      group
│     │     ├─ V1_0_2__seed_reference.sql        group
│     │     ├─ V2_0_1__reports.sql               Helitha
│     │     ├─ V3_0_1__warnings.sql              Inothma
│     │     ├─ V4_0_1__response.sql              Ranepura
│     │     ├─ V5_0_1__analytics.sql             Herath
│     │     ├─ V6_0_1__seed_master_data.sql      group
│     │     ├─ V6_0_2__seed_demo_reports.sql     Helitha (later)
│     │     └─ V6_0_3__seed_kalu_history.sql     Herath (later)
│     └─ test/java/lk/dmc/disaster/
│        ├─ DisasterApplicationTests.java     (generated, context + Flyway check)
│        ├─ TestcontainersConfiguration.java  (generated)
│        ├─ ModularityTests.java
│        ├─ support/      FixedClock, TestIds (seed UUIDs)
│        └─ shared/  reports/  warnings/  response/  analytics/
│
└─ frontend/
   └─ src/
      ├─ shared/      api/, offline/, components/, i18n/, theme/
      └─ features/    reports/, warnings/, response/, analytics/
```

**Create the backend packages** after Step 6 (Git does not track empty folders, so each folder gets a `package-info.java` in Section 7):

```bash
cd backend/src/main/java/lk/dmc/disaster
mkdir -p shared/{api,error,actor,domain,reference,storage,config}
mkdir -p reports/{web,application,domain,persistence}
mkdir -p warnings/{web,application,domain,persistence,channel,simulation}
mkdir -p response/{web,application,domain,persistence}
mkdir -p analytics/{web,application,domain,persistence,section,query,export}
cd -
mkdir -p backend/src/main/resources/db/migration
mkdir -p backend/src/test/java/lk/dmc/disaster/{support,shared,reports,warnings,response,analytics}
mkdir -p docs/api docs/modules .github/workflows
```

## 4. Root files

These four files sit at the repository root. The database credentials are for local development only.

**`docker-compose.yml`**

```yaml
services:
  postgres:
    image: postgres:16-alpine
    container_name: disaster-postgres
    environment:
      POSTGRES_DB: disaster
      POSTGRES_USER: disaster
      POSTGRES_PASSWORD: disaster
      TZ: UTC
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U disaster -d disaster"]
      interval: 5s
      timeout: 5s
      retries: 10

  pgadmin:
    image: dpage/pgadmin4
    container_name: disaster-pgadmin
    environment:
      PGADMIN_DEFAULT_EMAIL: admin@dmc.lk
      PGADMIN_DEFAULT_PASSWORD: admin
    ports:
      - "5050:80"
    depends_on:
      postgres:
        condition: service_healthy

volumes:
  pgdata:
```

To reset the database completely: `docker compose down -v && docker compose up -d`.

**`.gitignore`**

```text
# Java / Maven
backend/target/
backend/uploads/
*.class
*.log

# IDE
.idea/
*.iml
.vscode/

# OS
.DS_Store
Thumbs.db

# Frontend
frontend/node_modules/
frontend/dist/
frontend/coverage/

# Local secrets
.env
*.local
```

**`.editorconfig`**

```ini
root = true

[*]
charset = utf-8
end_of_line = lf
insert_final_newline = true
trim_trailing_whitespace = true
indent_style = space
indent_size = 2

[*.{sql,xml}]
indent_size = 4

[*.md]
trim_trailing_whitespace = false
```

**`.github/workflows/ci.yml`**

```yaml
name: CI

on:
  push:
    branches: [main, "feature/**"]
  pull_request:

jobs:
  backend:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: backend
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "21"
          cache: maven
      - run: chmod +x mvnw
      - run: ./mvnw -B verify          # Testcontainers uses the runner's Docker
      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: jacoco-report
          path: backend/target/site/jacoco

  frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        if: hashFiles('frontend/package-lock.json') != ''
        with:
          node-version: "20"
          cache: npm
          cache-dependency-path: frontend/package-lock.json
      - if: hashFiles('frontend/package-lock.json') != ''
        working-directory: frontend
        run: npm ci && npm run lint --if-present && npm test --if-present
```

Create a short `README.md` too: what the system is, prerequisites, the run commands from Section 9, and the module-owner table from Section 2.

## 5. Backend build

Let Spring Initializr produce the base `pom.xml`, so every starter name and version matches the current Spring Boot release; then add the six extra libraries and three build plugins below.

**Generate** (run at the repository root):

```bash
curl https://start.spring.io/starter.zip \
  -d type=maven-project -d language=java -d javaVersion=21 \
  -d groupId=lk.dmc -d artifactId=disaster-backend -d name=disaster \
  -d packageName=lk.dmc.disaster \
  -d description="Smart Disaster Early-Warning and Emergency Coordination backend" \
  -d dependencies=web,data-jpa,validation,actuator,postgresql,flyway,lombok,testcontainers,modulith \
  -o backend.zip
unzip backend.zip -d backend && rm backend.zip
```

Or use https://start.spring.io with the same values: Maven, Java 21, default Spring Boot version, Group `lk.dmc`, Artifact `disaster-backend`, Name `disaster` (gives `DisasterApplication`), Package `lk.dmc.disaster`, dependencies Spring Web, Spring Data JPA, Validation, Actuator, PostgreSQL Driver, Flyway Migration, Lombok, Testcontainers, Spring Modulith. The zip already contains `TestcontainersConfiguration` and a context-load test; keep both.

**Add to `<properties>`** (check Maven Central for the newest patch of each; springdoc must be the 3.x line for Spring Boot 4):

```xml
<java.version>21</java.version>
<springdoc.version>3.0.0</springdoc.version>
<mapstruct.version>1.6.3</mapstruct.version>
<lombok-mapstruct-binding.version>0.2.0</lombok-mapstruct-binding.version>
<openpdf.version>2.0.3</openpdf.version>
<commons-csv.version>1.14.0</commons-csv.version>
<!-- Coverage gate: keep false while modules are empty; set to true on 06 Oct -->
<jacoco.haltOnFailure>false</jacoco.haltOnFailure>
```

**Add to `<dependencies>`**:

```xml
<!-- Event publication registry for @ApplicationModuleListener (version from the Modulith BOM) -->
<dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-starter-jdbc</artifactId>
</dependency>
<!-- Swagger UI at /swagger-ui.html -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>${springdoc.version}</version>
</dependency>
<!-- Entity <-> DTO mapping -->
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>${mapstruct.version}</version>
</dependency>
<!-- UC04 export -->
<dependency>
    <groupId>com.github.librepdf</groupId>
    <artifactId>openpdf</artifactId>
    <version>${openpdf.version}</version>
</dependency>
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-csv</artifactId>
    <version>${commons-csv.version}</version>
</dependency>
<!-- Module diagrams written by ModularityTests -->
<dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-docs</artifactId>
    <scope>test</scope>
</dependency>
```

**Annotation processors.** Initializr already configures `maven-compiler-plugin` with Lombok in `<annotationProcessorPaths>`. Add MapStruct and the binding after Lombok, keeping Lombok first:

```xml
<path>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct-processor</artifactId>
    <version>${mapstruct.version}</version>
</path>
<path>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok-mapstruct-binding</artifactId>
    <version>${lombok-mapstruct-binding.version}</version>
</path>
```

Also add `<compilerArgs><arg>-Amapstruct.defaultComponentModel=spring</arg></compilerArgs>` to the same plugin's `<configuration>` so mappers become Spring beans.

**Add to `<build><plugins>`**:

```xml
<!-- Formatting: ./mvnw spotless:apply fixes, verify fails on unformatted code -->
<plugin>
    <groupId>com.diffplug.spotless</groupId>
    <artifactId>spotless-maven-plugin</artifactId>
    <version>2.44.4</version>
    <configuration>
        <java>
            <googleJavaFormat/>
            <removeUnusedImports/>
        </java>
    </configuration>
    <executions>
        <execution>
            <goals><goal>check</goal></goals>
        </execution>
    </executions>
</plugin>

<!-- Coverage: report + 80% line and branch gate on every package -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.13</version>
    <configuration>
        <excludes>
            <exclude>**/DisasterApplication.class</exclude>
            <exclude>**/config/**</exclude>
            <exclude>**/*MapperImpl.class</exclude>
            <exclude>**/package-info.class</exclude>
        </excludes>
    </configuration>
    <executions>
        <execution>
            <id>prepare-agent</id>
            <goals><goal>prepare-agent</goal></goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>verify</phase>
            <goals><goal>report</goal></goals>
        </execution>
        <execution>
            <id>check</id>
            <phase>verify</phase>
            <goals><goal>check</goal></goals>
            <configuration>
                <rules>
                    <rule>
                        <element>PACKAGE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                            <limit>
                                <counter>BRANCH</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>

<!-- Style report (does not fail the build; fix warnings in the IDE) -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-checkstyle-plugin</artifactId>
    <version>3.6.0</version>
    <configuration>
        <configLocation>google_checks.xml</configLocation>
        <consoleOutput>true</consoleOutput>
        <failOnViolation>false</failOnViolation>
    </configuration>
    <executions>
        <execution>
            <phase>verify</phase>
            <goals><goal>check</goal></goals>
        </execution>
    </executions>
</plugin>
```

Run `./mvnw spotless:apply` once after adding Spotless so the generated files are formatted, then `./mvnw verify` must pass.

## 6. Configuration

Delete the generated `application.properties` and create these three YAML files in `backend/src/main/resources/`. Hibernate only validates the schema; Flyway is the single source of table definitions.

**`application.yml`**

```yaml
spring:
  application:
    name: disaster-backend
  datasource:
    url: jdbc:postgresql://localhost:5432/disaster
    username: disaster
    password: disaster
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        jdbc:
          time_zone: UTC
  flyway:
    locations: classpath:db/migration
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 6MB
  modulith:
    events:
      jdbc:
        schema-initialization:
          enabled: true          # Modulith creates its event_publication table
      republish-outstanding-events-on-restart: true

server:
  port: 8080
  error:
    include-stacktrace: never

springdoc:
  swagger-ui:
    path: /swagger-ui.html
    operations-sorter: method

management:
  endpoints:
    web:
      exposure:
        include: health,info

app:
  storage:
    root: ./uploads
  cors:
    allowed-origins: http://localhost:5173
  paging:
    max-page-size: 100
```

**`application-dev.yml`** (local runs; enables the sensor and channel simulation endpoints)

```yaml
logging:
  level:
    lk.dmc.disaster: DEBUG
    org.springframework.modulith: DEBUG

app:
  simulation:
    enabled: true
```

**`application-test.yml`** (tests; the database comes from the generated `TestcontainersConfiguration` through `@ServiceConnection`)

```yaml
logging:
  level:
    lk.dmc.disaster: INFO

app:
  storage:
    root: ./target/test-uploads
  simulation:
    enabled: false
```

The event publication table is created by Spring Modulith, not by Flyway, so this replaces `V1_0_3__event_publication.sql` from the initialisation prompt.

## 7. Bootstrap code

Only structural code goes in at initialisation: module declarations, package documentation, JPA auditing and the modularity test. Business code is written by each owner.

**`shared/package-info.java`** — an OPEN module, so every module may use all of its subpackages:

```java
/**
 * Shared kernel: API envelope, error handling, acting user, reference data and file storage.
 * Owned by the whole group; change only through a reviewed pull request.
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Shared kernel",
    type = org.springframework.modulith.ApplicationModule.Type.OPEN)
package lk.dmc.disaster.shared;
```

**Module root `package-info.java`** — one per module; the root package is the module's public contract:

```java
/**
 * UC02 Submit and Verify Hazard Report. Owner: Helitha Y. M. Y.
 *
 * <p>Public contract (this package only): VerifiedReportQuery, VerifiedReportFilter,
 * VerifiedReportSummary, ReportVerifiedEvent, ReportRejectedEvent. Subpackages are internal.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Reports (UC02)")
package lk.dmc.disaster.reports;
```

| Module package | displayName | Owner | Public contract types |
| --- | --- | --- | --- |
| `lk.dmc.disaster.reports` | Reports (UC02) | Helitha Y. M. Y | VerifiedReportQuery, VerifiedReportFilter, VerifiedReportSummary, ReportVerifiedEvent, ReportRejectedEvent |
| `lk.dmc.disaster.warnings` | Warnings (UC01) | Inothma Y. M. A. | ActiveWarningQuery, ActiveWarningSummary, WarningPublishedEvent, WarningEscalatedEvent, WarningCancelledEvent |
| `lk.dmc.disaster.response` | Response (UC03) | R. D. L. S. I. Ranepura | AssignmentStatusChangedEvent, ShelterOccupancyChangedEvent |
| `lk.dmc.disaster.analytics` | Analytics (UC04) | H. M. S. C. K. Herath | none |

**Internal subpackage `package-info.java`** — Javadoc only, so Git tracks the folder and readers know what belongs there:

```java
/** web: REST controllers and request/response records for UC02. No business logic, no repositories. */
package lk.dmc.disaster.reports.web;
```

Use the same one-line pattern for every subpackage: `application` (use-case services, transactions, events), `domain` (entities, enums, state machines, Rules constants), `persistence` (Spring Data repositories), and the extra `channel`, `simulation`, `section`, `query`, `export` packages.

**`shared/config/JpaAuditingConfig.java`** — fills `created_at` / `updated_at` through `BaseEntity`:

```java
package lk.dmc.disaster.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** Enables JPA auditing for {@code @CreatedDate} and {@code @LastModifiedDate} fields. */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {}
```

**`src/test/java/lk/dmc/disaster/ModularityTests.java`** — fails the build when a module uses another module's internals:

```java
package lk.dmc.disaster;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModularityTests {

  private final ApplicationModules modules = ApplicationModules.of(DisasterApplication.class);

  @Test
  void modulesRespectTheirBoundaries() {
    modules.verify();
  }

  @Test
  void writesModuleDiagrams() {
    new Documenter(modules).writeModulesAsPlantUml().writeIndividualModulesAsPlantUml();
  }
}
```

**Generated tests.** Keep `TestcontainersConfiguration` and `DisasterApplicationTests` from Initializr; add `@ActiveProfiles("test")` to the test class. Its context-load test starts PostgreSQL in Docker, runs every Flyway migration and validates the JPA mappings, so a broken migration fails CI immediately.

## 8. Database schema and migrations

The schema is 30 tables in seven Flyway files, one per owner, so ownership is visible in Git history. Enums are `VARCHAR` + `CHECK` so JPA `@Enumerated(EnumType.STRING)` maps directly; IDs are UUIDs from `gen_random_uuid()` (built into PostgreSQL 13+); every timestamp is `TIMESTAMPTZ`.

&#91;embedded content: entity relationships · core tables coloured by owning module\]

Colour shows which module writes each table. Not drawn: every table links to `districts`; `hazard_evidence` and `warning_reports` join verified reports to hazards and warnings; `team_status_logs` and `activity_logs` hang off rescue teams and districts.

| File | Owner | Creates |
| --- | --- | --- |
| `V1_0_1__shared_reference.sql` | Group | districts, river\_basins, district\_river\_basins, hazard\_types, organisations, relief\_items, users, disaster\_events, event\_districts |
| `V1_0_2__seed_reference.sql` | Group | Fixed-UUID reference data, 9 named users, 40 demo residents, 2 events |
| `V2_0_1__reports.sql` | Helitha | report\_reference\_seq, hazard\_reports, report\_photos |
| `V3_0_1__warnings.sql` | Inothma | sensors, sensor\_readings, hazards, hazard\_evidence, warnings, warning\_target\_areas, warning\_reports, notification\_deliveries, channel\_settings |
| `V4_0_1__response.sql` | Ranepura | rescue\_teams, shelters, rescue\_assignments, team\_status\_logs, occupancy\_logs, relief\_stocks, resource\_allocations, relief\_distributions, activity\_logs |
| `V5_0_1__analytics.sql` | Herath | disaster\_reports |
| `V6_0_1__seed_master_data.sql` | Group | Sensors, shelters, rescue teams, relief stock for the active Kelani event |

Owners add `V6_0_2__seed_demo_reports.sql` (Helitha) and `V6_0_3__seed_kalu_history.sql` (Herath: warnings, deliveries, occupancy logs and distributions for the closed Kalu event) later. Never edit a migration after it is merged; add a new one instead.

**Fixed UUID scheme** used by the seed files and by `TestIds` in tests: `00000000-0000-0000-TTTT-0000000000NN`, where TTTT is the table code (0001 districts, 0002 basins, 0003 hazard types, 0004 organisations, 0005 relief items, 0006 users, 0007 events, 0008 sensors, 0009 shelters, 0010 rescue teams, 0011 stock) and NN the row.

### V1\_0\_1\_\_shared\_reference.sql

```sql
-- Owner: group. Reference data used by every module.

CREATE TABLE districts (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(10)  NOT NULL UNIQUE,
    name        VARCHAR(60)  NOT NULL,
    province    VARCHAR(60)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE river_basins (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(20)  NOT NULL UNIQUE,
    name        VARCHAR(60)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE district_river_basins (
    district_id     UUID NOT NULL REFERENCES districts (id),
    river_basin_id  UUID NOT NULL REFERENCES river_basins (id),
    PRIMARY KEY (district_id, river_basin_id)
);

CREATE TABLE hazard_types (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code               VARCHAR(30)  NOT NULL UNIQUE,
    name               VARCHAR(60)  NOT NULL,
    onset_speed        VARCHAR(10)  NOT NULL CHECK (onset_speed IN ('RAPID', 'SLOW')),
    report_categories  TEXT[]       NOT NULL,
    active             BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE organisations (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name           VARCHAR(120) NOT NULL,
    type           VARCHAR(20)  NOT NULL
                   CHECK (type IN ('GOVERNMENT', 'ARMED_FORCES', 'POLICE', 'NGO', 'PRIVATE_DONOR')),
    contact_phone  VARCHAR(20),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE relief_items (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(30)  NOT NULL UNIQUE,
    name        VARCHAR(80)  NOT NULL,
    unit        VARCHAR(20)  NOT NULL,
    category    VARCHAR(20)  NOT NULL
                CHECK (category IN ('FOOD', 'WATER', 'MEDICINE', 'HYGIENE', 'SHELTER_KIT')),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE users (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role                VARCHAR(30)  NOT NULL
                        CHECK (role IN ('CITIZEN', 'VOLUNTEER', 'DMC_OFFICER', 'DISTRICT_OFFICER',
                                        'RESCUE_MEMBER', 'SHELTER_COORDINATOR')),
    full_name           VARCHAR(120) NOT NULL,
    phone               VARCHAR(20),
    nic                 VARCHAR(12)  UNIQUE,
    home_address        VARCHAR(200),
    district_id         UUID         NOT NULL REFERENCES districts (id),
    river_basin_id      UUID         REFERENCES river_basins (id),
    preferred_language  VARCHAR(2)   NOT NULL DEFAULT 'en' CHECK (preferred_language IN ('en', 'si', 'ta')),
    organisation_id     UUID         REFERENCES organisations (id),
    rescue_team_id      UUID,        -- foreign key added in V4 (response)
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_users_role_district ON users (role, district_id);
CREATE INDEX idx_users_river_basin   ON users (river_basin_id);

CREATE TABLE disaster_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(120) NOT NULL,
    hazard_type_id  UUID         NOT NULL REFERENCES hazard_types (id),
    status          VARCHAR(10)  NOT NULL CHECK (status IN ('ACTIVE', 'CLOSED')),
    started_at      TIMESTAMPTZ  NOT NULL,
    ended_at        TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_event_dates  CHECK (ended_at IS NULL OR ended_at >= started_at),
    CONSTRAINT chk_event_closed CHECK (status = 'ACTIVE' OR ended_at IS NOT NULL)
);

CREATE TABLE event_districts (
    event_id     UUID NOT NULL REFERENCES disaster_events (id),
    district_id  UUID NOT NULL REFERENCES districts (id),
    PRIMARY KEY (event_id, district_id)
);
```

### V1\_0\_2\_\_seed\_reference.sql

```sql
-- Owner: group. Fixed UUIDs: 00000000-0000-0000-TTTT-0000000000NN (see Section 8).

INSERT INTO districts (id, code, name, province) VALUES
 ('00000000-0000-0000-0001-000000000001', 'CMB', 'Colombo',   'Western'),
 ('00000000-0000-0000-0001-000000000002', 'GAM', 'Gampaha',   'Western'),
 ('00000000-0000-0000-0001-000000000003', 'KAL', 'Kalutara',  'Western'),
 ('00000000-0000-0000-0001-000000000004', 'RAT', 'Ratnapura', 'Sabaragamuwa'),
 ('00000000-0000-0000-0001-000000000005', 'KEG', 'Kegalle',   'Sabaragamuwa');

INSERT INTO river_basins (id, code, name) VALUES
 ('00000000-0000-0000-0002-000000000001', 'KELANI', 'Kelani Ganga'),
 ('00000000-0000-0000-0002-000000000002', 'KALU',   'Kalu Ganga');

INSERT INTO district_river_basins (district_id, river_basin_id) VALUES
 ('00000000-0000-0000-0001-000000000001', '00000000-0000-0000-0002-000000000001'),  -- Colombo   / Kelani
 ('00000000-0000-0000-0001-000000000002', '00000000-0000-0000-0002-000000000001'),  -- Gampaha   / Kelani
 ('00000000-0000-0000-0001-000000000005', '00000000-0000-0000-0002-000000000001'),  -- Kegalle   / Kelani
 ('00000000-0000-0000-0001-000000000004', '00000000-0000-0000-0002-000000000002'),  -- Ratnapura / Kalu
 ('00000000-0000-0000-0001-000000000003', '00000000-0000-0000-0002-000000000002');  -- Kalutara  / Kalu

-- DROUGHT is inactive: enabling it is a data change, not a code change (Phase 2 flexibility).
INSERT INTO hazard_types (id, code, name, onset_speed, report_categories, active) VALUES
 ('00000000-0000-0000-0003-000000000001', 'FLOOD',     'Flood',     'RAPID',
  ARRAY['RISING_WATER', 'BLOCKED_ROAD', 'OTHER'], TRUE),
 ('00000000-0000-0000-0003-000000000002', 'LANDSLIDE', 'Landslide', 'RAPID',
  ARRAY['LANDSLIDE_CRACK', 'BLOCKED_ROAD', 'OTHER'], TRUE),
 ('00000000-0000-0000-0003-000000000003', 'DROUGHT',   'Drought',   'SLOW',
  ARRAY['WATER_SHORTAGE', 'OTHER'], FALSE);

INSERT INTO organisations (id, name, type) VALUES
 ('00000000-0000-0000-0004-000000000001', 'Disaster Management Centre',  'GOVERNMENT'),
 ('00000000-0000-0000-0004-000000000002', 'Ministry of Health',          'GOVERNMENT'),
 ('00000000-0000-0000-0004-000000000003', 'Sri Lanka Army',              'ARMED_FORCES'),
 ('00000000-0000-0000-0004-000000000004', 'Sri Lanka Navy',              'ARMED_FORCES'),
 ('00000000-0000-0000-0004-000000000005', 'Sri Lanka Air Force',         'ARMED_FORCES'),
 ('00000000-0000-0000-0004-000000000006', 'Sri Lanka Police',            'POLICE'),
 ('00000000-0000-0000-0004-000000000007', 'Sri Lanka Red Cross Society', 'NGO'),
 ('00000000-0000-0000-0004-000000000008', 'Sarvodaya',                   'NGO'),
 ('00000000-0000-0000-0004-000000000009', 'Private Donor Network',       'PRIVATE_DONOR');

INSERT INTO relief_items (id, code, name, unit, category) VALUES
 ('00000000-0000-0000-0005-000000000001', 'DRY_RATION',  'Dry ration pack',    'packs',   'FOOD'),
 ('00000000-0000-0000-0005-000000000002', 'WATER_5L',    'Drinking water 5 L', 'bottles', 'WATER'),
 ('00000000-0000-0000-0005-000000000003', 'FIRST_AID',   'First aid kit',      'kits',    'MEDICINE'),
 ('00000000-0000-0000-0005-000000000004', 'HYGIENE_KIT', 'Hygiene kit',        'kits',    'HYGIENE');

-- One named user per role (the frontend role switcher uses these).
INSERT INTO users (id, role, full_name, phone, nic, home_address, district_id, river_basin_id,
                   preferred_language, organisation_id) VALUES
 ('00000000-0000-0000-0006-000000000001', 'DMC_OFFICER',         'Nimal Perera',           '+94771000001', NULL, NULL,
  '00000000-0000-0000-0001-000000000001', NULL, 'en', '00000000-0000-0000-0004-000000000001'),
 ('00000000-0000-0000-0006-000000000002', 'DISTRICT_OFFICER',    'Kasun Jayawardena',      '+94771000002', NULL, NULL,
  '00000000-0000-0000-0001-000000000001', NULL, 'en', '00000000-0000-0000-0004-000000000001'),
 ('00000000-0000-0000-0006-000000000003', 'DISTRICT_OFFICER',    'Sanduni Wickramasinghe', '+94771000003', NULL, NULL,
  '00000000-0000-0000-0001-000000000004', NULL, 'en', '00000000-0000-0000-0004-000000000001'),
 ('00000000-0000-0000-0006-000000000004', 'CITIZEN',             'Ruwan Fernando',         '+94771000004', '199012345678',
  '45 Station Road, Kolonnawa', '00000000-0000-0000-0001-000000000001', '00000000-0000-0000-0002-000000000001', 'si', NULL),
 ('00000000-0000-0000-0006-000000000005', 'VOLUNTEER',           'Tharindu Silva',         '+94771000005', '199534567890',
  '12 Temple Lane, Biyagama', '00000000-0000-0000-0001-000000000002', '00000000-0000-0000-0002-000000000001', 'si', NULL),
 ('00000000-0000-0000-0006-000000000006', 'SHELTER_COORDINATOR', 'Dilani Gunasekara',      '+94771000006', NULL, NULL,
  '00000000-0000-0000-0001-000000000001', NULL, 'si', '00000000-0000-0000-0004-000000000001'),
 ('00000000-0000-0000-0006-000000000007', 'RESCUE_MEMBER',       'Asanka Bandara',         '+94771000007', NULL, NULL,
  '00000000-0000-0000-0001-000000000001', NULL, 'si', '00000000-0000-0000-0004-000000000003'),
 ('00000000-0000-0000-0006-000000000008', 'RESCUE_MEMBER',       'Mahesh Kumara',          '+94771000008', NULL, NULL,
  '00000000-0000-0000-0001-000000000001', NULL, 'si', '00000000-0000-0000-0004-000000000004'),
 ('00000000-0000-0000-0006-000000000009', 'CITIZEN',             'Priya Shanmugam',        '+94771000009', '199278901234',
  '8 River View, Kalutara', '00000000-0000-0000-0001-000000000003', '00000000-0000-0000-0002-000000000002', 'ta', NULL);

-- 40 demo residents (ids ...0006-000000000101 to ...140) across the 5 districts; every 4th is a volunteer.
INSERT INTO users (id, role, full_name, phone, nic, home_address, district_id, river_basin_id, preferred_language)
SELECT ('00000000-0000-0000-0006-' || lpad(n::text, 12, '0'))::uuid,
       CASE WHEN n % 4 = 0 THEN 'VOLUNTEER' ELSE 'CITIZEN' END,
       'Demo Resident ' || n,
       '+9477' || lpad((2000000 + n)::text, 7, '0'),
       '1985' || lpad(n::text, 8, '0'),
       'Demo address ' || n,
       d.id,
       drb.river_basin_id,
       (ARRAY['si', 'ta', 'en'])[1 + n % 3]
FROM generate_series(101, 140) AS n
JOIN districts d ON d.code = (ARRAY['CMB', 'GAM', 'KAL', 'RAT', 'KEG'])[1 + n % 5]
JOIN district_river_basins drb ON drb.district_id = d.id;

INSERT INTO disaster_events (id, name, hazard_type_id, status, started_at, ended_at) VALUES
 ('00000000-0000-0000-0007-000000000001', 'Kelani Flood October 2026', '00000000-0000-0000-0003-000000000001',
  'ACTIVE', '2026-10-01 06:00:00+05:30', NULL),
 ('00000000-0000-0000-0007-000000000002', 'Kalu Flood May 2026',       '00000000-0000-0000-0003-000000000001',
  'CLOSED', '2026-05-14 04:00:00+05:30', '2026-05-21 18:00:00+05:30');

INSERT INTO event_districts (event_id, district_id) VALUES
 ('00000000-0000-0000-0007-000000000001', '00000000-0000-0000-0001-000000000001'),
 ('00000000-0000-0000-0007-000000000001', '00000000-0000-0000-0001-000000000002'),
 ('00000000-0000-0000-0007-000000000002', '00000000-0000-0000-0001-000000000004'),
 ('00000000-0000-0000-0007-000000000002', '00000000-0000-0000-0001-000000000003');
```

### V2\_0\_1\_\_reports.sql (Helitha)

```sql
-- Owner: reports module (UC02).

CREATE SEQUENCE report_reference_seq START WITH 1 INCREMENT BY 1;   -- RPT-2026-0001, RPT-2026-0002 ...

CREATE TABLE hazard_reports (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reference_no          VARCHAR(20)  NOT NULL UNIQUE,
    reporter_id           UUID         NOT NULL REFERENCES users (id),
    hazard_type_id        UUID         NOT NULL REFERENCES hazard_types (id),
    category              VARCHAR(40)  NOT NULL,
    description           VARCHAR(500) NOT NULL CHECK (char_length(description) BETWEEN 10 AND 500),
    latitude              DOUBLE PRECISION,
    longitude             DOUBLE PRECISION,
    is_manual_location    BOOLEAN      NOT NULL DEFAULT FALSE,
    manual_location_text  VARCHAR(200),
    district_id           UUID         NOT NULL REFERENCES districts (id),
    status                VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                          CHECK (status IN ('PENDING', 'NEEDS_MORE_INFO', 'VERIFIED', 'REJECTED')),
    client_ref            UUID         NOT NULL UNIQUE,          -- offline sync idempotency key
    captured_at           TIMESTAMPTZ  NOT NULL,                 -- device time
    synced_at             TIMESTAMPTZ  NOT NULL,                 -- server receipt time
    reviewed_by           UUID         REFERENCES users (id),
    reviewed_at           TIMESTAMPTZ,
    rejection_reason      VARCHAR(30)
                          CHECK (rejection_reason IN ('INSUFFICIENT_EVIDENCE', 'DUPLICATE', 'LOCATION_MISMATCH',
                                                      'NOT_A_HAZARD', 'OTHER')),
    review_comment        VARCHAR(300),
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_report_location  CHECK ((latitude IS NOT NULL AND longitude IS NOT NULL)
                                           OR (is_manual_location AND manual_location_text IS NOT NULL)),
    CONSTRAINT chk_report_rejection CHECK (status <> 'REJECTED' OR rejection_reason IS NOT NULL),
    CONSTRAINT chk_report_review    CHECK (status = 'PENDING' OR reviewed_by IS NOT NULL)
);
CREATE INDEX idx_reports_status_captured      ON hazard_reports (status, captured_at);
CREATE INDEX idx_reports_type_district_status ON hazard_reports (hazard_type_id, district_id, status);
CREATE INDEX idx_reports_reporter             ON hazard_reports (reporter_id, captured_at DESC);

CREATE TABLE report_photos (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    report_id   UUID         NOT NULL REFERENCES hazard_reports (id) ON DELETE CASCADE,
    file_path   VARCHAR(255) NOT NULL,
    mime_type   VARCHAR(20)  NOT NULL CHECK (mime_type IN ('image/jpeg', 'image/png')),
    size_bytes  INTEGER      NOT NULL CHECK (size_bytes > 0 AND size_bytes <= 5242880),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_report_photos_report ON report_photos (report_id);
```

### V3\_0\_1\_\_warnings.sql (Inothma)

```sql
-- Owner: warnings module (UC01). Sensors are simulated IoT gauges (assignment FAQ: mock with dummy data).

CREATE TABLE sensors (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code               VARCHAR(30)      NOT NULL UNIQUE,
    name               VARCHAR(80)      NOT NULL,
    kind               VARCHAR(15)      NOT NULL CHECK (kind IN ('RIVER_GAUGE', 'RAIN_GAUGE')),
    river_basin_id     UUID             NOT NULL REFERENCES river_basins (id),
    district_id        UUID             NOT NULL REFERENCES districts (id),
    latitude           DOUBLE PRECISION NOT NULL,
    longitude          DOUBLE PRECISION NOT NULL,
    alert_level        NUMERIC(6, 2)    NOT NULL,
    major_flood_level  NUMERIC(6, 2)    NOT NULL,
    unit               VARCHAR(10)      NOT NULL,
    created_at         TIMESTAMPTZ      NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ      NOT NULL DEFAULT now(),
    CONSTRAINT chk_sensor_levels CHECK (major_flood_level > alert_level)
);

CREATE TABLE sensor_readings (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sensor_id    UUID          NOT NULL REFERENCES sensors (id),
    value        NUMERIC(6, 2) NOT NULL,
    recorded_at  TIMESTAMPTZ   NOT NULL
);
CREATE INDEX idx_sensor_readings_sensor_time ON sensor_readings (sensor_id, recorded_at);

CREATE TABLE hazards (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id        UUID         REFERENCES disaster_events (id),
    hazard_type_id  UUID         NOT NULL REFERENCES hazard_types (id),
    severity        SMALLINT     NOT NULL CHECK (severity BETWEEN 1 AND 5),
    district_id     UUID         REFERENCES districts (id),
    river_basin_id  UUID         REFERENCES river_basins (id),
    description     VARCHAR(500) NOT NULL,
    source          VARCHAR(10)  NOT NULL CHECK (source IN ('MANUAL', 'SENSOR', 'REPORT')),
    sensor_id       UUID         REFERENCES sensors (id),
    status          VARCHAR(20)  NOT NULL DEFAULT 'UNDER_ASSESSMENT'
                    CHECK (status IN ('UNDER_ASSESSMENT', 'WARNED', 'MONITORING', 'RESOLVED')),
    detected_at     TIMESTAMPTZ  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_hazard_area   CHECK (district_id IS NOT NULL OR river_basin_id IS NOT NULL),
    CONSTRAINT chk_hazard_sensor CHECK (source <> 'SENSOR' OR sensor_id IS NOT NULL)
);
CREATE INDEX idx_hazards_status_type_district ON hazards (status, hazard_type_id, district_id);

-- Verified reports linked to hazards. Written by the ReportVerifiedEvent listener,
-- so the warnings module never writes the reports module's table.
CREATE TABLE hazard_evidence (
    hazard_id  UUID        NOT NULL REFERENCES hazards (id),
    report_id  UUID        NOT NULL REFERENCES hazard_reports (id),
    linked_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (hazard_id, report_id)
);

CREATE TABLE warnings (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hazard_id      UUID          NOT NULL REFERENCES hazards (id),
    event_id       UUID          REFERENCES disaster_events (id),
    level          VARCHAR(10)   NOT NULL CHECK (level IN ('ADVISORY', 'WATCH', 'WARNING', 'EVACUATE')),
    status         VARCHAR(10)   NOT NULL DEFAULT 'ACTIVE'
                   CHECK (status IN ('ACTIVE', 'ESCALATED', 'CANCELLED', 'EXPIRED')),
    target_type    VARCHAR(12)   NOT NULL CHECK (target_type IN ('DISTRICT', 'RIVER_BASIN')),
    title          VARCHAR(80)   NOT NULL,
    message        VARCHAR(1000) NOT NULL,
    sms_text       VARCHAR(160)  NOT NULL,
    instructions   VARCHAR(300)  NOT NULL,
    issued_by      UUID          NOT NULL REFERENCES users (id),
    issued_at      TIMESTAMPTZ   NOT NULL,
    supersedes_id  UUID          REFERENCES warnings (id),    -- escalation chain
    cancelled_at   TIMESTAMPTZ,
    cancel_reason  VARCHAR(300),
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT chk_warning_cancel CHECK (status <> 'CANCELLED' OR cancel_reason IS NOT NULL)
);
CREATE INDEX idx_warnings_status ON warnings (status);
CREATE INDEX idx_warnings_hazard ON warnings (hazard_id);
CREATE INDEX idx_warnings_event  ON warnings (event_id, issued_at);

CREATE TABLE warning_target_areas (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    warning_id      UUID NOT NULL REFERENCES warnings (id) ON DELETE CASCADE,
    district_id     UUID REFERENCES districts (id),
    river_basin_id  UUID REFERENCES river_basins (id),
    CONSTRAINT chk_target_one_area CHECK (num_nonnulls(district_id, river_basin_id) = 1)
);
CREATE INDEX idx_target_areas_warning ON warning_target_areas (warning_id);

CREATE TABLE warning_reports (
    warning_id  UUID NOT NULL REFERENCES warnings (id),
    report_id   UUID NOT NULL REFERENCES hazard_reports (id),
    PRIMARY KEY (warning_id, report_id)
);

CREATE TABLE notification_deliveries (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    warning_id      UUID         NOT NULL REFERENCES warnings (id),
    citizen_id      UUID         NOT NULL REFERENCES users (id),
    channel         VARCHAR(10)  NOT NULL CHECK (channel IN ('PUSH', 'SMS', 'AUDIBLE')),
    status          VARCHAR(10)  NOT NULL CHECK (status IN ('QUEUED', 'DELIVERED', 'FAILED')),
    attempted_at    TIMESTAMPTZ  NOT NULL,
    delivered_at    TIMESTAMPTZ,
    failure_reason  VARCHAR(200),
    CONSTRAINT uq_delivery UNIQUE (warning_id, citizen_id, channel),
    CONSTRAINT chk_delivery_failure CHECK (status <> 'FAILED' OR failure_reason IS NOT NULL)
);
CREATE INDEX idx_deliveries_warning_status ON notification_deliveries (warning_id, status);

CREATE TABLE channel_settings (
    channel           VARCHAR(10) PRIMARY KEY CHECK (channel IN ('PUSH', 'SMS', 'AUDIBLE')),
    enabled           BOOLEAN     NOT NULL DEFAULT TRUE,
    simulate_failure  BOOLEAN     NOT NULL DEFAULT FALSE,
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
INSERT INTO channel_settings (channel) VALUES ('PUSH'), ('SMS'), ('AUDIBLE');
```

### V4\_0\_1\_\_response.sql (Ranepura)

```sql
-- Owner: response module (UC03).

CREATE TABLE rescue_teams (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name             VARCHAR(100) NOT NULL,
    organisation_id  UUID         NOT NULL REFERENCES organisations (id),
    district_id      UUID         NOT NULL REFERENCES districts (id),
    team_type        VARCHAR(10)  NOT NULL CHECK (team_type IN ('BOAT', 'MEDICAL', 'SEARCH')),
    capacity         INTEGER      NOT NULL CHECK (capacity > 0),
    status           VARCHAR(20)  NOT NULL DEFAULT 'AVAILABLE'
                     CHECK (status IN ('AVAILABLE', 'DISPATCHED', 'EN_ROUTE', 'ACTIVE', 'OFFLINE_UNKNOWN')),
    last_status_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version          BIGINT       NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_rescue_teams_district_status ON rescue_teams (district_id, status);

ALTER TABLE users
    ADD CONSTRAINT fk_users_rescue_team FOREIGN KEY (rescue_team_id) REFERENCES rescue_teams (id);

CREATE TABLE shelters (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name               VARCHAR(120)     NOT NULL,
    district_id        UUID             NOT NULL REFERENCES districts (id),
    address            VARCHAR(200)     NOT NULL,
    latitude           DOUBLE PRECISION NOT NULL,
    longitude          DOUBLE PRECISION NOT NULL,
    capacity           INTEGER          NOT NULL CHECK (capacity > 0),
    current_occupancy  INTEGER          NOT NULL DEFAULT 0,
    status             VARCHAR(10)      NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'FULL', 'CLOSED')),
    coordinator_id     UUID             REFERENCES users (id),
    version            BIGINT           NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ      NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ      NOT NULL DEFAULT now(),
    CONSTRAINT chk_shelter_occupancy CHECK (current_occupancy BETWEEN 0 AND capacity)
);
CREATE INDEX idx_shelters_district_status ON shelters (district_id, status);

CREATE TABLE rescue_assignments (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id                UUID             NOT NULL REFERENCES disaster_events (id),
    warning_id              UUID             REFERENCES warnings (id),
    team_id                 UUID             REFERENCES rescue_teams (id),
    created_by              UUID             NOT NULL REFERENCES users (id),
    latitude                DOUBLE PRECISION NOT NULL,
    longitude               DOUBLE PRECISION NOT NULL,
    location_text           VARCHAR(200)     NOT NULL,
    task                    VARCHAR(500)     NOT NULL,
    priority                SMALLINT         NOT NULL CHECK (priority BETWEEN 1 AND 3),
    people_estimated        INTEGER          NOT NULL DEFAULT 0 CHECK (people_estimated >= 0),
    destination_shelter_id  UUID             REFERENCES shelters (id),
    status                  VARCHAR(20)      NOT NULL DEFAULT 'UNASSIGNED'
                            CHECK (status IN ('UNASSIGNED', 'PENDING_ACK', 'ACCEPTED', 'EN_ROUTE', 'ACTIVE',
                                              'COMPLETED', 'CANCELLED')),
    decline_reason          VARCHAR(200),
    assigned_at             TIMESTAMPTZ,
    acknowledged_at         TIMESTAMPTZ,
    completed_at            TIMESTAMPTZ,
    version                 BIGINT           NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ      NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ      NOT NULL DEFAULT now(),
    CONSTRAINT chk_assignment_team CHECK (status IN ('UNASSIGNED', 'CANCELLED') OR team_id IS NOT NULL)
);
-- A team can hold only one live assignment at a time.
CREATE UNIQUE INDEX ux_one_live_assignment_per_team ON rescue_assignments (team_id)
    WHERE status IN ('PENDING_ACK', 'ACCEPTED', 'EN_ROUTE', 'ACTIVE');
CREATE INDEX idx_assignments_event_status ON rescue_assignments (event_id, status);

CREATE TABLE team_status_logs (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id           UUID        NOT NULL REFERENCES rescue_teams (id),
    assignment_id     UUID        REFERENCES rescue_assignments (id),
    from_status       VARCHAR(20) NOT NULL,
    to_status         VARCHAR(20) NOT NULL,
    changed_by        UUID        NOT NULL REFERENCES users (id),
    changed_at        TIMESTAMPTZ NOT NULL,            -- device time (may be offline)
    recorded_offline  BOOLEAN     NOT NULL DEFAULT FALSE,
    client_ref        UUID        UNIQUE,              -- offline sync idempotency key
    synced_at         TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_team_status_logs_team ON team_status_logs (team_id, changed_at);

CREATE TABLE occupancy_logs (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shelter_id   UUID        NOT NULL REFERENCES shelters (id),
    event_id     UUID        REFERENCES disaster_events (id),
    occupancy    INTEGER     NOT NULL CHECK (occupancy >= 0),
    delta        INTEGER     NOT NULL,
    recorded_by  UUID        NOT NULL REFERENCES users (id),
    recorded_at  TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_occupancy_logs_shelter_time ON occupancy_logs (shelter_id, recorded_at);

CREATE TABLE relief_stocks (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    item_id             UUID        NOT NULL REFERENCES relief_items (id),
    organisation_id     UUID        NOT NULL REFERENCES organisations (id),
    district_id         UUID        NOT NULL REFERENCES districts (id),
    quantity_available  INTEGER     NOT NULL CHECK (quantity_available >= 0),
    version             BIGINT      NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_stock_item_owner_district UNIQUE (item_id, organisation_id, district_id)
);

CREATE TABLE resource_allocations (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    stock_id      UUID        NOT NULL REFERENCES relief_stocks (id),
    shelter_id    UUID        NOT NULL REFERENCES shelters (id),
    event_id      UUID        NOT NULL REFERENCES disaster_events (id),
    quantity      INTEGER     NOT NULL CHECK (quantity > 0),
    status        VARCHAR(25) NOT NULL DEFAULT 'ALLOCATED'
                  CHECK (status IN ('ALLOCATED', 'PARTIALLY_DISTRIBUTED', 'DISTRIBUTED', 'CANCELLED')),
    allocated_by  UUID        NOT NULL REFERENCES users (id),
    allocated_at  TIMESTAMPTZ NOT NULL,
    version       BIGINT      NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_allocations_shelter ON resource_allocations (shelter_id);
CREATE INDEX idx_allocations_event   ON resource_allocations (event_id);

CREATE TABLE relief_distributions (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    allocation_id         UUID        NOT NULL REFERENCES resource_allocations (id),
    quantity_distributed  INTEGER     NOT NULL CHECK (quantity_distributed > 0),
    distributed_by        UUID        NOT NULL REFERENCES users (id),
    distributed_at        TIMESTAMPTZ NOT NULL,
    recorded_offline      BOOLEAN     NOT NULL DEFAULT FALSE,
    client_ref            UUID        UNIQUE,
    synced_at             TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_distributions_allocation ON relief_distributions (allocation_id);

-- Feeds "Recent Disaster Updates & Actions" on the district dashboard.
CREATE TABLE activity_logs (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    district_id  UUID         NOT NULL REFERENCES districts (id),
    event_id     UUID         REFERENCES disaster_events (id),
    type         VARCHAR(15)  NOT NULL CHECK (type IN ('WARNING', 'DISPATCH', 'TEAM_STATUS', 'SHELTER', 'RELIEF')),
    message      VARCHAR(200) NOT NULL,
    occurred_at  TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_activity_logs_district_time ON activity_logs (district_id, occurred_at DESC);
```

### V5\_0\_1\_\_analytics.sql (Herath)

```sql
-- Owner: analytics module (UC04). Reads every other table read-only; writes only this one.

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

### V6\_0\_1\_\_seed\_master\_data.sql

```sql
-- Owner: group. Master data the modules operate on during the active Kelani flood.
-- Gauge thresholds are demo values for the simulator, not official Irrigation Department levels.

INSERT INTO sensors (id, code, name, kind, river_basin_id, district_id, latitude, longitude,
                     alert_level, major_flood_level, unit) VALUES
 ('00000000-0000-0000-0008-000000000001', 'KEL-NAGALAGAM',  'Kelani at Nagalagam Street', 'RIVER_GAUGE',
  '00000000-0000-0000-0002-000000000001', '00000000-0000-0000-0001-000000000001', 6.9597, 79.8737,  1.20,  2.50, 'm'),
 ('00000000-0000-0000-0008-000000000002', 'KEL-HANWELLA',   'Kelani at Hanwella',         'RIVER_GAUGE',
  '00000000-0000-0000-0002-000000000001', '00000000-0000-0000-0001-000000000001', 6.9093, 80.0814,  7.00, 10.00, 'm'),
 ('00000000-0000-0000-0008-000000000003', 'KEL-GLENCOURSE', 'Kelani at Glencourse',       'RIVER_GAUGE',
  '00000000-0000-0000-0002-000000000001', '00000000-0000-0000-0001-000000000005', 6.9790, 80.1830, 16.00, 19.00, 'm'),
 ('00000000-0000-0000-0008-000000000004', 'KALU-RATNAPURA', 'Kalu at Ratnapura',          'RIVER_GAUGE',
  '00000000-0000-0000-0002-000000000002', '00000000-0000-0000-0001-000000000004', 6.6828, 80.3992,  5.20,  7.50, 'm'),
 ('00000000-0000-0000-0008-000000000005', 'KALU-ELLAGAWA',  'Kalu at Ellagawa',           'RIVER_GAUGE',
  '00000000-0000-0000-0002-000000000002', '00000000-0000-0000-0001-000000000004', 6.5486, 80.2208, 10.50, 13.00, 'm'),
 ('00000000-0000-0000-0008-000000000006', 'KALU-PUTUPAULA', 'Kalu at Putupaula',          'RIVER_GAUGE',
  '00000000-0000-0000-0002-000000000002', '00000000-0000-0000-0001-000000000003', 6.5994, 80.0697,  2.50,  4.00, 'm');

INSERT INTO shelters (id, name, district_id, address, latitude, longitude, capacity, current_occupancy,
                      status, coordinator_id) VALUES
 ('00000000-0000-0000-0009-000000000001', 'Kolonnawa Maha Vidyalaya',         '00000000-0000-0000-0001-000000000001',
  'Kolonnawa',    6.9330, 79.8880, 300, 140, 'OPEN',   NULL),
 ('00000000-0000-0000-0009-000000000002', 'Wellampitiya Community Hall',      '00000000-0000-0000-0001-000000000001',
  'Wellampitiya', 6.9390, 79.8920, 200, 185, 'OPEN',   '00000000-0000-0000-0006-000000000006'),  -- 92.5 %: amber
 ('00000000-0000-0000-0009-000000000003', 'Kaduwela Bodhirajaramaya',         '00000000-0000-0000-0001-000000000001',
  'Kaduwela',     6.9330, 79.9840, 120, 120, 'FULL',   NULL),
 ('00000000-0000-0000-0009-000000000004', 'Biyagama Central College',         '00000000-0000-0000-0001-000000000002',
  'Biyagama',     6.9415, 79.9877, 250,  40, 'OPEN',   NULL),
 ('00000000-0000-0000-0009-000000000005', 'Kelaniya Community Centre',        '00000000-0000-0000-0001-000000000002',
  'Kelaniya',     6.9553, 79.9220, 150,   0, 'CLOSED', NULL),
 ('00000000-0000-0000-0009-000000000006', 'Ratnapura Sivali Central College', '00000000-0000-0000-0001-000000000004',
  'Ratnapura',    6.6830, 80.4000, 300,   0, 'OPEN',   NULL);

-- Opening occupancy entries so "occupancy over time" starts from real rows.
INSERT INTO occupancy_logs (shelter_id, event_id, occupancy, delta, recorded_by, recorded_at) VALUES
 ('00000000-0000-0000-0009-000000000001', '00000000-0000-0000-0007-000000000001', 140, 140,
  '00000000-0000-0000-0006-000000000002', '2026-10-02 09:00:00+05:30'),
 ('00000000-0000-0000-0009-000000000002', '00000000-0000-0000-0007-000000000001', 185, 185,
  '00000000-0000-0000-0006-000000000006', '2026-10-02 09:30:00+05:30'),
 ('00000000-0000-0000-0009-000000000003', '00000000-0000-0000-0007-000000000001', 120, 120,
  '00000000-0000-0000-0006-000000000002', '2026-10-02 10:00:00+05:30'),
 ('00000000-0000-0000-0009-000000000004', '00000000-0000-0000-0007-000000000001',  40,  40,
  '00000000-0000-0000-0006-000000000002', '2026-10-02 10:30:00+05:30');

INSERT INTO rescue_teams (id, name, organisation_id, district_id, team_type, capacity) VALUES
 ('00000000-0000-0000-0010-000000000001', 'Army Rapid Response Unit - Colombo', '00000000-0000-0000-0004-000000000003',
  '00000000-0000-0000-0001-000000000001', 'SEARCH',  12),
 ('00000000-0000-0000-0010-000000000002', 'Navy Boat Team - Kelani A',          '00000000-0000-0000-0004-000000000004',
  '00000000-0000-0000-0001-000000000001', 'BOAT',     8),
 ('00000000-0000-0000-0010-000000000003', 'SLAF Rescue Wing - Katunayake',      '00000000-0000-0000-0004-000000000005',
  '00000000-0000-0000-0001-000000000002', 'SEARCH',  10),
 ('00000000-0000-0000-0010-000000000004', 'Police Life Saving Unit - Gampaha',  '00000000-0000-0000-0004-000000000006',
  '00000000-0000-0000-0001-000000000002', 'BOAT',     6),
 ('00000000-0000-0000-0010-000000000005', 'Red Cross Medical Team - Colombo',   '00000000-0000-0000-0004-000000000007',
  '00000000-0000-0000-0001-000000000001', 'MEDICAL',  6),
 ('00000000-0000-0000-0010-000000000006', 'DMC Volunteer Response - Ratnapura', '00000000-0000-0000-0004-000000000001',
  '00000000-0000-0000-0001-000000000004', 'SEARCH',  15);

-- Link the two rescue-member users to their teams.
UPDATE users SET rescue_team_id = '00000000-0000-0000-0010-000000000001'
 WHERE id = '00000000-0000-0000-0006-000000000007';
UPDATE users SET rescue_team_id = '00000000-0000-0000-0010-000000000002'
 WHERE id = '00000000-0000-0000-0006-000000000008';

-- Relief stock: item x owner organisation x district (11 rows, 5 organisation types).
INSERT INTO relief_stocks (id, item_id, organisation_id, district_id, quantity_available) VALUES
 ('00000000-0000-0000-0011-000000000001', '00000000-0000-0000-0005-000000000001', '00000000-0000-0000-0004-000000000001', '00000000-0000-0000-0001-000000000001', 500),  -- dry ration, DMC, Colombo
 ('00000000-0000-0000-0011-000000000002', '00000000-0000-0000-0005-000000000001', '00000000-0000-0000-0004-000000000007', '00000000-0000-0000-0001-000000000001', 300),  -- dry ration, Red Cross, Colombo
 ('00000000-0000-0000-0011-000000000003', '00000000-0000-0000-0005-000000000001', '00000000-0000-0000-0004-000000000009', '00000000-0000-0000-0001-000000000002', 200),  -- dry ration, private donor, Gampaha
 ('00000000-0000-0000-0011-000000000004', '00000000-0000-0000-0005-000000000002', '00000000-0000-0000-0004-000000000003', '00000000-0000-0000-0001-000000000001', 800),  -- water, Army, Colombo
 ('00000000-0000-0000-0011-000000000005', '00000000-0000-0000-0005-000000000002', '00000000-0000-0000-0004-000000000008', '00000000-0000-0000-0001-000000000002', 400),  -- water, Sarvodaya, Gampaha
 ('00000000-0000-0000-0011-000000000006', '00000000-0000-0000-0005-000000000003', '00000000-0000-0000-0004-000000000002', '00000000-0000-0000-0001-000000000001', 120),  -- first aid, MoH, Colombo
 ('00000000-0000-0000-0011-000000000007', '00000000-0000-0000-0005-000000000003', '00000000-0000-0000-0004-000000000007', '00000000-0000-0000-0001-000000000002',  60),  -- first aid, Red Cross, Gampaha
 ('00000000-0000-0000-0011-000000000008', '00000000-0000-0000-0005-000000000004', '00000000-0000-0000-0004-000000000008', '00000000-0000-0000-0001-000000000001', 150),  -- hygiene, Sarvodaya, Colombo
 ('00000000-0000-0000-0011-000000000009', '00000000-0000-0000-0005-000000000004', '00000000-0000-0000-0004-000000000004', '00000000-0000-0000-0001-000000000001', 100),  -- hygiene, Navy, Colombo
 ('00000000-0000-0000-0011-000000000010', '00000000-0000-0000-0005-000000000001', '00000000-0000-0000-0004-000000000001', '00000000-0000-0000-0001-000000000004', 400),  -- dry ration, DMC, Ratnapura
 ('00000000-0000-0000-0011-000000000011', '00000000-0000-0000-0005-000000000002', '00000000-0000-0000-0004-000000000005', '00000000-0000-0000-0001-000000000004', 300);  -- water, Air Force, Ratnapura
```

## 9. Frontend scaffold, verification and first commit

**Frontend** (React + TypeScript + Vite; the frontend owner sets up Tailwind and tests afterwards):

```bash
npm create vite@latest frontend -- --template react-ts
cd frontend
npm install
npm install react-router-dom @tanstack/react-query recharts idb
mkdir -p src/shared/{api,offline,components,i18n,theme} src/features/{reports,warnings,response,analytics}
echo "VITE_API_BASE_URL=http://localhost:8080/api" > .env.development
cd ..
```

**Run**

```bash
docker compose up -d
cd backend
./mvnw spotless:apply
./mvnw verify
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

**Verification checklist**

- [ ] `./mvnw verify` ends with BUILD SUCCESS (Spotless, tests, ModularityTests, JaCoCo report).
- [ ] Startup log shows Flyway applying 7 migrations (V1\_0\_1 to V6\_0\_1) with no errors, and Hibernate validation passes.
- [ ] In pgAdmin (server host `postgres`, user and password `disaster`) the `disaster` database has 30 tables plus `flyway_schema_history` and `event_publication`.
- [ ] `SELECT count(*) FROM users;` returns 49 (9 named users + 40 demo residents); `districts` returns 5; `shelters` 6; `rescue_teams` 6; `relief_stocks` 11.
- [ ] http://localhost:8080/actuator/health returns `{"status":"UP"}`.
- [ ] http://localhost:8080/swagger-ui.html opens (no endpoints yet; they appear as modules are built).
- [ ] `target/spring-modulith-docs/` contains the module diagrams.
- [ ] `cd frontend && npm run dev` serves the Vite page at http://localhost:5173.

**First commit**

```bash
git checkout -b chore/initialise-repository
git add .
git commit -m "chore: initialise Spring Boot backend, migrations, CI and frontend scaffold"
git push -u origin chore/initialise-repository
```

Open a pull request, let CI pass, merge into `main`, then turn on branch protection (require pull request review and the `backend` check). Each member then creates `feature/<module>-<topic>` from `main`.

**Hand-off to members:** everyone pulls `main`, runs `docker compose up -d` and `./mvnw verify`, and starts their module from the Backend Initialisation Prompt's member follow-up prompt or their `docs/modules/<module>.md` guide.
