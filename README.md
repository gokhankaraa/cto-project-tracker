# Weekly Project Status Reporting and CTO Tracking System

A web application where project managers enter weekly status reports for the projects they own,
and the CTO monitors the entire project portfolio from a single place: a Spring Boot REST API with
PostgreSQL, and a React frontend.

Track: **Full Stack (Java + Spring Boot, React + TypeScript)** · Author: Gökhan Kara

> **Status:** MVP (T01–T12) complete. T13 (days 21–24) adds filtering on the CTO dashboard and two
> cross-project list endpoints for reports and work items, with paging, sorting and a shared delay
> rule. Data now lives in PostgreSQL, with the schema managed by Flyway and tests running against a
> real PostgreSQL via Testcontainers. Reports carry the year of the week they describe (Flyway V2,
> corrected at year boundaries in V3), and the filters accept a `year`. The repository now also holds the frontend: phase 0 (skeleton,
> API client, CORS) is in place, and phase 1 (the MVP screens) is complete: CTO dashboard,
> projects, weekly reports, work items and the cross-project report and work item lists.
> Role-based authorization (T14) comes next.

Project documents (in [`docs/`](docs/)):
- [`on_analiz.md`](docs/on_analiz.md) — pre-analysis (problem, roles, scope, user stories, acceptance criteria)
- [`teknik-karar-notu.docx`](docs/teknik-karar-notu.docx) — technical decision note and learning plan
- [`test-raporu-MVP-T08.docx`](docs/test-raporu-MVP-T08.docx) — MVP test report (incl. T09 regression)
- [`analiz-notu-T13.md`](docs/analiz-notu-T13.md) — T13 filtering analysis (decisions, acceptance criteria)
- [`filtre-sozlesmesi-T13.md`](docs/filtre-sozlesmesi-T13.md) — T13 filter API contract (parameters, examples, errors)
- [`Test Senaryolari - CTO Takip Sistemi.xlsx`](docs/) — detailed test-case log
- Weekly internship reports and per-task reports

---

## Repository layout

```
├── backend/             Spring Boot REST API (Maven project, Dockerfile)
├── frontend/            React + TypeScript web app (Vite)
├── docs/                Analysis notes, API contracts, reports
├── docker-compose.yml   PostgreSQL and the backend container
└── .env.example         Values docker compose reads
```

## Tech stack

| Layer | Choice | Rationale |
|---|---|---|
| Language | Java 21 (LTS) | LTS release; compatibility with the libraries used is verified |
| Framework | Spring Boot 4.1 | Start a new project on a current version; natural support for layered architecture |
| Data access | Spring Data JPA (Hibernate) | Entity–table mapping, repository abstraction |
| Database | PostgreSQL 17 (Docker Compose for development) | Production-grade relational DB; same engine in development, tests and deployment |
| Schema migrations | Flyway | Versioned SQL scripts (`db/migration`); Hibernate only validates the schema |
| Validation | Spring Validation (Bean Validation) | Field-level required/format/range checks |
| API docs | springdoc-openapi 3.0.3 (Swagger UI) | Test endpoints from the browser and show the API in the demo |
| Helper | Lombok | Reduce boilerplate getter/setter/constructor code |
| Tests | JUnit 5, Mockito, Testcontainers | Integration tests run against a throwaway PostgreSQL container |

> springdoc's 2.x line targets Spring Boot 3; with Boot 4 the **3.x** line must be used.

Frontend:

| Layer | Choice | Rationale |
|---|---|---|
| Language / UI | TypeScript, React 19 | Typed API responses; the most widely used UI library |
| Build / dev server | Vite 8 | Fast dev server with hot reload; static build output |
| Components | Mantine 9 (`core`, `form`, `notifications`) | Ready table, select / multi-select, modal and form components; `@mantine/form` maps the backend's `fieldErrors` to inputs |
| Server state | TanStack Query 5 | Caching, loading/error states and retries for API calls |
| Routing | React Router 8 | Page routes, loaded lazily per page; filters kept in the address |
| Tests / lint | Vitest, Testing Library, oxlint | Unit and component tests on jsdom against a mocked API; fast linting |

## Requirements

- JDK 21 or newer (`java -version`)
- No need to install Maven — the bundled Maven Wrapper (`./mvnw`) is used
- Docker with Compose (e.g. Docker Desktop) — runs PostgreSQL, and is needed by the tests
  (Testcontainers). No local PostgreSQL installation is required.
- For the frontend: Node.js 24 and npm 11 (`node -v`)

## Setup and run

```bash
git clone <repository-url>
cd cto-project-tracker

docker compose up -d db         # starts PostgreSQL (host port 5433)
cd backend
./mvnw spring-boot:run          # starts the application (http://localhost:8080)
```

On first start Flyway creates the schema and the demo data is loaded (see [Demo data](#demo-data)).
The database lives in a Docker volume, so **data survives restarts**. To start over with an empty
database (demo data is loaded again on the next start):

```bash
docker compose down -v          # removes the container and its data volume
docker compose up -d db
```

Alternatively, build and run a jar (from `backend/`):

```bash
./mvnw clean package
java -jar target/cto-project-tracker-backend-0.0.1-SNAPSHOT.jar
```

### Run everything with Docker

The app ships with a multi-stage [`backend/Dockerfile`](backend/Dockerfile) (builds the jar inside the image, then runs it on a
JRE). `docker-compose.yml` starts it together with PostgreSQL, so no local Java/Maven is needed:

```bash
docker compose up -d --build            # PostgreSQL + application

curl http://localhost:8080/api/health   # {"status":"UP","databaseStatus":"UP", ...}

docker compose down                     # stop (add -v to also delete the data)
```

The image is built with `-DskipTests`: Testcontainers needs Docker, which is not reachable inside
`docker build`. Run `./mvnw test` (in `backend/`) before building an image.

### Verifying it runs

| What | Address | Expected |
|---|---|---|
| Health endpoint | http://localhost:8080/api/health | `status: UP`, `databaseStatus: UP` |
| Swagger UI | http://localhost:8080/swagger-ui.html | API documentation opens |
| OpenAPI JSON | http://localhost:8080/v3/api-docs | Returns the OpenAPI schema |

```bash
curl http://localhost:8080/api/health
```

```json
{
  "status": "UP",
  "application": "cto-project-tracker-backend",
  "databaseStatus": "UP",
  "database": "PostgreSQL 17.11",
  "timestamp": "2026-07-22T15:49:49.528096+03:00"
}
```

The `databaseStatus` field is produced by actually issuing a query against the database; it confirms
not just that the application is up, but that the DB connection works.

### Frontend

With the backend running on port 8080:

```bash
cd frontend
npm install
npm run dev          # http://localhost:5173
```

The home page calls `/api/health` and shows the backend and database status, so it also confirms
that the browser can reach the API (CORS). The API address comes from `VITE_API_BASE_URL`
(default `http://localhost:8080`); to change it, copy `frontend/.env.example` to
`frontend/.env.local`. If the frontend runs on another address, add that address to the backend's
`CORS_ALLOWED_ORIGINS`.

| Command (in `frontend/`) | What it does |
|---|---|
| `npm run dev` | Dev server with hot reload |
| `npm run build` | Type check and production build into `dist/` |
| `npm test` | Unit tests (Vitest) |
| `npm run lint` | Lint (oxlint) |

All API calls go through one client (`frontend/src/api/client.ts`). It turns the backend's
`ApiError` body into a typed `ApiRequestError` (`status`, `code`, `fieldErrors`), so screens can
tell error types apart and forms can show `fieldErrors` next to the right input. Network failures
get the code `NETWORK_ERROR`.

Shared building blocks for the screens: `src/api/types.ts` (response types and enum values, same
names as the backend), `src/lib/labels.ts` (Turkish labels and badge colors for every enum),
`EnumBadge` and `QueryState` (one way to show loading, empty and error states), `PageHeader`,
`ProgressValue`, and `ListControls` (result count, sorting and paging for the paged lists).

Screens:

| Address | Screen |
|---|---|
| `/` | CTO dashboard: counters, project table and filters (year / week, owner, project status, overall status, risk, delay) |
| `/projects` | Project list with search by project, customer or owner (Turkish letters and case ignored) |
| `/projects/:id` | Project details and its weekly reports, newest week first; delete with confirmation (blocked while the project has reports) |
| `/projects/new`, `/projects/:id/edit` | Project form; checks the backend's rules before sending and shows the backend's `fieldErrors` under the matching input |
| `/reports/:id` | Weekly report: stage, progress, status, risk, live tasks and the four text fields; delete with confirmation (its work items are deleted too). Below it, the report's work items with add / edit in a dialog and delete with confirmation |
| `/projects/:id/reports/new`, `/reports/:id/edit` | Weekly report form (see below) |
| `/reports` | Weekly reports of all projects, past weeks included; filters for project, owner, year with a week range, report date range, stage, overall status and risk; paging and sorting, all kept in the address. A reversed range cannot be chosen (earlier weeks are disabled in "Son hafta", the date inputs limit each other) |
| `/work-items` | Work items across projects, from each project's latest report (or the report up to a chosen year / week); filters for project, owner name, status and delay, paging and sorting, all kept in the address |

Weekly report form:

- The week is the week the report describes; the report date must fall in that week or the week
  after (in the demo data a report is dated the Monday after its week). The form checks this before
  sending, with the backend's messages, and shows "Bu rapor 2026 / 31. hafta olarak kaydedilir".
- The year is not entered: it is the year of the described week, which can differ from the report
  date's year at the turn of the year (week 53 of 2026 reported on 4 January 2027 is 2026).
- A new report starts with today's date, last week as the week (always a valid pair) and the
  stage of the project's latest report.
- When editing, only the current stage and the next one can be chosen (the backend rejects going
  back or skipping a stage); the others are shown disabled.
- Backend errors land where they belong: a second report for the same week (`409`) under the week,
  a stage rule violation under the stage, `fieldErrors` under their inputs, anything else above the form.

Filters are kept in the address (`/?riskLevel=ORTA,YUKSEK&year=2026`), so a filtered view survives
a reload and can be shared as a link. Invalid values in a hand-edited address are ignored, the same
way the backend would reject them (for example a week without a year). The header shows whether the
backend is reachable (`/api/health`, refreshed every minute). Each page is loaded as a separate
chunk the first time it is opened, so the first visit downloads only the layout and that page.

## Configuration

All settings live in [`backend/src/main/resources/application.yml`](backend/src/main/resources/application.yml).
Database connection details come from environment variables; the defaults point at the development
database in `docker-compose.yml`. The defaults are development-only values — any other environment
must supply its own through environment variables, never through committed files.
[`.env.example`](.env.example) lists the values `docker compose` reads; copy it to `.env` (ignored by
git) to change them.

| Environment variable | Default | Description |
|---|---|---|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5433` | PostgreSQL port (5433 avoids clashing with a locally installed PostgreSQL on 5432) |
| `DB_NAME` | `ctotracker` | Database name |
| `DB_USER` | `ctotracker` | Database user |
| `DB_PASSWORD` | `ctotracker_dev` | Database password (development only) |
| `SPRING_PROFILES_ACTIVE` | *(none → `demo`)* | Active Spring profile, see below |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Comma-separated origins allowed to call `/api/**` from a browser (the frontend); other origins get **403** |

**Profiles:** when no profile is chosen, the `demo` profile is used and `DataSeeder` loads the demo
data into an empty database. Choosing any other profile (e.g. `SPRING_PROFILES_ACTIVE=prod`) starts
without demo data.

Other notable settings:

| Setting | Value | Description |
|---|---|---|
| `server.port` | `8080` | Application port |
| `spring.flyway.enabled` | `true` | Flyway applies the migrations in `backend/src/main/resources/db/migration` on startup |
| `spring.jpa.hibernate.ddl-auto` | `validate` | Hibernate never changes the schema; startup fails if entities and tables don't match |

### Database migrations

The schema is created and changed **only** by Flyway scripts in
[`backend/src/main/resources/db/migration`](backend/src/main/resources/db/migration) (`V1__init.sql`, `V2__...`, `V3__...`).
Flyway records each applied script with a checksum, so an applied script is never edited — not even
a comment. A schema change is a new script with the next version number, together with the matching
entity change.

## API endpoints

| Method | Path | Description |
|---|---|---|
| GET | `/api/health` | Application and DB health check |
| GET | `/api/users` | Users sorted by name (`id`, `fullName`, `role`; optional `role` filter) — for picking a project owner |
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

- A project can have only **one report per week of a year** — a duplicate `(project, year, week)`
  returns **409**. The same week number in another year is a separate report.
- The year is not sent in the request. It is the ISO year of the **week the report describes**,
  derived from `weekNumber` and `reportDate` and returned as `year`: the year in which that week
  is nearest to the report date. A report is written during its week or the week after, so
  `reportDate` must be between that week's Monday and the Sunday after it; otherwise **400**
  `VALIDATION_ERROR` with `reportDate` in `fieldErrors`. Week 53 exists only in 53-week years
  (otherwise `weekNumber` in `fieldErrors`). Example: week 53, report date Monday 2027-01-04 →
  `year: 2026`. Flyway V3 recalculated existing years with this rule (V2 had used the report date's
  ISO year, which put such a report in 2027).
- Progress is tracked as ordered stages `ANALIZ → GELISTIRME → TEST → TAMAMLANDI`; the percentage
  (`25 / 50 / 75 / 100`) is **derived from the stage, never entered by hand**.
- On update a report's stage may stay the same or advance exactly one step; going backward or
  skipping a stage returns **400**.
- Work items belong to a report; deleting a report also deletes its work items (cascade). The
  report's **live task count** is derived from the number of `DEVAM_EDIYOR` work items — not stored
  separately.
- A project that still has weekly reports cannot be deleted (**409**); delete its reports first.
- The dashboard summary reports each project's **latest** report (latest year, then highest week). Projects with
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
| `GET /api/dashboard/summary` | `projectId`, `projectStatus`, `ownerId`, `year`, `weekNumber`, `overallStatus`, `riskLevel`, `delayed` | None; rows ordered by project id |
| `GET /api/reports` | `projectId`, `ownerId`, `year`, `weekFrom`, `weekTo`, `dateFrom`, `dateTo`, `overallStatus`, `riskLevel`, `progressStage` | Default `reportDate,desc`; sortable by `reportDate`, `weekNumber`, `projectName` |
| `GET /api/work-items` | `projectId`, `year`, `weekNumber`, `assignee`, `status`, `delayed` | Default `plannedDate,asc` (items without a date last); sortable by `plannedDate`, `title`, `projectName` |

- Different parameters combine with **AND**. Status, risk and stage parameters accept several
  comma-separated values, which combine with **OR** (`riskLevel=ORTA,YUKSEK`).
- On the dashboard and the work item list, `year` and `weekNumber` are a reference point, not a
  filter: each project is evaluated on its latest report up to that point. Without them that is the
  latest report; `year=2026` alone means up to the end of 2026; `year=2026&weekNumber=29` means up to
  week 29 of 2026, including earlier years. On the dashboard, projects with no report by then are
  still listed with `hasReport: false`, unless a report-based filter (`overallStatus`, `riskLevel`,
  `delayed`) is also given.
  The work item list shows only the items of that evaluated report, so the same task is not
  repeated across weeks.
- On the report list, `year` is a plain filter on the report's year, and `weekFrom`/`weekTo` apply
  within that year.
- A week number means nothing without its year, so `weekNumber`, `weekFrom` and `weekTo` are only
  accepted together with `year`; on their own they return **400** `VALIDATION_ERROR` with the week
  parameter in `fieldErrors`. `year` must be between 2000 and 2100.
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

Under the `demo` profile (the default), `DataSeeder` loads sample data on startup when the database
is empty, so Swagger and the endpoints are usable immediately without manual setup. Records already
in the database are kept; reset with `docker compose down -v` to get the original demo data back.

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

Negative checks worth showing: a duplicate `(project, year, week)` report → **409**, skipping a progress
stage on update → **400**, deleting a project that still has reports → **409**.

## Package and layer structure

```
com.kolaysoft.ctotracker
├── config/           Application configuration (OpenAPI/Swagger, demo data seeder)
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
| `DUPLICATE_RESOURCE` | 409 | Uniqueness violation (project+year+week, email) |
| `RESOURCE_IN_USE` | 409 | Resource can't be deleted because other records depend on it (e.g. a project that still has reports) |
| `INTERNAL_ERROR` | 500 | Unexpected error; technical detail is logged, not leaked to the client |

## Tests

```bash
cd backend
./mvnw test        # Docker must be running

cd frontend
npm test           # frontend unit tests, no backend needed
```

Spring tests run against a real PostgreSQL 17 started by Testcontainers (`TestcontainersConfiguration`);
the schema comes from the same Flyway scripts as the application. Each test context starts with an
empty database. Only `MvpIntegrationTest` uses the `demo` profile on purpose; every other test
creates its own data. Service tests use Mockito and need no database.

Backend tests (140 in total):

- `MvpIntegrationTest` — end-to-end HTTP flows (@SpringBootTest + MockMvc): happy path
  (project → report → work item → dashboard), 404 / 400 / 409, stage skip, a report date outside
  its week (400 on `reportDate`), dashboard filter
- `FilterIntegrationTest` — T13 filters over HTTP (MockMvc): dashboard filters and counters, year and
  week reference (including across years), report paging and sorting, work item filters, null-last default sort, and the error
  cases of the filter contract (such as a week parameter without `year`)
- `WeeklyReportQueryTest` / `WorkItemQueryTest` — filter queries against the database
  (@DataJpaTest): inclusive ranges, AND/OR combination, the year filter, evaluated-report selection
  by year and week (also at the year boundary), case-insensitive
  assignee search, and the delay condition matching the Java delay rule (`DelayPolicy`)
- `WeeklyReportYearTest` — report year on PostgreSQL: the year of the described week at year
  boundaries, and the V3 migration SQL (re-run on rows stored with the V2 rule) gives the same year
  as the Java rule; the year follows both week and report date, week 53 only in 53-week years,
  uniqueness is per project + year + week, and the latest report compares the year before the week
- `SortParameterTest` — sort parsing: default, direction, field mapping, rejected values
- `HealthControllerTest` — health endpoint and DB connection (reports PostgreSQL)
- `UserControllerTest` — user list sorted by name, role filter, e-mail not exposed, unknown role (400)
- `CorsConfigTest` — CORS: the frontend origin passes preflight and gets the allow header, other
  origins get 403
- `GlobalExceptionHandlerTest` — the error-format contract (400 / 404 / 405 / 409 / 500), including
  invalid enum/type request parameters
- `WeeklyReportServiceTest` — business rules: uniqueness (409), stage skip/backward (400),
  report date before its week or after the following week and a missing week 53 (400 with the
  field), year of the described week, percentage derivation, not-found (404)
- `ProjectServiceTest` — project create/update/mapping, owner-not-found (404), delete-with-reports (409)
- `WorkItemServiceTest` — work item CRUD, report-not-found (404), delete
- `DashboardServiceTest` — portfolio aggregation, latest-report selection, risk and multi-value
  filters, year and week reference, delay filter and counter, empty portfolio
- `CtoProjectTrackerBackendApplicationTests` — does the Spring context load

Frontend tests (130, Vitest + Testing Library on jsdom; component tests never reach the real backend,
`src/test/mockApi.ts` stands in for it and `src/test/fixtures.ts` holds the shared test data):

- `client.test.ts` — query string building (empty values skipped, multi-value parameters joined
  with commas) and error handling (`ApiError` body, non-JSON error, network failure)
- `QueryState.test.tsx` — the shared loading / empty / error (with retry) states
- `EnumBadge.test.tsx` — enum values shown with their Turkish label, a dash when there is no value
- `AppLayout.test.tsx` — every screen is a menu link; the current page's item is active, also on
  sub-pages (`/projects/3`, `/reports/4`, `/work-items?page=2`)
- `searchParams.test.ts` — reading filters from the address (unknown enum values, out-of-range and
  non-numeric numbers dropped) and writing them back
- `dashboard/filters.test.ts` — dashboard filters from the address: all filters, week without year
  dropped, invalid values dropped, round trip
- `DashboardPage.test.tsx` — counters and rows (including a project without a report), address
  filters sent to the API, reference text for year / week, clearing filters, empty result
- `format.test.ts` — Turkish-insensitive search, date and week formatting, id parsing from the address
- `ProjectListPage.test.tsx` — list with detail links, search typed or taken from the address
- `ProjectDetailPage.test.tsx` — details and reports newest first, delete disabled when there are
  reports, delete and return to the list (no request for the deleted project), 409 shown in the
  dialog, unknown and invalid ids
- `ProjectFormPage.test.tsx` — required fields checked before sending, create (empty optional
  fields sent as `null`), backend `fieldErrors` under the input, other errors above the form, edit
  with current values and `PUT`
- `isoWeek.test.ts` — ISO week and week-based year, including year boundaries (2027-01-01 is week 53
  of 2026); invalid dates; week 53, Mondays, and the report week rule with the same table and
  messages as the backend
- `ReportFormPage.test.tsx` — defaults (last week, today, latest stage; the date is fixed in the
  test), create with the request body, required choices and the week / date rule checked before
  sending, `409` under the week, only the current and next stage selectable
  when editing, `PUT`, stage rule error under the stage
- `ReportDetailPage.test.tsx` — report fields and texts, delete and return to the project
- `WorkItemsSection.test.tsx` — the report's work items, add in a dialog (request body), title
  checked before sending and backend `fieldErrors` in the dialog, edit with `PUT`, delete
- `workItems/filters.test.ts` — work item list state from the address: filters, page, sort,
  defaults left out of the address, invalid values dropped
- `WorkItemListPage.test.tsx` — links to project and report, address state sent to the API (page
  is 1-based in the address, 0-based for the backend), next page, clearing filters returns to page 1,
  owner search sends one request after typing stops, empty result
- `reports/listFilters.test.ts` — report list state from the address: all filters, defaults, week
  range without a year dropped, reversed week or date range drops the end, invalid values dropped
- `ReportListPage.test.tsx` — links to project and report, address state sent to the API, week
  range disabled without a year, earlier weeks disabled as the end of the range, sorting returns
  to page 1, empty result

## Known gaps

MVP scope decisions, with rationale, are in sections 13–14 of the pre-analysis document.
Not yet implemented at this stage:

- Authentication (login) and role-based authorization — out of MVP scope; `User.password` is
  omitted for now and will arrive with authentication
- Report locking, audit log, export — out of MVP scope
