# Weekly Project Status Reporting and CTO Tracking System

A web application where project managers enter weekly status reports for the projects they own,
and the CTO monitors the entire project portfolio from a single place: a Spring Boot REST API with
PostgreSQL, and a React frontend.

Stack: **Java + Spring Boot, React + TypeScript** · Author: Gökhan Kara

> **Status:** Projects, weekly reports, work items and the CTO dashboard are in place, with filtering
> on the dashboard and on cross-project report and work item lists (paging, sorting and a shared
> delay rule). Data lives in PostgreSQL, with the schema managed by Flyway and tests running against
> a real PostgreSQL via Testcontainers. Reports carry the year of the week they describe (Flyway V2,
> corrected at year boundaries in V3), and the filters accept a `year`. The React frontend covers
> all of these screens behind a login screen. The API requires a login (JWT) and enforces
> role-based permissions; the frontend shows each role only the actions it may take. Test results,
> open issues, risks and the next phase are under [Project status](#project-status).

Project documents (in [`docs/`](docs/), Turkish):
- [`on_analiz.md`](docs/on_analiz.md) — pre-analysis (problem, roles, data model, endpoints, assumptions, scope)
- [`filtreleme-analizi.md`](docs/filtreleme-analizi.md) — filtering analysis (decisions `KF-xx`, acceptance criteria `KK-xx`)
- [`filtre-sozlesmesi.md`](docs/filtre-sozlesmesi.md) — filter API contract (parameters, examples, errors)

---

## Repository layout

```
├── backend/             Spring Boot REST API (Maven project, Dockerfile)
├── frontend/            React + TypeScript web app (Vite; Dockerfile + nginx.conf)
├── docs/                Analysis notes and the filter API contract
├── .github/workflows/   CI: backend tests, frontend checks, Docker image builds
├── docker-compose.yml   PostgreSQL, backend and frontend containers
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
| Security | Spring Security 7, OAuth2 Resource Server (JWT, HS256), BCrypt | Stateless login tokens with Spring's own JWT support (no extra JWT library); passwords stored only as hashes |
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

`docker-compose.yml` starts the whole system — PostgreSQL, the backend and the frontend — so no
local Java, Maven or Node is needed:

```bash
docker compose up -d --build            # PostgreSQL + backend + frontend

open http://localhost:3000              # the web app (log in with a demo user, see Demo data)
curl http://localhost:8080/api/health   # the API directly: {"status":"UP","databaseStatus":"UP", ...}

docker compose down                     # stop (add -v to also delete the data)
```

| Service | Image | Address |
|---|---|---|
| `db` | `postgres:17-alpine` | `localhost:5433` (`DB_PORT`) |
| `app` | [`backend/Dockerfile`](backend/Dockerfile): builds the jar, runs it on a JRE | http://localhost:8080 (`APP_PORT`), Swagger at `/swagger-ui.html` |
| `web` | [`frontend/Dockerfile`](frontend/Dockerfile): builds the app with Node, serves it with nginx | http://localhost:3000 (`WEB_PORT`) |

The `web` container's nginx ([`frontend/nginx.conf`](frontend/nginx.conf)) serves the built files and
forwards `/api/**` to the `app` service. The browser talks to one address, so no CORS setup is
needed and the image does not contain a backend address (it is built with an empty
`VITE_API_BASE_URL`, so requests go to the page's own origin). Unknown paths such as
`/projects/3` return `index.html` and React Router resolves them.

The backend image is built with `-DskipTests`: Testcontainers needs Docker, which is not reachable
inside `docker build`. Run `./mvnw test` (in `backend/`) and `npm test` (in `frontend/`) before
building images. The backend takes a few seconds to start; until then the frontend shows
"Bağlantı yok" in the top bar.

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

Every page except `/login` needs a login; log in with one of the demo users
(e.g. `admin@example.com` / `Demo1234!`, see [Sample users](#demo-data)). The top bar shows the
backend and database status from `/api/health` (no login needed), so it also confirms that the
browser can reach the API (CORS). The API address comes from `VITE_API_BASE_URL`
(default `http://localhost:8080`; empty means the page's own origin, as in the Docker image); to
change it, copy `frontend/.env.example` to `frontend/.env.local`. If the frontend runs on another address, add that address to the backend's
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

Login (`src/auth/`, `src/pages/LoginPage.tsx`):

- The login response (token, expiry, user) is kept in `localStorage`, so a page reload or a new tab
  stays logged in; logging in or out in one tab applies to the other tabs too.
- The client sends the token as `Authorization: Bearer …` on every request. A request that was
  sent with a token and gets **401** (expired or invalid token) ends the session.
- `RequireAuth` guards every page: without a session it redirects to `/login` and remembers the
  address, so after logging in the user returns to it, with a note if the session had expired.
  After a deliberate logout the address is not remembered, since the next user may not be allowed
  to see that page.
- Logging in and out clears the query cache, so one user's data is never shown to the next one.
- The top bar shows the user's name and role and a logout button.
- Write actions follow the role (see [Permissions](#permissions)): `usePermissions()` hides the
  buttons, `RequirePermission` guards the form routes in `router.tsx`.

Shared building blocks for the screens: `src/api/types.ts` (response types and enum values, same
names as the backend), `src/lib/labels.ts` (Turkish labels and badge colors for every enum),
`EnumBadge` and `QueryState` (one way to show loading, empty and error states; a record that is
not found or not visible to the user gets its own "Kayıt bulunamadı" state, since retrying cannot help), `PageHeader`,
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
| `APP_PORT` | `8080` | Host port of the backend container (`docker compose` only) |
| `WEB_PORT` | `3000` | Host port of the frontend container (`docker compose` only) |
| `DB_NAME` | `ctotracker` | Database name |
| `DB_USER` | `ctotracker` | Database user |
| `DB_PASSWORD` | `ctotracker_dev` | Database password (development only) |
| `SPRING_PROFILES_ACTIVE` | *(none → `demo`)* | Active Spring profile, see below |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Comma-separated origins allowed to call `/api/**` from a browser (a frontend on another address, e.g. `npm run dev`); other origins get **403**. Not needed for the `web` container, which reaches the API through its own address |
| `JWT_SECRET` | *(development-only value)* | Secret that signs login tokens (HS256), **at least 32 bytes** or the app does not start. Any real environment must set its own random value (e.g. `openssl rand -base64 48`); changing it invalidates all issued tokens |
| `JWT_TOKEN_TTL` | `8h` | How long a login token is valid; there is no refresh, the user logs in again |

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

Every endpoint except health, login and the API docs requires a login token in the
`Authorization: Bearer <token>` header (see [Authentication](#authentication)).

| Method | Path | Description |
|---|---|---|
| GET | `/api/health` | Application and DB health check (no login needed) |
| POST | `/api/auth/login` | Log in with e-mail and password; returns a signed token (no login needed) |
| GET | `/api/auth/me` | The logged-in user (`id`, `fullName`, `role`) |
| GET | `/api/users` | Users sorted by name (`id`, `fullName`, `role`; optional `role` filter) — for picking a project owner |
| GET / POST | `/api/projects` | List / create projects |
| GET / PUT / DELETE | `/api/projects/{id}` | Project detail / update / delete |
| GET / POST | `/api/projects/{projectId}/reports` | List / create weekly reports for a project |
| GET / PUT / DELETE | `/api/reports/{id}` | Weekly report detail / update / delete |
| GET | `/api/reports` | Cross-project report list: filtered, paged, sorted |
| GET / POST | `/api/reports/{reportId}/work-items` | List / create work items for a report |
| GET / PUT / DELETE | `/api/work-items/{id}` | Work item detail / update / delete |
| GET | `/api/work-items` | Cross-project work item list: filtered, paged, sorted |
| GET / POST | `/api/projects/{projectId}/tracking-items` | List / create a project's risks, blockers, decisions and actions |
| GET / PUT / DELETE | `/api/tracking-items/{id}` | Risk, blocker, decision or action detail / update / delete |
| GET | `/api/tracking-items` | Cross-project risk / blocker / decision / action list: filtered, paged, sorted |
| GET | `/api/dashboard/summary` | CTO portfolio summary, with optional filters |

The full, interactive contract is in Swagger UI (use **Authorize** with a token from
`/api/auth/login`). Key business rules (see pre-analysis §H-03/H-04):

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
- A project that still has weekly reports, or risk / blocker / decision / action records, cannot
  be deleted (**409**); delete those first.
- The dashboard summary reports each project's **latest** report (latest year, then highest week). Projects with
  no report are still listed, with empty/default progress. Portfolio counters (total, high-risk,
  without-report, total live tasks) accompany the per-project rows.
- The dashboard also summarizes risks, blockers, decisions and actions: open risks, open blockers,
  pending decisions and overdue follow-ups across the listed projects, and on each row the
  project's open and overdue records and the highest level among its open risks and blockers. These
  always show today's state; a year / week reference does not change them.

**Risks, blockers, decisions and actions** (tracking items) belong to a project, not to a week: a
risk lives until it is closed. All four types share one table (`tracking_items`, Flyway V5) and
one API; `type` is `RISK`, `ENGEL`, `KARAR` or `AKSIYON`.

- `riskLevel` is required for risks and blockers and not accepted for decisions and actions.
- A record that is not closed (`ACIK`, `DEVAM_EDIYOR`) needs an `assignee` (free text, like on work
  items) and a `followUpDate`. A new or changed follow-up date cannot be before today; an unchanged
  past date is kept, so an overdue record can still be edited without moving its date. All of these
  return **400** `VALIDATION_ERROR` with the field in `fieldErrors`.
- `closedDate` is set by the server: today when the record is closed (`KAPANDI`), cleared when it
  is reopened. For a decision, open means "decision pending" and closed means "decided".
- A record is **overdue** (`overdue: true`) when it is not closed and its follow-up date is before
  today. Unlike work items, the reference is today, not a report date, because these records are
  not tied to a week. "Today" is the date in Turkey (`Europe/Istanbul`), whatever the server's time
  zone.
- A project's list shows open records first, by follow-up date, and closed ones last.

> **Scope note (from the pre-analysis):** progress uses a single staged model, so there is no
> separate "target %"; the schedule (`scheduleStatus`) field is out of MVP scope. Both are
> documented decisions in the analysis document.

## Authentication

The API is stateless: logging in returns a signed JWT, and every later request carries it.

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email": "cto@example.com", "password": "Demo1234!"}'
# {"token":"eyJ...","expiresAt":"...","user":{"id":3,"fullName":"Zeynep Demir","role":"CTO"}}

curl -s http://localhost:8080/api/projects -H "Authorization: Bearer <token>"
```

- Passwords are stored only as **BCrypt** hashes (`users.password_hash`, Flyway V4) and never
  appear in a response. A user without a password cannot log in.
- A wrong password and an unknown e-mail get the same **401** `INVALID_CREDENTIALS` answer, so the
  API does not reveal which e-mails exist.
- The token is signed with `JWT_SECRET` (HS256) and holds the user's id, e-mail, name and role. It
  expires after `JWT_TOKEN_TTL` (8 hours); there is no refresh token, the user logs in again.
- No token, or an invalid / expired one → **401** `UNAUTHORIZED` with a `WWW-Authenticate: Bearer`
  header; a logged-in user whose role is not allowed → **403** `FORBIDDEN`. Both use the
  [error format](#error-format).
- The token travels in a header, not a cookie, so CSRF protection is not needed; CORS allows the
  `Authorization` header from the frontend origin.

## Permissions

| | Admin | CTO | Project manager |
|---|---|---|---|
| Dashboard, lists, details | all projects | all projects | **only projects they own** |
| Create / edit / delete projects | ✓ | ✗ | ✗ |
| Write / delete weekly reports, work items, risks, blockers, decisions and actions | ✓ | ✗ | **only in projects they own** |
| User list, `/api/auth/me` | ✓ | ✓ | ✓ |

- **Role** (who may write what) is checked by method and path in the security filter, before the
  request reaches a controller (rules in `security/Permissions`). A role that is not allowed gets
  **403** `FORBIDDEN` without its body being read, so an invalid body or a missing record does not
  turn it into a 400 or 404. The CTO reads everything and changes nothing.
- A write request (`POST` / `PUT` / `PATCH` / `DELETE`) to a path without a rule is denied for
  everyone, so a new write endpoint does not work until its rule is added.
- **Ownership** (whose data) is part of every query: a project manager's lists, dashboard rows,
  counters and page totals only cover their own projects, combined with the filters by AND
  (`?ownerId=` of someone else returns an empty result, not an error).
- Someone else's project, report or work item opened by id is **404** for a project manager, with
  the same message as a record that does not exist, so the API does not reveal that it exists.
  This also applies to writes: a project manager writing into someone else's project gets 404.
- The frontend follows the same table (`frontend/src/auth/permissions.ts`): buttons for actions a
  role may not take are not shown, and a form address typed by hand shows "Yetkiniz yok" instead
  of the form. This is only for usability; the backend checks above are what enforce the rules.

## Filtering, paging and sorting

The full contract, with examples against the demo data, is in
[`filtre-sozlesmesi.md`](docs/filtre-sozlesmesi.md). Every parameter is also described in
Swagger UI with an example value.

| Endpoint | Filters | Paging / sorting |
|---|---|---|
| `GET /api/dashboard/summary` | `projectId`, `projectStatus`, `ownerId`, `year`, `weekNumber`, `overallStatus`, `riskLevel`, `delayed` | None; rows ordered by project id |
| `GET /api/reports` | `projectId`, `ownerId`, `year`, `weekFrom`, `weekTo`, `dateFrom`, `dateTo`, `overallStatus`, `riskLevel`, `progressStage` | Default `reportDate,desc`; sortable by `reportDate`, `weekNumber`, `projectName` |
| `GET /api/work-items` | `projectId`, `year`, `weekNumber`, `assignee`, `status`, `delayed` | Default `plannedDate,asc` (items without a date last); sortable by `plannedDate`, `title`, `projectName` |
| `GET /api/tracking-items` | `projectId`, `type`, `status`, `riskLevel`, `overdue`, `assignee` | Default `followUpDate,asc` (records without a date last); sortable by `followUpDate`, `createdAt`, `title`, `projectName` |

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
- Risks, blockers, decisions and actions are not tied to a week, so `/api/tracking-items` has no
  `year` or `weekNumber`; `overdue` is relative to today.
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

**Sample users** — all with the demo password **`Demo1234!`** (demo data only; a database seeded
before passwords existed gets the same password for users that have none):

| id | email | name | role |
|---|---|---|---|
| 1 | `ayse@example.com` | Ayse Yilmaz | PROJECT_MANAGER |
| 2 | `mehmet@example.com` | Mehmet Kaya | PROJECT_MANAGER |
| 3 | `cto@example.com` | Zeynep Demir | CTO |
| 4 | `admin@example.com` | Sistem Admin | ADMIN |

- **Projects:** Atlas IK (owner: Ayşe), e-Belge (owner: Mehmet), Nova POS (owner: Ayşe, no report yet)
- **Weekly reports:** Atlas IK weeks 29–31, e-Belge week 30, with a few work items. Atlas IK week 31 has
  an overdue work item, so the project shows `delayed: true` even though its status is `YOLUNDA`.
- **Risks, blockers, decisions and actions:** Atlas IK has an open high risk, an action whose
  follow-up date has passed (overdue) and a decision already taken; e-Belge has a blocker in
  progress and a pending decision. Their dates are relative to the day the data is loaded, so the
  demo looks the same every day. Nova POS has none, so it can still be deleted. A database seeded
  before these records existed gets them on the next start, as long as it has no records yet and
  the demo projects are still there.

All sample data is fictional (project, customer and person names are made up; emails use the reserved
`example.com` domain). The repository is public, so no real company or customer data is used.

## Demo scenario

An end-to-end walkthrough on a freshly seeded database (`docker compose down -v` first if you have
played with the data). Each step is done by the role that does it in real life; every user's
password is `Demo1234!`.

### In the web app

Start with `docker compose up -d --build` and open http://localhost:3000 (or run the dev servers
and open http://localhost:5173, see [Frontend](#frontend)). Log out from the user menu in the top
right between roles.

1. **CTO — portfolio at a glance** (`cto@example.com`)
   - The dashboard lists all three projects with their latest status; Nova POS has no report yet.
   - Filter **Risk → Yüksek**: only e-Belge. Clear it and filter **Gecikme → Yalnızca gecikenler**:
     only Atlas IK. Its status is "Yolunda", but a work item in week 31 is past its planned date.
   - **İş kalemleri** with the same delay filter shows that item, "Rapor ekrani entegrasyonu".
   - Open any project: there is no "Düzenle", "Sil" or "Yeni rapor" button, and typing
     `/projects/new` into the address bar shows "Yetkiniz yok". The CTO reads, but does not write.
2. **Project manager files a report** (`ayse@example.com`)
   - The dashboard and every list show only Ayşe's projects, Atlas IK and Nova POS.
   - Nova POS → **Yeni rapor**: the form starts at last week and today's date. Pick the general
     status and the risk, then save; the report opens with the progress taken from its stage.
   - **İş kalemi ekle** on the report: the item appears in the list and "Canlı task" goes up by one.
   - Nova POS → **Yeni rapor** again with the same week: the save is refused (**409**), and the
     message "Bu proje için … haftasına ait rapor zaten mevcut." appears under the week.
   - Back on the dashboard, Nova POS now shows the new report.
3. **Another project manager** (`mehmet@example.com`)
   - Everything is limited to e-Belge: dashboard, projects, reports and work items.
   - Opening Ayşe's Atlas IK report by address, http://localhost:3000/reports/4, shows "Kayıt
     bulunamadı", exactly as for a record that does not exist (**404**).
4. **Admin** (`admin@example.com`)
   - Sees every project, including the Nova POS report Ayşe just filed, and can create, edit and
     delete projects. **Sil** is disabled for a project that has reports.

### Over the API

The same flow with `curl` or Swagger UI (**Authorize** with the token). Log in with
`POST /api/auth/login` and send the returned token as `Authorization: Bearer <token>`.

1. **As the CTO:** `GET /api/dashboard/summary` → 200, portfolio counters and one row per project;
   `?riskLevel=YUKSEK` → only e-Belge; `?delayed=true` → only Atlas IK, and
   `GET /api/work-items?delayed=true` shows the overdue item.
2. **As the CTO, try to write:** `POST /api/projects/3/reports` → **403** `FORBIDDEN`, whatever the body.
3. **As Ayşe, file a report** for Nova POS (id 3):
   `POST /api/projects/3/reports` `{ "weekNumber": 33, "reportDate": "2026-08-13",
   "progressStage": "ANALIZ", "overallStatus": "YOLUNDA", "riskLevel": "DUSUK" }` → 201, `progressPercentage: 25`.
   The same request again → **409**.
4. **As Ayşe, add a work item:** `POST /api/reports/{reportId}/work-items`
   `{ "title": "Kickoff", "status": "DEVAM_EDIYOR", "assignee": "Ayşe" }` → 201, and
   `GET /api/reports/{reportId}` → `liveTaskCount: 1` (`{reportId}` comes from step 3).
5. **As Mehmet:** `GET /api/reports/4` (Ayşe's Atlas IK report) → **404**, and
   `GET /api/dashboard/summary` has only the e-Belge row.
6. **As the CTO again:** the dashboard shows Nova POS with `hasReport: true` and the fresh status.

Other negative checks: skipping a progress stage on update → **400**; deleting a project that still
has reports (as the Admin) → **409**.

## Package and layer structure

```
com.kolaysoft.ctotracker
├── config/           Application configuration (OpenAPI/Swagger, CORS, demo data seeder)
├── security/         Spring Security setup, token issuing, 401/403 responses, permission rules
│                     (Permissions) and the logged-in user's visibility (CurrentUser, Viewer)
├── controller/       HTTP layer: request handling, DTO exchange
├── service/          Business rules (stage transitions, uniqueness, delay rule)
├── repository/       Database access via Spring Data JPA
│   └── spec/         Filter conditions as JPA Specifications
├── entity/           JPA data model and enums
├── dto/              Request/response contracts (entities are not exposed directly)
│   └── filter/       Filter records passed from controllers to services
└── common/
    ├── error/        ApiError, ErrorCode, GlobalExceptionHandler
    ├── exception/    ResourceNotFoundException, DuplicateResourceException, BusinessRuleException,
    │                 InvalidCredentialsException
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
  "message": "Proje bulunamadı: 42",
  "path": "/api/projects/42"
}
```

Validation errors additionally include a `fieldErrors` array:

```json
{
  "timestamp": "2026-07-22T15:49:49.598473+03:00",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Gönderilen alanlar doğrulama kurallarına uymuyor.",
  "path": "/api/reports",
  "fieldErrors": [
    { "field": "weekNumber", "message": "Hafta numarası en fazla 53 olmalıdır." }
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
| `UNAUTHORIZED` | 401 | No login token, or the token is invalid or expired |
| `INVALID_CREDENTIALS` | 401 | Wrong e-mail or password at login (which one is not revealed) |
| `FORBIDDEN` | 403 | Logged in, but the role is not allowed to do this |
| `INTERNAL_ERROR` | 500 | Unexpected error; technical detail is logged, not leaked to the client |

## Tests

```bash
cd backend
./mvnw test        # Docker must be running

cd frontend
npm test           # frontend unit tests, no backend needed
```

**Continuous integration:** [`.github/workflows/ci.yml`](.github/workflows/ci.yml) runs on every push
to `main` and every pull request, in three parallel jobs: backend tests (`./mvnw test`, Java 21,
Testcontainers on the runner's Docker), frontend checks (`npm ci`, lint, tests, build, Node 24) and
`docker compose build` (both images still build). If backend tests fail, the Surefire reports are
attached to the run.

Spring tests run against a real PostgreSQL 17 started by Testcontainers (`TestcontainersConfiguration`);
the schema comes from the same Flyway scripts as the application. Each test context starts with an
empty database. Only `MvpIntegrationTest` uses the `demo` profile on purpose; every other test
creates its own data. Service tests use Mockito and need no database; they run as an Admin
(a mocked `CurrentUser`), since ownership is covered by `AuthorizationIntegrationTest`.

Backend tests (194 in total). The HTTP tests of business rules run as an Admin
(`@WithMockUser`); login, tokens and permissions are tested with real tokens:

- `MvpIntegrationTest` — end-to-end HTTP flows (@SpringBootTest + MockMvc): happy path
  (project → report → work item → dashboard), 404 / 400 / 409, stage skip, a report date outside
  its week (400 on `reportDate`), dashboard filter
- `FilterIntegrationTest` — filter endpoints over HTTP (MockMvc): dashboard filters and counters, year and
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
- `TrackingItemIntegrationTest` — risks, blockers, decisions and actions over HTTP: create, read,
  close (closing date set), reopen (cleared) and delete; the project list order and the overdue
  flag on the seed data; field errors (`riskLevel` by type, a past follow-up date, a missing
  assignee); a decision closed without a follow-up date; a project with records cannot be deleted;
  the cross-project list (default order, filters, paging) and the dashboard's tracking counters and
  row fields (not changed by a week reference, narrowed by a project filter)
- `TrackingItemQueryTest` — tracking item filter queries against the database (@DataJpaTest): the
  overdue condition (today's follow-up is not overdue, a closed record without a date stays in the
  `overdue=false` list), multi-value type / status / risk filters, assignee search, project owner
- `SortParameterTest` — sort parsing: default, direction, field mapping, rejected values
- `HealthControllerTest` — health endpoint and DB connection (reports PostgreSQL)
- `AuthorizationIntegrationTest` — the permission matrix end to end with the demo users logging in:
  lists, dashboard (rows and counters), cross-project lists scoped to the project manager's own
  projects; someone else's records (tracking items included) 404 with the same message as a missing
  one; the CTO gets 403 on every write, also with an invalid body or a missing record (no `fieldErrors` leak); a write
  without a rule is denied even for the Admin; project managers can't manage projects (403) and
  write only into their own (404 elsewhere); the Admin can do everything
- `AuthControllerTest` — login returns a token that works on protected endpoints and `/me`; wrong
  password, unknown e-mail and a user without a password get the same 401; empty fields (400); no
  token, a tampered token and an expired token (401, `ApiError` + `WWW-Authenticate`); a deleted
  user's token; public health and API docs; BCrypt storage; a JWT secret shorter than 32 bytes
- `UserControllerTest` — user list sorted by name, role filter, e-mail not exposed, unknown role (400)
- `CorsConfigTest` — CORS: the frontend origin passes preflight and gets the allow header, other
  origins get 403
- `GlobalExceptionHandlerTest` — the error-format contract (400 / 403 / 404 / 405 / 409 / 500), including
  invalid enum/type request parameters and a permission denial inside a controller (403, not 500)
- `WeeklyReportServiceTest` — business rules: uniqueness (409), stage skip/backward (400),
  report date before its week or after the following week and a missing week 53 (400 with the
  field), year of the described week, percentage derivation, not-found (404)
- `ProjectServiceTest` — project create/update/mapping, owner-not-found (404), delete with reports or
  tracking items (409)
- `WorkItemServiceTest` — work item CRUD, report-not-found (404), delete
- `TrackingItemServiceTest` — tracking item rules with the clock fixed: risk level by type, assignee
  and follow-up date required while open, no past follow-up date on create, an unchanged past date
  kept (and overdue) on update but a new past date rejected, the closing date following the status,
  the project list order, someone else's project (404), the dashboard summary per project
- `DashboardServiceTest` — portfolio aggregation, latest-report selection, risk and multi-value
  filters, year and week reference, delay filter and counter, empty portfolio; tracking counters
  only from the listed projects
- `CtoProjectTrackerBackendApplicationTests` — does the Spring context load

Frontend tests (166, Vitest + Testing Library on jsdom; component tests never reach the real backend,
`src/test/mockApi.ts` stands in for it and `src/test/fixtures.ts` holds the shared test data):

- `client.test.ts` — query string building (empty values skipped, multi-value parameters joined
  with commas; an empty `VITE_API_BASE_URL` targets the page's origin), error handling (`ApiError` body, non-JSON error, network failure), the token sent
  in the `Authorization` header, a 401 ending the session (403 does not)
- `session.test.ts` — the session store: saved to `localStorage`, an expired token is no session,
  logout reason kept when a 401 follows, login and logout in another tab
- `LoginPage.test.tsx` — redirect to login and back to the requested address (query string
  included) after logging in, trimmed e-mail, query cache cleared; wrong password (backend message,
  password cleared), empty fields not sent, backend `fieldErrors` under the input, network error;
  already logged in goes to the home page; a note after an expired session; no return to the old
  page after a deliberate logout
- `RequireAuth.test.tsx` — the page shows with a session; a 401 from the page's request returns to
  login with the note
- `RequirePermission.test.tsx` — the permission table per role (same as the backend's), no
  permissions without a session; a form route shows "Yetkiniz yok" to the CTO and project manager
- Page tests run as an Admin by default; each screen with write actions also checks what the CTO
  and a project manager see (no "Yeni proje" for either, project edit / delete only for the Admin,
  report and work item actions for the project manager, none for the CTO)
- `QueryState.test.tsx` — the shared loading / empty / error (with retry) states; a 404 shows
  "Kayıt bulunamadı" with a link home instead of a retry
- `EnumBadge.test.tsx` — enum values shown with their Turkish label, a dash when there is no value
- `AppLayout.test.tsx` — every screen is a menu link; the current page's item is active, also on
  sub-pages (`/projects/3`, `/reports/4`, `/work-items?page=2`); the user's name and role in the top
  bar, logout ends the session and clears the cache
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

## Project status

**Done:** projects, weekly reports and work items (create, edit, delete, with their business
rules); the CTO dashboard and the filtered, paged report and work item lists; login with JWT and
role-based permissions in the API, mirrored in the UI; the whole stack (PostgreSQL, backend,
frontend behind nginx) in Docker Compose; CI on every push.

### Latest test run

Run on 2026-11-10 against the current `main`:

| Check | Command | Result |
|---|---|---|
| Backend tests | `cd backend && ./mvnw test` | 194 passed, 0 failed, 0 skipped |
| Frontend tests | `cd frontend && npm test` | 166 passed in 23 files |
| Frontend lint | `npm run lint` | no findings |
| Frontend build | `npm run build` | builds; largest chunk 324 kB (100 kB gzip) |

The [Tests](#tests) section lists what each test class covers.

### Open issues

- **Demo data without Turkish letters:** seeded names and texts are stored in ASCII ("Ayse Yilmaz",
  "Demo Musteri A.S."), while the UI and the API messages use Turkish letters. Cosmetic only.
- **A slow test can time out on a busy machine:** the new-report form test takes about 0.6 s, but
  once exceeded Vitest's 5 s limit while the backend tests were running in parallel. It passes on
  its own and in a normal full run.
- **Unknown query parameters are ignored:** a typo such as `?risk=YUKSEK` returns the unfiltered
  result instead of an error (see [Filtering](#filtering-paging-and-sorting)).
- **No user management or password change:** users and their passwords come from the demo data.
- **No token refresh or server-side logout:** a token stays valid until it expires (8 hours), even
  after logging out in the UI.
- Report locking, audit log and export are out of the MVP scope (sections 7–8 of the
  pre-analysis document).

### Risks

- **Demo defaults in a real deployment.** The `demo` profile is the default and seeds users with a
  publicly known password, and `JWT_SECRET` falls back to a development value that is in this
  repository (anyone who knows it can sign a valid token). Both must be changed before the app
  runs anywhere other than a developer machine; today nothing stops it from starting with them.
- **The token is kept in `localStorage`,** so any script running on the page can read it. There are
  no third-party scripts and React escapes the content it renders, but an XSS bug would expose the
  token. An `httpOnly` cookie would avoid that, at the cost of adding CSRF protection.
- **Ownership is a filter in each query.** A new read endpoint that forgets it would show a project
  manager other people's data. `AuthorizationIntegrationTest` covers every current endpoint;
  each new one needs a test there too.
- **The ISO week rule exists twice,** in the backend and in `frontend/src/lib/isoWeek.ts`. Both are
  tested against the same table of dates, but a change on one side must be repeated on the other.
- **Not deployed yet:** it runs only locally in Docker, without HTTPS.

### Next phase

1. **Risk, decision and action records** for each project: who owns them, due dates and status,
   linked to the weekly report where they were raised, and visible on the dashboard.
2. **CTO metrics:** status and risk trends over the weeks for each project and the portfolio, with
   charts, and export of the filtered lists (CSV / Excel).
3. **User management for the Admin:** creating users, assigning roles and owners, resetting
   passwords; a password change for every user. After that, the app no longer depends on demo data.
4. **Production readiness:** a profile without demo data that refuses to start without a real
   `JWT_SECRET`, HTTPS, a deployment target, and token refresh or revocation.
5. **Quality:** browser end-to-end tests of the demo scenario in CI, Turkish letters in the demo
   data, and a longer timeout for the slow form tests.
