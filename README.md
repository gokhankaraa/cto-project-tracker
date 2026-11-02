# Weekly Project Status Reporting and CTO Tracking System — Backend

Backend API where project managers enter weekly status reports for the projects they own, and the
CTO monitors the entire project portfolio from a single place.

Track: **Backend (Java + Spring Boot)** · Author: Gökhan Kara

> **Status:** MVP (T01–T12) complete. T13 (days 21–24) adds filtering on the CTO dashboard and two
> cross-project list endpoints for reports and work items, with paging, sorting and a shared delay
> rule. Next: PostgreSQL migration, then role-based authorization (T14).

Project documents (in [`docs/`](docs/)):
- [`on_analiz.md`](docs/on_analiz.md) — pre-analysis (problem, roles, scope, user stories, acceptance criteria)
- [`teknik-karar-notu.docx`](docs/teknik-karar-notu.docx) — technical decision note and learning plan
- [`test-raporu-MVP-T08.docx`](docs/test-raporu-MVP-T08.docx) — MVP test report (incl. T09 regression)
- [`analiz-notu-T13.md`](docs/analiz-notu-T13.md) — T13 filtering analysis (decisions, acceptance criteria)
- [`filtre-sozlesmesi-T13.md`](docs/filtre-sozlesmesi-T13.md) — T13 filter API contract (parameters, examples, errors)
- [`Test Senaryolari - CTO Takip Sistemi.xlsx`](docs/) — detailed test-case log
- Weekly internship reports and per-task reports

---

## Tech stack

| Layer | Choice | Rationale |
|---|---|---|
| Language | Java 21 (LTS) | LTS release; compatibility with the libraries used is verified |
| Framework | Spring Boot 4.1 | Start a new project on a current version; natural support for layered architecture |
| Data access | Spring Data JPA (Hibernate) | Entity–table mapping, repository abstraction |
| Database | H2 (embedded, in-memory) | Zero setup; portable to PostgreSQL thanks to the repository abstraction |
| Validation | Spring Validation (Bean Validation) | Field-level required/format/range checks |
| API docs | springdoc-openapi 3.0.3 (Swagger UI) | Test endpoints from the browser and show the API in the demo |
| Helper | Lombok | Reduce boilerplate getter/setter/constructor code |

> springdoc's 2.x line targets Spring Boot 3; with Boot 4 the **3.x** line must be used.

## Requirements

- JDK 21 or newer (`java -version`)
- No need to install Maven — the bundled Maven Wrapper (`./mvnw`) is used
- No separate database setup needed; H2 runs in memory alongside the application

## Setup and run

```bash
git clone <repository-url>
cd cto-project-tracker-backend

./mvnw spring-boot:run          # starts the application (http://localhost:8080)
```

Alternatively, build and run a jar:

```bash
./mvnw clean package
java -jar target/cto-project-tracker-backend-0.0.1-SNAPSHOT.jar
```

### Run with Docker

The app ships with a multi-stage `Dockerfile` (builds the jar inside the image, then runs it on a
JRE), so it runs anywhere Docker is installed — no local Java/Maven needed:

```bash
docker build -t cto-tracker:1.0 .
docker run -d --name cto-demo -p 8080:8080 cto-tracker:1.0

curl http://localhost:8080/api/health   # {"status":"UP","databaseStatus":"UP", ...}
```

Stop and remove the container:

```bash
docker rm -f cto-demo
```

The in-memory H2 database is seeded on startup, so the dashboard and endpoints are usable
immediately inside the container (no external database required).

### Verifying it runs

| What | Address | Expected |
|---|---|---|
| Health endpoint | http://localhost:8080/api/health | `status: UP`, `databaseStatus: UP` |
| Swagger UI | http://localhost:8080/swagger-ui.html | API documentation opens |
| OpenAPI JSON | http://localhost:8080/v3/api-docs | Returns the OpenAPI schema |
| H2 console | http://localhost:8080/h2-console | JDBC URL: `jdbc:h2:mem:ctotracker`, user: `sa`, empty password |

```bash
curl http://localhost:8080/api/health
```

```json
{
  "status": "UP",
  "application": "cto-project-tracker-backend",
  "databaseStatus": "UP",
  "database": "H2 2.4.240 (2025-09-22)",
  "timestamp": "2026-07-22T15:49:49.528096+03:00"
}
```

The `databaseStatus` field is produced by actually issuing a query against the database; it confirms
not just that the application is up, but that the DB connection works.

## Configuration

All settings live in [`src/main/resources/application.yml`](src/main/resources/application.yml).
The development environment contains no secret values (H2 user `sa`, empty password). When moving to
PostgreSQL, connection details will be supplied via environment variables and never committed.

The MVP runs in a single environment (local H2), so no separate Spring profile was needed. For
production (e.g. PostgreSQL), a `prod` profile with env-supplied connection details can be added
without touching the code.

| Setting | Default | Description |
|---|---|---|
| `server.port` | `8080` | Application port |
| `spring.datasource.url` | `jdbc:h2:mem:ctotracker` | In-memory H2; data is dropped when the app stops |
| `spring.jpa.hibernate.ddl-auto` | `create-drop` | Schema is generated from entities on each startup |
| `spring.h2.console.enabled` | `true` | H2 web console (development only) |

## API endpoints

| Method | Path | Description |
|---|---|---|
| GET | `/api/health` | Application and DB health check |
| GET / POST | `/api/projects` | List / create projects |
| GET / PUT / DELETE | `/api/projects/{id}` | Project detail / update / delete |
| GET / POST | `/api/projects/{projectId}/reports` | List / create weekly reports for a project |
| GET / PUT / DELETE | `/api/reports/{id}` | Weekly report detail / update / delete |
| GET | `/api/reports` | Cross-project report list: filtered, paged, sorted |
| GET / POST | `/api/reports/{reportId}/work-items` | List / create work items for a report |
| GET / PUT / DELETE | `/api/work-items/{id}` | Work item detail / update / delete |
| GET | `/api/work-items` | Cross-project work item list: filtered, paged, sorted |
| GET | `/api/dashboard/summary` | CTO portfolio summary, with optional filters |

The full, interactive contract is in Swagger UI. Key business rules (see pre-analysis §H-03/H-04):

- A project can have only **one report per week** — a duplicate `(project, week)` returns **409**.
- Progress is tracked as ordered stages `ANALIZ → GELISTIRME → TEST → TAMAMLANDI`; the percentage
  (`25 / 50 / 75 / 100`) is **derived from the stage, never entered by hand**.
- On update a report's stage may stay the same or advance exactly one step; going backward or
  skipping a stage returns **400**.
- Work items belong to a report; deleting a report also deletes its work items (cascade). The
  report's **live task count** is derived from the number of `DEVAM_EDIYOR` work items — not stored
  separately.
- A project that still has weekly reports cannot be deleted (**409**); delete its reports first.
- The dashboard summary reports each project's **latest** report (highest week number). Projects with
  no report are still listed, with empty/default progress. Portfolio counters (total, high-risk,
  without-report, total live tasks) accompany the per-project rows.

> **Scope note (from the pre-analysis):** progress uses a single staged model, so there is no
> separate "target %"; the schedule (`scheduleStatus`) field is out of MVP scope. Both are
> documented decisions in the analysis document.

## Filtering, paging and sorting

The full contract, with examples against the demo data, is in
[`filtre-sozlesmesi-T13.md`](docs/filtre-sozlesmesi-T13.md). Every parameter is also described in
Swagger UI with an example value.

| Endpoint | Filters | Paging / sorting |
|---|---|---|
| `GET /api/dashboard/summary` | `projectId`, `projectStatus`, `ownerId`, `weekNumber`, `overallStatus`, `riskLevel`, `delayed` | None; rows ordered by project id |
| `GET /api/reports` | `projectId`, `ownerId`, `weekFrom`, `weekTo`, `dateFrom`, `dateTo`, `overallStatus`, `riskLevel`, `progressStage` | Default `reportDate,desc`; sortable by `reportDate`, `weekNumber`, `projectName` |
| `GET /api/work-items` | `projectId`, `weekNumber`, `assignee`, `status`, `delayed` | Default `plannedDate,asc` (items without a date last); sortable by `plannedDate`, `title`, `projectName` |

- Different parameters combine with **AND**. Status, risk and stage parameters accept several
  comma-separated values, which combine with **OR** (`riskLevel=ORTA,YUKSEK`).
- `weekNumber` is a reference point: each project is evaluated on its latest report up to that
  week. On the dashboard, projects with no report by then are still listed with `hasReport: false`,
  unless a report-based filter (`overallStatus`, `riskLevel`, `delayed`) is also given.
  The work item list shows only the items of that evaluated report, so the same task is not
  repeated across weeks.
- Paging: `page` starts at `0`, `size` is `1–100` (default `20`). The response is
  `{ content, page, size, totalElements, totalPages }`. Sorting is `field` or `field,asc|desc`,
  and `id` is always added as a tie-breaker so pages don't shift.
- `assignee` is a case-insensitive "contains" search (`assignee=ayse` matches "Ayse Yilmaz").
- **Delay rule:** a work item is delayed if its status is `GECIKTI`, or if its planned date is
  before its report's date and it is not `TAMAMLANDI`. A project is delayed if its evaluated report
  is `GECIKMELI` or contains a delayed work item. The report date is the reference, not today, so
  past weeks keep their own result.
- An empty result is `200` with an empty list (and zero counters), never `404`. Invalid values
  (unknown enum, wrong type, out-of-range number, reversed range, unknown sort field) return `400`
  in the shared error format.
- **Unknown parameter names are ignored** (Spring's default): a typo such as `?risk=YUKSEK`
  returns the unfiltered result instead of an error.

## Demo data

On startup, `DataSeeder` loads sample data into the in-memory H2 (only when the DB is empty), so
Swagger and the endpoints are usable immediately without manual setup:

**Sample users** (no password — authentication is out of MVP scope):

| id | email | role |
|---|---|---|
| 1 | `ayse@example.com` | PROJECT_MANAGER |
| 2 | `mehmet@example.com` | PROJECT_MANAGER |
| 3 | `cto@example.com` | CTO |
| 4 | `admin@example.com` | ADMIN |

- **Projects:** Atlas IK (owner: Ayşe), e-Belge (owner: Mehmet), Nova POS (owner: Ayşe, no report yet)
- **Weekly reports:** Atlas IK weeks 29–31, e-Belge week 30, with a few work items. Atlas IK week 31 has
  an overdue work item, so the project shows `delayed: true` even though its status is `YOLUNDA`.

All sample data is fictional (project, customer and person names are made up; emails use the reserved
`example.com` domain). The repository is public, so no real company or customer data is used.

## Demo scenario

A typical end-to-end walkthrough (run against the seeded data via Swagger UI or `curl`). `{id}`
values come from the responses of the previous steps.

1. **CTO opens the dashboard** — see every project's latest status in one view:
   `GET /api/dashboard/summary` → 200, portfolio counters + per-project rows
2. **Filter the portfolio:** `GET /api/dashboard/summary?riskLevel=YUKSEK` → only e-Belge;
   `?delayed=true` → only Atlas IK, and `GET /api/work-items?delayed=true` shows the overdue item
3. **Drill into a project:** `GET /api/projects/3` and `GET /api/projects/3/reports`
   (Nova POS has no report yet)
4. **PM files a weekly report** for Nova POS (id 3):
   `POST /api/projects/3/reports` `{ "weekNumber": 33, "reportDate": "2026-08-13",
   "progressStage": "ANALIZ", "overallStatus": "YOLUNDA", "riskLevel": "DUSUK" }` → 201, `progressPercentage: 25`
5. **Add a work item** to that report: `POST /api/reports/{reportId}/work-items`
   `{ "title": "Kickoff", "status": "DEVAM_EDIYOR", "assignee": "Ayşe" }` → 201
6. **Re-read the report** — the live task count reflects the new item:
   `GET /api/reports/{reportId}` → `liveTaskCount: 1`
7. **Back to the dashboard** — Nova POS now shows `hasReport: true` with the fresh status.

Negative checks worth showing: a duplicate `(project, week)` report → **409**, skipping a progress
stage on update → **400**, deleting a project that still has reports → **409**.

## Package and layer structure

```
com.kolaysoft.ctotracker
├── config/           Application configuration (OpenAPI/Swagger)
├── controller/       HTTP layer: request handling, DTO exchange
├── service/          Business rules (stage transitions, uniqueness, delay rule)
├── repository/       Database access via Spring Data JPA
│   └── spec/         Filter conditions as JPA Specifications
├── entity/           JPA data model and enums
├── dto/              Request/response contracts (entities are not exposed directly)
│   └── filter/       Filter records passed from controllers to services
└── common/
    ├── error/        ApiError, ErrorCode, GlobalExceptionHandler
    ├── exception/    ResourceNotFoundException, DuplicateResourceException, BusinessRuleException
    └── web/          SortParameter (sort allow-list and validation)
```

Layering rule: `controller → service → repository`. Controllers contain no business rules, services
don't know about HTTP, and entities are not exposed directly to the API (DTOs are used).

## Error format

Whatever the error, the API returns a single body shape (see pre-analysis, section 11):

```json
{
  "timestamp": "2026-07-22T15:49:49.598473+03:00",
  "status": 404,
  "code": "RESOURCE_NOT_FOUND",
  "message": "Proje bulunamadi: 42",
  "path": "/api/projects/42"
}
```

Validation errors additionally include a `fieldErrors` array:

```json
{
  "timestamp": "2026-07-22T15:49:49.598473+03:00",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Gonderilen alanlar dogrulama kurallarina uymuyor.",
  "path": "/api/reports",
  "fieldErrors": [
    { "field": "weekNumber", "message": "Hafta numarasi en fazla 53 olmalidir." }
  ]
}
```

| Code | HTTP | When |
|---|---|---|
| `VALIDATION_ERROR` | 400 | Missing required field, format/range error |
| `INVALID_REQUEST` | 400 | Malformed JSON, unknown enum value, bad date |
| `BUSINESS_RULE_VIOLATION` | 400 | Business-rule violation (e.g. skipping a progress stage) |
| `RESOURCE_NOT_FOUND` | 404 | Record or endpoint not found |
| `METHOD_NOT_ALLOWED` | 405 | Endpoint does not support this HTTP method |
| `DUPLICATE_RESOURCE` | 409 | Uniqueness violation (project+week, email) |
| `RESOURCE_IN_USE` | 409 | Resource can't be deleted because other records depend on it (e.g. a project that still has reports) |
| `INTERNAL_ERROR` | 500 | Unexpected error; technical detail is logged, not leaked to the client |

## Tests

```bash
./mvnw test
```

Current tests (100 in total):

- `MvpIntegrationTest` — end-to-end HTTP flows (@SpringBootTest + MockMvc): happy path
  (project → report → work item → dashboard), 404 / 400 / 409, stage skip, dashboard filter
- `FilterIntegrationTest` — T13 filters over HTTP (MockMvc): dashboard filters and counters, week
  reference, report paging and sorting, work item filters, null-last default sort, and the error
  cases of the filter contract
- `WeeklyReportQueryTest` / `WorkItemQueryTest` — filter queries against the database
  (@DataJpaTest): inclusive ranges, AND/OR combination, evaluated-report selection, case-insensitive
  assignee search, and the delay condition matching the in-memory delay rule
- `SortParameterTest` — sort parsing: default, direction, field mapping, rejected values
- `HealthControllerTest` — health endpoint and DB connection
- `GlobalExceptionHandlerTest` — the error-format contract (400 / 404 / 405 / 409 / 500), including
  invalid enum/type request parameters
- `WeeklyReportServiceTest` — business rules: uniqueness (409), stage skip/backward (400),
  percentage derivation, not-found (404)
- `ProjectServiceTest` — project create/update/mapping, owner-not-found (404), delete-with-reports (409)
- `WorkItemServiceTest` — work item CRUD, report-not-found (404), delete
- `DashboardServiceTest` — portfolio aggregation, latest-report selection, risk and multi-value
  filters, week reference, delay filter and counter, empty portfolio
- `CtoProjectTrackerBackendApplicationTests` — does the Spring context load

## Known gaps

MVP scope decisions, with rationale, are in sections 13–14 of the pre-analysis document.
Not yet implemented at this stage:

- Authentication (login) and role-based authorization — out of MVP scope; `User.password` is
  omitted for now and will arrive with authentication
- Report locking, audit log, export — out of MVP scope
