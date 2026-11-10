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
> role-based permissions; the frontend shows each role only the actions it may take. Each project
> also keeps its risks, blockers, decisions and actions with an assignee and a follow-up date
> (Flyway V5); they have their own list and are summarized on the dashboard. Every change made
> through the API is kept in a change history: who changed which fields of which record, and when
> (Flyway V6). Test results, open issues, risks and the next phase are under
> [Project status](#project-status).

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
| Excel export | Apache POI 5.5.1 (`poi-ooxml`) | Writes the CTO summary as an `.xlsx` file with typed number and date cells |
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
| Charts | `@mantine/charts` (Recharts) | The dashboard's weekly trend; same theme as the rest of the UI, loaded only with the charts |
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
get the code `NETWORK_ERROR`. File downloads (the Excel export) go through the same path with
`apiDownload`: a plain link cannot carry the login token, so the file is fetched with it, saved
under the name the backend suggests (`Content-Disposition`, UTF-8 form first), and errors and an
expired session behave like any other request.

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
| `/` | CTO dashboard: counters, project table and filters (year / week, owner, project status, overall status, risk, delay). **Excel'e aktar** downloads the same view as an `.xlsx` file, with the filters chosen. A second row of counters covers risks, blockers, decisions and actions (open risks, open blockers, pending decisions, overdue follow-ups); each card opens the matching list, and a note says these show today's state whatever year / week is chosen. Each project row shows its open records, the highest open risk level and how many are overdue, linking to that project's records. The table fits a 1280 px screen without scrolling sideways. Below it, "Son 12 hafta" charts the projects by status and the open risks, blockers, decisions and actions week by week (stacked bars, a tooltip per week, and the same numbers as a table on demand), using the dashboard's owner, project status and year / week filters |
| `/projects` | Project list with search by project, customer or owner (Turkish letters and case ignored) |
| `/projects/:id` | Project details, its risks, blockers, decisions and actions (add / edit in a dialog, delete with confirmation), and its weekly reports, newest week first; delete with confirmation (blocked while the project has reports or such records). At the bottom, "Değişiklik geçmişi": who changed what and when in the project, its reports, work items and records, newest first, as a timeline ("Risk seviyesi: ~~Orta~~ → Yüksek"), with a record type filter and "Daha fazla göster"; a change made in a dialog on the page shows up without reloading |
| `/projects/new`, `/projects/:id/edit` | Project form; checks the backend's rules before sending and shows the backend's `fieldErrors` under the matching input |
| `/reports/:id` | Weekly report: stage, progress, status, risk, live tasks and the four text fields; delete with confirmation (its work items are deleted too). Below it, the report's work items with add / edit in a dialog and delete with confirmation |
| `/projects/:id/reports/new`, `/reports/:id/edit` | Weekly report form (see below) |
| `/reports` | Weekly reports of all projects, past weeks included; filters for project, owner, year with a week range, report date range, stage, overall status and risk; paging and sorting, all kept in the address. A reversed range cannot be chosen (earlier weeks are disabled in "Son hafta", the date inputs limit each other) |
| `/work-items` | Work items across projects, from each project's latest report (or the report up to a chosen year / week); filters for project, owner name, status and delay, paging and sorting, all kept in the address |
| `/tracking-items` | Risks, blockers, decisions and actions across projects. Shows records that are not closed by default ("Kayıtlar": not closed / closed / all); filters for project, type, risk level, overdue and assignee, paging and sorting, all kept in the address |

Risk, blocker, decision and action dialog:

- The type is chosen at the top; the risk level field appears only for risks and blockers, and a
  level chosen before switching to another type is not sent.
- While the record is not closed, the assignee and the follow-up date are required, and a new or
  changed follow-up date cannot be before today; an unchanged past date can stay. These are the
  backend's rules, checked before sending with the same messages.
- For a decision the statuses read "Karar bekleniyor" / "Görüşülüyor" / "Karar verildi".
- In the tables, impact and action plan appear under the title, so a reader without write access
  sees them too; an overdue record has a "Gecikmede" badge, a closed one shows its closing date.

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

The trend charts use the badge colors' hue families, at steps chosen with a color-vision
check so neighboring segments stay apart for color-blind readers too (adjacent pairs ΔE ≥ 8.6 in
simulated color blindness); legends keep the series order, values sit in neutral text, and the
"Tablo olarak göster" button shows the numbers without relying on color. The chart library is
a separate chunk, so the dashboard's cards and table appear before it loads.

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
| GET | `/api/projects/{projectId}/history` | A project's change history (project, reports, work items, tracking items): newest first, paged |
| GET | `/api/dashboard/summary` | CTO portfolio summary, with optional filters |
| GET | `/api/dashboard/trend` | The portfolio week by week (default: the last 12 weeks), with the dashboard's project filters |
| GET | `/api/dashboard/export` | The CTO summary as an Excel file (`.xlsx`), with the dashboard's filters |

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
- The weekly **trend** applies the dashboard's week reference to every week of a window (default
  12 weeks, ending this week or at the given `year` / `weekNumber`): for each week, the projects by
  overall status, high-risk and delayed projects and the average progress, each project judged by
  its latest report up to that week. So the last week matches the dashboard opened with the same
  reference. Risks, blockers, decisions and actions open at the end of each week come from their
  creation and closing dates, plus how many were opened and closed that week. Their risk level
  history is not kept, so there is no level breakdown over time.
- The **Excel export** (`cto-ozeti-<date>.xlsx`) is the dashboard as a file, with the same filters,
  in four sheets: *Özet* (when it was made, the filters applied and every counter), *Projeler*
  (the dashboard rows), *Riskler ve aksiyonlar* (the open records of the listed projects, overdue
  first) and *Trend* (the 12 weeks ending at the same reference). Numbers are number cells and
  dates are date cells, so the sheets can be sorted and filtered in Excel; the header row stays in
  place and has filters. Text is always written as text, so a title starting with `=` is not run
  as a formula. A project manager's file only contains their own projects.
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

**Change history** (audit log, Flyway V6): every create, update and delete of a project, weekly
report, work item or tracking item made through the API is written as an event — who did it, when,
which record (type, id and a readable name such as the title or "2026 / 45. hafta") and which
project.

- An update keeps only the fields that changed, with the old and the new value; a create keeps the
  fields it filled; a delete keeps only the record's name. An update that changes nothing writes
  nothing.
- Values are stored raw (enum names, ISO dates, numbers; a project's owner by name); long texts are
  cut at 500 characters. Field names are the API's.
- The person comes from the login token (`sub` and `name`), never from the request, so writing an
  event needs no extra query.
- The event is written in the **same transaction** as the change: a change that is refused (400,
  403, 404, 409) leaves no trace, and if the event cannot be written the change is rolled back too.
- Events are only added; the API has no way to change or delete them. The record and project ids
  are not foreign keys, so the history of a deleted record stays. Deleting a report also deletes its
  work items; only the report's deletion is recorded. The demo data is not in the history (it does
  not go through the API).
- **Reading it:** `GET /api/projects/{projectId}/history`, newest first, with `entityType` and
  `entityId` filters and paging ([filter contract](docs/filtre-sozlesmesi.md), section 4b). Whoever
  can see the project can read its history: a project manager their own projects, the CTO and the
  Admin every project; anyone else gets **404** as for a missing project. A page loads in at most 4
  queries whatever its size: the changed fields of all its events come in one query.

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

## Change history

A small prototype on top of the MVP: the app records who changed what and when, and shows it on the
project page. The rules of what is recorded are under [API endpoints](#api-endpoints) ("Change
history"); this section is the technical, security and validation note.

**Design decisions**

- **Explicit calls in the services, not Hibernate Envers or entity listeners.** Every write goes
  through 12 service methods (4 record types × create / update / delete). Each takes a snapshot
  before the change (`AuditSubject`) and hands it to `AuditService`. That gives one table, a diff of
  readable field names and no extra tables per entity, and it is easy to test.
- **Same transaction as the change** (`Propagation.MANDATORY`). A refused change leaves nothing, and
  a change is never stored without its event. The alternative, writing after commit from an event
  listener, could lose the event while keeping the change.
- **Changed fields in their own table** (`audit_event_changes`), not a JSON column. The question
  "when did this risk level change?" becomes a plain `WHERE field = 'riskLevel'`. It also avoids
  depending on Hibernate's JSON mapping, which would need extra setup for the Jackson version Spring
  Boot 4 ships with.
- **Raw values, labels on screen.** Events store `YUKSEK` and `2026-11-13`; the frontend turns them
  into "Yüksek" and "13.11.2026". A renamed label also changes old entries, and the Excel export's
  duplicated labels do not grow.
- **No foreign keys to the record or project**, so the history of a deleted record stays readable.

**Security**

- **The person comes from the login token** (`sub`, `name`), never from the request. A body with
  `"actorName": "Zeynep Demir"` is ignored (checked end to end).
- **Read-only over the API.** There is no endpoint to change or delete history. `POST`, `PUT`,
  `PATCH` and `DELETE` on `/api/projects/{id}/history` get **403** even for the Admin, because
  unlisted writes are denied for everyone (see [Permissions](#permissions)).
- **Visibility follows the project.** A project manager reads only their own projects' history;
  anyone else gets the same **404** as for a project that does not exist, so its existence does not
  leak.
- **Output is text.** Values are shown as text by React. A title such as `<img src=x onerror=…>`
  appears literally and runs nothing (checked end to end).
- **What it stores:** people's names, which the app already shows, and the record's own fields,
  free texts cut at 500 characters. Passwords and users are not recorded.

**Validation:** the success indicators set before building, and how each was measured

| Indicator | Result | Measured by |
|---|---|---|
| Each of the 12 write paths writes exactly one event with the right person and fields | ✓ | `AuditIntegrationTest` |
| A refused change (400, 403, 404, 409) leaves no event; a failed event rolls the change back | ✓ (the rollback test fails when the error is swallowed) | `AuditIntegrationTest`, end-to-end step 3 |
| A project manager cannot read another project's history | ✓ 404, same as a missing project | `AuditIntegrationTest`, end-to-end step 5 |
| "Who raised this risk level, and when?" answered on one screen | ✓ project page, "Risk seviyesi: ~~Orta~~ → Yüksek" | end-to-end steps 1 and 8 |
| Small cost: a write adds one insert per event and one per changed field; a history page takes a fixed number of queries | ✓ at most 4 queries for a 20-event page (23 without batch loading) | `AuditIntegrationTest` with Hibernate statistics |

**Limits and next steps**

- History is tamper-proof only at the application level. A database user can still change or delete
  rows. Next step: a database role that can only `INSERT` and `SELECT` on the history tables, or a
  trigger that rejects `UPDATE` and `DELETE`.
- No retention policy: events are kept forever.
- The person's name comes from the token, so after a rename it can be up to 8 hours old (the app
  has no rename yet).
- The history of a deleted project stays in the database but cannot be read over the API.
  Deleting a report also deletes its work items, and only the report's deletion is recorded.
- There is no cross-project history page. The weekly trend does not use the history yet; it could
  show how risk levels moved.
- Changes made before Flyway V6, and the demo data, have no history.

## Filtering, paging and sorting

The full contract, with examples against the demo data, is in
[`filtre-sozlesmesi.md`](docs/filtre-sozlesmesi.md). Every parameter is also described in
Swagger UI with an example value.

| Endpoint | Filters | Paging / sorting |
|---|---|---|
| `GET /api/dashboard/summary` | `projectId`, `projectStatus`, `ownerId`, `year`, `weekNumber`, `overallStatus`, `riskLevel`, `delayed` | None; rows ordered by project id |
| `GET /api/reports` | `projectId`, `ownerId`, `year`, `weekFrom`, `weekTo`, `dateFrom`, `dateTo`, `overallStatus`, `riskLevel`, `progressStage` | Default `reportDate,desc`; sortable by `reportDate`, `weekNumber`, `projectName` |
| `GET /api/work-items` | `projectId`, `year`, `weekNumber`, `assignee`, `status`, `delayed` | Default `plannedDate,asc` (items without a date last); sortable by `plannedDate`, `title`, `projectName` |
| `GET /api/dashboard/trend` | `projectId`, `projectStatus`, `ownerId`, `year`, `weekNumber`, `weeks` (1–52, default 12) | None; weeks oldest first |
| `GET /api/tracking-items` | `projectId`, `type`, `status`, `riskLevel`, `overdue`, `assignee` | Default `followUpDate,asc` (records without a date last); sortable by `followUpDate`, `createdAt`, `title`, `projectName` |
| `GET /api/projects/{projectId}/history` | `entityType`, `entityId` (only with `entityType`) | Fixed: newest first |

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

- **Projects:** Atlas IK (owner: Ayşe), e-Belge (owner: Mehmet), Nova POS (owner: Ayşe, no report yet),
  Orion Mobil (owner: Mehmet)
- **Weekly reports:** Atlas IK weeks 29–31, e-Belge week 30, with a few work items. Atlas IK week 31 has
  an overdue work item, so the project shows `delayed: true` even though its status is `YOLUNDA`.
  Orion Mobil has a report for each of the last 10 weeks, dated relative to the day the data is
  loaded, so the dashboard's 12-week trend always moves. The project starts on track, turns at risk,
  is delayed and blocked for a week, and gets back on track. Its latest report is `YOLUNDA` with low
  risk and no work items, so it does not change the dashboard counters. A demo database seeded before
  this project existed gets it on the next start (it comes back if deleted, in the demo profile only).
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
   - The dashboard lists all four projects with their latest status; Nova POS has no report yet.
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
   - Everything is limited to his projects, e-Belge and Orion Mobil: dashboard, projects, reports and
     work items.
   - Opening Ayşe's Atlas IK report by address, http://localhost:3000/reports/4, shows "Kayıt
     bulunamadı", exactly as for a record that does not exist (**404**).
4. **Admin** (`admin@example.com`)
   - Sees every project, including the Nova POS report Ayşe just filed, and can create, edit and
     delete projects. **Sil** is disabled for a project that has reports.

### Risks, blockers, decisions and actions

On a freshly seeded database (dates in the demo data are relative to the day it is loaded):

1. **Ayşe tracks her projects** (`ayse@example.com`)
   - Under the dashboard's report counters, "Riskler ve aksiyonlar" shows only her projects:
     1 open risk, 0 blockers, 0 pending decisions, 1 overdue follow-up.
   - Nova POS → **Kayıt ekle**: choose **Engel**, a title, risk level **Yüksek**, an assignee and
     yesterday as the follow-up date. The dialog refuses the date ("Takip tarihi bugünden önce
     olamaz.") without sending anything; with a date three days ahead the blocker is added.
   - Add a **Karar** the same way: its status reads "Karar bekleniyor".
   - Atlas IK: the action "Musteriyle kapsam toplantisi" is marked **Gecikmede**. **Düzenle** →
     status **Kapandı** → it moves down, dimmed, with today as its closing date.
   - Back on the dashboard: 1 blocker, 1 pending decision, 0 overdue; the Nova POS row shows
     2 open records with **Yüksek**.
2. **Mehmet** (`mehmet@example.com`): **Riskler ve aksiyonlar** lists only e-Belge's two records;
   choosing Atlas IK in the project filter gives an empty list.
3. **CTO** (`cto@example.com`): the counters cover all projects (1 risk, 2 blockers, 2 pending
   decisions, 0 overdue). The **Açık engel** card opens the list of both blockers (Nova POS and
   e-Belge). Project pages show the records but no buttons to change them.
4. **Admin** (`admin@example.com`): **Sil** is now disabled on Nova POS as well, since it has
   records.

### Weekly trend and Excel export

1. **CTO** (`cto@example.com`), at the bottom of the dashboard: "Son 12 hafta" charts the projects by
   status and the open records, week by week. Hover a bar for that week's numbers, or press
   **Tablo olarak göster** to read them as a table.
   - Filter **Sorumlu → Mehmet Kaya**: e-Belge is at risk in every week, while Orion Mobil goes from
     "Yolunda" to "Riskli", "Gecikmeli", one week "Bloke", and back to "Yolunda". Eleven weeks ago
     Orion Mobil has no report yet ("Rapor yok").
   - Choose **Yıl 2026, Hafta 31**: both charts end at week 31, and every project is judged by its
     latest report up to each week.
   - **Excel'e aktar** downloads `cto-ozeti-<today>.xlsx` with the filters chosen: *Özet* (counters
     and the filters in words), *Projeler*, *Riskler ve aksiyonlar* (open records, overdue first)
     and *Trend* (the same 12 weeks).
2. **Ayşe** (`ayse@example.com`): the trend and the Excel file cover only Atlas IK and Nova POS.

### Change history

On a freshly seeded database the history is empty: the demo data is not recorded.

1. **Ayşe** (`ayse@example.com`) opens Atlas IK. At the bottom, "Değişiklik geçmişi" says there are
   no changes yet. In "Riskler ve aksiyonlar", **Düzenle** the risk "Entegrasyon API'si gec teslim
   edilebilir" and set **Risk seviyesi** to **Orta**. Without reloading, the history shows
   **Güncellendi**, "Risk / aksiyon: Entegrasyon API'si gec teslim edilebilir", her name, the time,
   and "Risk seviyesi: ~~Yüksek~~ → Orta".
2. **Mehmet** (`mehmet@example.com`): http://localhost:3000/projects/1 shows "Kayıt bulunamadı",
   with no history; on e-Belge he sees only e-Belge's changes.
3. **CTO** (`cto@example.com`) opens Atlas IK and sees Ayşe's change. **Kayıt türü → Risk /
   aksiyon** keeps only those records. There is no button to change anything.
4. **Admin** (`admin@example.com`): adds a work item to Atlas IK's week 31 report and deletes it. On
   Atlas IK both show up, "Eklendi" with its fields and "Silindi" with its title; the record is gone,
   its trace is not.

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
   `GET /api/dashboard/summary` has only the e-Belge and Orion Mobil rows.
6. **As the CTO again:** the dashboard shows Nova POS with `hasReport: true` and the fresh status.

Other negative checks: skipping a progress stage on update → **400**; deleting a project that still
has reports (as the Admin) → **409**.

Risks, blockers, decisions and actions over the API:

- **As Ayşe:** `POST /api/projects/3/tracking-items` `{ "type": "ENGEL", "title": "Kasa donanimi
  gecikti", "riskLevel": "YUKSEK", "assignee": "Ayşe", "followUpDate": "<a date from today on>",
  "status": "ACIK" }` → 201. Without `riskLevel`, or with a past `followUpDate` → **400** with the
  field in `fieldErrors`.
- **As the CTO:** `GET /api/tracking-items?overdue=true` → the overdue action;
  `GET /api/dashboard/summary` → `openRisks`, `openBlockers`, `pendingDecisions`,
  `overdueTrackingItems`. `POST /api/projects/1/tracking-items` → **403**.
- **As Mehmet:** `GET /api/tracking-items/{id}` of an Atlas IK record → **404**.
- **As the Admin:** `DELETE /api/projects/3` once Nova POS has a record → **409**.

Weekly trend and Excel over the API:

- **As the CTO:** `GET /api/dashboard/trend?ownerId=2` → 12 weeks, oldest first. Orion Mobil's
  status moves week by week in `projectsByStatus`. `?year=2026&weekNumber=31&weeks=4` → weeks
  28–31 of the fixed demo reports.
- **As the CTO:** `GET /api/dashboard/export?riskLevel=YUKSEK` → an `.xlsx` file
  (`Content-Disposition: attachment; filename="cto-ozeti-<today>.xlsx"`) with only e-Belge;
  `?weekNumber=31` without a year → **400** in JSON.
- **As Ayşe:** the same two requests cover only her projects.

Change history over the API:

- **As Ayşe:** `PUT /api/tracking-items/{id}` on Atlas IK's risk with `"riskLevel": "ORTA"` (the
  other fields unchanged), then `GET /api/projects/1/history` → one event, `"actorName": "Ayse
  Yilmaz"`, `"changes": [{ "field": "riskLevel", "oldValue": "YUKSEK", "newValue": "ORTA" }]`. The
  same request again adds nothing. Adding `"actorName": "Zeynep Demir"` to the body changes nothing:
  the person comes from the token.
- **As Mehmet:** `GET /api/projects/1/history` → **404**, as for `/api/projects/999/history`.
- **As the Admin:** `DELETE /api/projects/1/history` → **403**: history cannot be changed over the
  API. `GET /api/projects/1/history?entityId=4` → **400** (`entityId` needs `entityType`).

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

### Regression plan

Which tests guard each area before a release, and what is still missing. Backend and frontend tests
run in CI on every push. The browser end-to-end checks still run from a local Playwright script
against a clean Docker setup (see [Latest test run](#latest-test-run)).

| Area | Backend (unit / integration) | Frontend (component) | Browser end to end | Gap, closed in |
|---|---|---|---|---|
| Login, token, roles | `AuthControllerTest`, `AuthorizationIntegrationTest`, `CorsConfigTest`, `UserControllerTest` | `LoginPage`, `session`, `RequireAuth`, `RequirePermission`, `AppLayout`, `client` | every flow logs in as the role that does the work | a setup without demo users and the first admin (prod profile) |
| Projects | `ProjectServiceTest`, `MvpIntegrationTest` | `ProjectListPage`, `ProjectDetailPage`, `ProjectFormPage` | Admin: create, edit, delete blocked | — |
| Weekly reports and work items | `WeeklyReportServiceTest`, `WorkItemServiceTest`, `WeeklyReportYearTest`, `WeeklyReportQueryTest`, `WorkItemQueryTest`, `FilterIntegrationTest` | `ReportFormPage`, `ReportDetailPage`, `ReportListPage`, `WorkItemsSection`, `WorkItemListPage`, `isoWeek`, `listFilters` | report flow: new report, work item, same week 409, delay filter | per-row queries in the paged lists |
| Risks, blockers, decisions, actions | `TrackingItemServiceTest`, `TrackingItemQueryTest`, `TrackingItemIntegrationTest` | `TrackingItemsSection`, `TrackingItemListPage`, `filters` | risks and actions flow | — |
| Dashboard, trend, Excel | `DashboardServiceTest`, `TrendIntegrationTest`, `ExportIntegrationTest`, `DataSeederIntegrationTest` | `DashboardPage`, `TrendSection`, `filters` | trend and Excel flow, file content | — |
| Change history | `AuditServiceTest`, `AuditIntegrationTest` | `ProjectHistorySection`, `historyFormat` | change history flow (token identity, refused writes, XSS, paging) | history editable at the database level |
| Errors, paging, formats | `GlobalExceptionHandlerTest`, `SortParameterTest` | `QueryState`, `client`, `searchParams`, `format` | console errors checked in every flow | — |
| Build, health, setup | `HealthControllerTest`; images built in CI | production build in CI | local Docker demo | a production setup that refuses missing secrets, security headers, smoke test |
| All of the above | | | **not in the repository or CI** | browser checks moved into the repository and CI |

**Before a release** (smoke checklist, run on the production setup with an empty database):

1. The production setup refuses to start without its secrets (database password, `JWT_SECRET`, first
   admin).
2. The images build, every container becomes healthy, and `GET /api/health` reports `UP` with the
   database `UP`.
3. The web app opens; the demo users cannot log in; the first admin can.
4. Restarting does not create the admin again or change their password, and the data stays.
5. As the Admin: create a project, a weekly report with a work item and a risk; the dashboard and
   the Excel export show them; the project's change history lists each change.
6. Responses carry the security headers; Swagger is not reachable; logs are at `INFO` and show no
   secrets.

A release goes out when CI is green and every smoke step passes.

Spring tests run against a real PostgreSQL 17 started by Testcontainers (`TestcontainersConfiguration`);
the schema comes from the same Flyway scripts as the application. Each test context starts with an
empty database. Tests that rely on the demo data ask for the `demo` profile on purpose
(`MvpIntegrationTest`, `AuthorizationIntegrationTest`, `TrackingItemIntegrationTest`,
`ExportIntegrationTest`, `DataSeederIntegrationTest`, `AuditIntegrationTest`); every other test
creates its own data. Service tests use Mockito and need no database; they run as an Admin
(a mocked `CurrentUser`), since ownership is covered by `AuthorizationIntegrationTest`.

Backend tests (227 in total). The HTTP tests of business rules run as an Admin
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
  uniqueness is per project + year + week, and the dashboard's one-query "evaluated report per
  project" compares the year before the week, with and without a year / week reference
- `ExportIntegrationTest` — the Excel file read back with POI: the four sheets, the file name with
  today's date, the counters, project rows with Turkish labels (progress as a number, empty for a
  project without a report), open records with the overdue one first and the follow-up date as a
  date cell, the trend sheet, dashboard filters applied and written into *Özet*, a title starting
  with `=` stored as text, the file returned whatever `Accept` says while a bad parameter still gets
  a JSON 400
- `TrendIntegrationTest` — the weekly trend over HTTP with its own data and fixed dates: each week
  judged by the latest report up to it (status counts, high risk, delay with an overdue work item,
  average progress), records open at the end of each week from creation and closing dates, a week
  starting at midnight in Turkey rather than UTC, a window across the year boundary, the last week
  equal to the dashboard, an empty portfolio, invalid parameters
- `AuditIntegrationTest` — change history with the demo users logging in: each of the 12 write paths
  (project, report, work item, tracking item × create, update, delete) writes exactly one event
  with the right person and project, a report update keeps only its two changed fields, a project
  manager's risk level change is recorded once (the same request again writes nothing), refused
  changes (400, 403, 404, 409) leave no event, and a failure to write the event rolls the change
  back. Reading: newest first with raw values, type and record filters, paging, someone else's
  project 404 and the CTO's access, invalid parameters 400, and a 20-event page in at most 4 queries
  (23 without batch loading)
- `DataSeederIntegrationTest` — the demo data's trend project: the default 12-week trend moves week
  by week (no report, then on track, at risk, delayed, blocked, back on track), its latest report
  leaves the dashboard counters unchanged, and a demo database without it gets it once on the next start
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
  lists, dashboard (rows and counters), its weekly trend and Excel export, cross-project lists scoped to the project manager's own
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
- `AuditServiceTest` — change history events: a create with its filled fields in order, an update
  with only the changed fields, no event for an update that changes nothing (a blank text counts as
  empty), a delete with the name only, the person and time from the session and the clock; record
  snapshots (report name at the year boundary, owner by name, a long text cut at 500 characters)
- `DashboardServiceTest` — portfolio aggregation, latest-report selection, risk and multi-value
  filters, year and week reference, delay filter and counter, empty portfolio; tracking counters
  only from the listed projects
- `CtoProjectTrackerBackendApplicationTests` — does the Spring context load

Frontend tests (215, Vitest + Testing Library on jsdom; component tests never reach the real backend,
`src/test/mockApi.ts` stands in for it and `src/test/fixtures.ts` holds the shared test data):

- `client.test.ts` — file download: the file and its suggested name (UTF-8 name first, a fallback
  when there is none), the token sent and any type accepted, errors and 401 handled like JSON
  requests; query string building (empty values skipped, multi-value parameters joined
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
  filters sent to the API, reference text for year / week, clearing filters, empty result; the
  risk and action cards link to the filtered list, a row's open records link to the project's
  records, and the note that a year / week does not change these counters; **Excel'e aktar** sends
  the address filters and saves the file, a failed download shows a notification
- `TrendSection.test.tsx` — only the project filters and the year / week reach the trend request
  (report filters do not), the reference text, the table view with each week's numbers
- `format.test.ts` — Turkish-insensitive search, date and week formatting, date and time in Turkey's
  time zone (also across midnight UTC), id parsing from the address
- `ProjectHistorySection.test.tsx` — the change history: action badge, record, person and time in
  Turkey; changed fields with their form labels and Turkish values (old value struck through, "yeni
  değer" for screen readers), a create with new values only, a delete with no fields, a cleared field
  as "boş", the empty state, the record type filter sent to the API, "Daha fazla göster" appending
  the next page
- `historyFormat.test.ts` — field names to form labels (type-specific first, unknown names kept),
  values to labels by record type (status) and field, ISO dates to `dd.mm.yyyy`
- `ProjectListPage.test.tsx` — list with detail links, search typed or taken from the address
- `ProjectDetailPage.test.tsx` — details and reports newest first, the change history at the end, delete disabled when there are
  reports or risk / action records, delete and return to the list (no request for the deleted project), 409 shown in the
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
- `TrackingItemsSection.test.tsx` — a project's records with type, overdue badge, decision status
  and closing date; adding a risk (request body), switching to an action drops the risk level, the
  rules checked before sending (risk level, assignee, follow-up date, a past date), a closed record
  without assignee or date, an overdue record edited with its old date kept, a backend field error
  under the input, delete, the empty state; the CTO sees no write buttons, a project manager does
- `tracking/filters.test.ts` — the list state in the address: defaults left out, invalid values
  dropped, round trip; "not closed / closed / all" turned into the backend's status filter
- `TrackingItemListPage.test.tsx` — open records requested by default, address filters sent to the
  API ("all" sends no status), changing the scope returns to page 1, empty result
- `ReportListPage.test.tsx` — links to project and report, address state sent to the API, week
  range disabled without a year, earlier weeks disabled as the end of the range, sorting returns
  to page 1, empty result

## Project status

**Done:** projects, weekly reports and work items (create, edit, delete, with their business
rules); risks, blockers, decisions and actions with assignees, follow-up dates and overdue
tracking; the CTO dashboard with a 12-week trend and an Excel export, and the filtered, paged
report, work item and risk / action lists; a change history of every write, shown on the project
page; login with JWT and
role-based permissions in the API, mirrored in the UI; the whole stack (PostgreSQL, backend,
frontend behind nginx) in Docker Compose; CI on every push.

### Latest test run

Run on 2026-11-10 against the current `main`:

| Check | Command | Result |
|---|---|---|
| Backend tests | `cd backend && ./mvnw test` | 227 passed, 0 failed, 0 skipped |
| Frontend tests | `cd frontend && npm test` | 215 passed in 29 files |
| Frontend lint | `npm run lint` | no findings |
| Frontend build | `npm run build` | builds; main chunk 326 kB (100 kB gzip), the trend charts a separate 422 kB chunk (120 kB gzip) loaded with the dashboard's charts |
| CI | GitHub Actions on push to `main` | backend, frontend and Docker image jobs passed |
| End-to-end | The demo scenarios in a browser on a clean Docker setup (local Playwright script) | report flow (14 checks), risks and actions flow (12 checks), the API scenario (14 checks) the trend and Excel flow (6 checks: tooltip, week-31 numbers, one owner's projects changing week by week, filtered download and its content, a project manager's own data, no sideways scroll on a phone) and the change history flow (9 checks: a change in a dialog shown without reloading, the person taken from the token whatever the body says, refused changes leaving no trace, history writes refused even for the Admin, another project's history 404 like a missing one, HTML in a title shown as text, paging, a deleted record's trace and the type filter, no sideways scroll on a phone) passed, no console errors |

The [Tests](#tests) section lists what each test class covers.

### Performance

Measured on 2026-11-10 with the Docker setup on a laptop, after loading 200 extra projects with a
report for every week of 2026 (10,404 reports, 31,206 work items, 1,005 risk and action records),
logged in as the CTO (whole portfolio). Each endpoint was called 3 times to warm up, then 5 times;
the SQL count is the statements PostgreSQL logged for one call.

| Endpoint | Median | SQL statements |
|---|---|---|
| `GET /api/dashboard/summary` | 8–20 ms (was 65 ms) | 7 (was 562) |
| `GET /api/dashboard/summary?year=2026&weekNumber=20` | 6–12 ms | 7 |
| `GET /api/dashboard/trend` (12 weeks) | 50 ms | 5 |
| `GET /api/dashboard/trend?weeks=52` | 48 ms | 5 |
| `GET /api/dashboard/export` (Excel, 37 kB) | 80–100 ms (was 119 ms) | 12 (was 567) |
| `GET /api/reports` (first page) | 8–10 ms | 43 |
| `GET /api/work-items` (first page) | 180–290 ms | 41 |
| `GET /api/tracking-items` (first page) | 3 ms | 11 |

- The dashboard summary used to run a few queries **per project** (562 for 200 projects). On a
  laptop that is fast, but with the database on another server every query adds a network round
  trip (1–2 ms), which would have made the dashboard and the Excel export take 0.5–1 s. It now
  loads each project's evaluated report in one query (PostgreSQL `DISTINCT ON`; a correlated
  `MAX` subquery took 46 ms here, this takes 9 ms) and their work items in another, so the number
  of queries no longer grows with the number of projects. The trend was written this way from the
  start.
- The paged lists still look up each row's project and report separately (about 2 queries per
  row, at most 20 rows per page), and the work item list's "evaluated report" condition is a
  subquery per row, which makes it the slowest call here. Both are bounded by the page size and
  are left for later.
- Timings on a laptop vary between runs (the same unchanged call measured 181 and 288 ms); the SQL
  counts do not.

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
- **Change history starts with Flyway V6:** changes made before it, and the demo data, have no
  history. The weekly trend still counts open risks and actions from their creation and closing
  dates; it does not yet use the history to show how risk levels moved.
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
- **Turkish labels exist twice:** the screens use `frontend/src/lib/labels.ts`, the Excel export
  `backend/.../export/ExportLabels.java`. A new enum value fails to compile on both sides until it
  gets a label, but a renamed label has to be changed in both places.
- **The ISO week rule exists twice,** in the backend and in `frontend/src/lib/isoWeek.ts`. Both are
  tested against the same table of dates, but a change on one side must be repeated on the other.
- **Not deployed yet:** it runs only locally in Docker, without HTTPS.

### Next phase

1. **User management for the Admin:** creating users, assigning roles and owners, resetting
   passwords; a password change for every user. After that, the app no longer depends on demo data.
2. **Production readiness:** a profile without demo data that refuses to start without a real
   `JWT_SECRET`, HTTPS, a deployment target, and token refresh or revocation. A database role that
   can only add to and read the change history, and a retention period for it.
3. **Quality:** browser end-to-end tests of the demo scenarios in CI (today they run from a local
   Playwright script), the per-row queries of the paged lists (see [Performance](#performance)), Turkish letters in the demo
   data, and a longer timeout for the slow form tests.
