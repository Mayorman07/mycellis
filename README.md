# Mycellis

<p>
  <img src="docs/hero.svg" alt="Mycellis" width="100%" />
</p>

<p>
  <img src="https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white" alt="Java 21" />
  <img src="https://img.shields.io/badge/Spring_Boot-4-6DB33F?style=flat-square&logo=springboot&logoColor=white" alt="Spring Boot 4" />
  <img src="https://img.shields.io/badge/React-19-61DAFB?style=flat-square&logo=react&logoColor=black" alt="React 19" />
  <img src="https://img.shields.io/badge/TypeScript-5-3178C6?style=flat-square&logo=typescript&logoColor=white" alt="TypeScript" />
  <img src="https://img.shields.io/badge/PostgreSQL-Neon-4169E1?style=flat-square&logo=postgresql&logoColor=white" alt="PostgreSQL / Neon" />
  <img src="https://img.shields.io/badge/Fly.io-Backend-8B5CF6?style=flat-square&logo=flydotio&logoColor=white" alt="Fly.io" />
  <img src="https://img.shields.io/badge/license-proprietary-lightgrey?style=flat-square" alt="Proprietary license" />
</p>

## What it is

Mycellis watches the endpoints your product depends on and tells you the moment one stops breathing normally — health, latency, and uptime, in real time.

<p>
  <img src="docs/screenshots/dashboard.gif" alt="Mycellis dashboard" width="100%" />
</p>

## Project structure

Monorepo, two independently deployed apps:

- `backend/` — Spring Boot 4 (Java 21) REST API. Maven project, deployed to Fly.io as a Docker container.
- `frontend/` — Vite + React SPA. Deployed to Cloudflare Pages.
- `docs/` — internal notes: brand guidelines, architecture decisions, deploy-day reference.
- `grafana/`, `prometheus.yml` — local Prometheus/Grafana provisioning, wired up via `docker-compose.yml`.
- `docker-compose.yml` — local Postgres and MailHog (fake SMTP) for backend development, plus Prometheus/Grafana. Does not run the backend itself — that runs directly via Maven.

## Tech stack

**Backend:** Spring Boot 4, Java 21, PostgreSQL (Neon in production, local Docker Postgres in dev), Fly.io hosting, Resend for transactional email, Flyway migrations.

**Frontend:** Vite, React 19, Tailwind CSS 3, React Router 7, TanStack Query 5, deployed to Cloudflare Pages.

## Quick Start

### Backend

Prerequisites: JDK 21, Docker (for local Postgres/MailHog). Maven itself isn't required — the repo ships a wrapper (`mvnw` / `mvnw.cmd`).

```bash
git clone <repo-url>
cd mycellis/backend

# Start local Postgres + MailHog — the dev profile's datasource is
# hardcoded to match this compose file's credentials exactly
docker compose up -d postgres mailhog

# Run the app — dev is the default profile, no extra flags needed
./mvnw spring-boot:run
```

The API comes up on `http://localhost:8080`. The dev profile boots with **zero required environment variables** — optional ones (admin seeding, Resend) are documented in `backend/.env.example`. Spring Boot doesn't auto-load `.env` files, so export them in your shell or your IDE's run configuration if you want those optional features locally.

### Frontend

Prerequisites: Node 20 LTS or newer (no version pinned in this repo), npm (this repo commits `package-lock.json`, not a pnpm or yarn lockfile).

```bash
cd frontend
cp .env.example .env.local   # only needed to point at a non-default API URL
npm install
npm run dev
```

Dev server runs on `http://localhost:5173` and proxies `/api` and `/actuator` to `localhost:8080` (see `vite.config.ts`) — you don't need to set `VITE_API_BASE_URL` locally unless you're pointing at a deployed backend.

```bash
npm run build   # outputs to frontend/dist
npm run lint
```

## Architecture

### System overview

```mermaid
flowchart LR
    Browser["User's browser"] -->|HTTPS| Pages["Cloudflare Pages<br/>React SPA"]
    Pages -->|"api.mycellis.dev<br/>cross-subdomain cookie auth"| Backend["Fly.io backend<br/>Spring Boot"]
    Backend -->|SQL| Neon[("Neon Postgres<br/>Frankfurt")]
    Backend -->|REST API| Resend["Resend<br/>transactional email"]
    Robot["UptimeRobot"] -->|health check| Pages
```

### Pulse cycle

A stalk (monitored endpoint) gets checked on its own schedule; this is one cycle.

```mermaid
sequenceDiagram
    participant Scheduler as Backend scheduler
    participant Backend
    participant Endpoint as Monitored endpoint
    participant DB as Postgres
    participant Resend
    participant Dashboard

    Scheduler->>Backend: trigger pulse for stalk
    Backend->>Endpoint: HTTP request
    Endpoint-->>Backend: response / timeout
    Backend->>DB: record pulse result
    alt unhealthy
        Backend->>Resend: queue alert email
    end
    Dashboard->>Backend: GET /api/stalks
    Backend->>DB: read latest state
    Backend-->>Dashboard: updated stalk state
```

### Auth flow

```mermaid
sequenceDiagram
    participant User
    participant Frontend as React SPA (mycellis.dev)
    participant Backend as Fly.io backend (api.mycellis.dev)

    User->>Frontend: submits login form
    Frontend->>Backend: POST /api/auth/login
    Backend->>Backend: validate credentials, create session
    Backend-->>Frontend: Set-Cookie JSESSIONID<br/>Domain=mycellis.dev, SameSite=Lax
    Note over Frontend,Backend: cookie shared across mycellis.dev<br/>and api.mycellis.dev subdomains
    Frontend->>Backend: GET /api/me (cookie attached)
    Frontend->>Backend: GET /api/stalks (cookie attached)
```

### Backend internals

The pulse tick and alert tick are two separate, independently-scheduled loops rather than one pipeline — the code frames this as deliberate: alerting only needs a 60-second cadence at beta scale, and decoupling it from the 5-second pulse tick keeps alert evaluation from ever slowing down the check cycle. Within a single tick, the claim phase (which takes row locks) and the dispatch phase (which makes outbound HTTP calls) are split into separate transactional boundaries specifically so locks are released before any network I/O begins. `PulseEngine` leans on JDK 21 virtual threads — one per check — to get high concurrency from ordinary blocking calls rather than needing a reactive stack. There's no external message queue anywhere in this flow; "async" here means Spring's in-process event bus plus two small bounded thread pools, not a broker.

```mermaid
flowchart TD
    subgraph tick["Pulse tick — every 5s (StalkSchedulerService)"]
        Claim["StalkClaimService<br/>SELECT FOR UPDATE, reschedule + jitter<br/>(own transaction, commits first)"]
        Engine["PulseEngine<br/>one virtual thread per stalk<br/>blocking RestClient GET"]
        Claim --> Engine
    end

    Engine -->|"publishes PulseCheckedEvent<br/>(in-process, no broker)"| Listener["PulsePersistenceListener<br/>@Async pulseExecutor pool<br/>10-50 threads"]
    Listener -->|save| PulseTable[("pulses table")]
    Listener -->|"updateMetricsAndTransitionState()"| StalkTable[("stalks table<br/>health index, state")]

    subgraph alertTick["Alert tick — every 60s, independent (AlertEngine)"]
        Eval["evaluateStalk()<br/>reads recent pulses directly<br/>per active stalk"]
    end

    PulseTable -.->|reads| Eval
    Eval -->|"down 5+ min, or just recovered"| AlertTable[("alerts table<br/>suppression state")]
    Eval -->|"if alertsEnabled"| AlertEvent["StalkDownEvent /<br/>StalkRecoveryEvent"]
    AlertEvent --> NotifyListener["AlertNotificationListener<br/>@TransactionalEventListener<br/>AFTER_COMMIT"]
    NotifyListener --> EmailSvc["EmailService<br/>Resend in prod"]

    Dashboard["Dashboard<br/>GET /api/stalks"] -.->|polls| StalkTable
```

### Frontend internals

There's no client-side state library beyond TanStack Query — two query keys (`['me']`, `['dashboard']`) are the entire application state, and `useDashboardData` deliberately merges two REST resources into one query so dependent UI (a stalk's state pill and its sparkline) can never visually desync between renders. Auth gating is composed at the route level (`ProtectedRoute` → `AuthenticatedLayout` → `SuperAdminRoute`) rather than duplicated per page, and the entire API layer is one `apiFetch()` wrapper with cookie-based sessions — no tokens are ever handled in the frontend.

```mermaid
flowchart TD
    subgraph routing["App.tsx — React Router 7"]
        Public["Public routes<br/>HomePage, LoginPage, GuidePage,<br/>PublicStatusPage, ..."]
        Protected["ProtectedRoute<br/>redirects to /login on 401"]
        Layout["AuthenticatedLayout<br/>DashboardHeader + &lt;Outlet/&gt;"]
        SuperAdmin["SuperAdminRoute<br/>redirects to /dashboard<br/>if role check fails"]
        Protected --> Layout
        Layout --> Pages["DashboardPage, StalkDetailPage,<br/>Create/EditStalkPage, SettingsPage"]
        Layout --> SuperAdmin
        SuperAdmin --> SAPages["SuperAdmin*Page"]
    end

    Protected -->|useSession| Session
    Layout -->|useSession| Session
    Pages -->|"useSession + useDashboardData"| QueryLayer

    subgraph QueryLayer["lib/hooks — TanStack Query 5 is the only client state"]
        Session["useSession()<br/>queryKey ['me']"]
        Dashboard["useDashboardData()<br/>queryKey ['dashboard']<br/>stalks + pulses, one query"]
    end

    Session --> Resources
    Dashboard --> Resources

    subgraph api["lib/api — one thin wrapper file per resource"]
        Resources["stalks · pulses · me<br/>auth · status · superAdmin"]
        Client["client.ts apiFetch()<br/>credentials: 'include'"]
        Resources --> Client
    end

    Client -->|cookie session| Backend[("api.mycellis.dev")]

    Protected -.->|while loading| Loader["MycellisFlowerLoader<br/>components/shared/"]
    Pages -.->|renders| Shared["DashboardHeader, CommandPalette,<br/>StalkTable, KpiStrip, Sparkline...<br/>components/dashboard/"]
```

## Deployment

Backend (from `backend/`, after committing):

```bash
fly deploy -a mycellis-backend
```

Frontend (from `frontend/`):

```bash
npm run build && npx wrangler pages deploy dist --project-name=mycellis
```

## Environment variables

See `backend/.env.example` and `frontend/.env.example` for the full list of variables each app reads, with comments on what each one does and which are optional. Production values are set as Fly secrets (`fly secrets set ...`) and Cloudflare Pages environment variables, respectively — no real values are committed anywhere in this repo.

## Documentation Index

| Doc | What's in it |
|---|---|
| [`RUNBOOK.md`](./RUNBOOK.md) | Incident response |
| [`COPYRIGHT.md`](./COPYRIGHT.md) | Ownership and license terms |
| [`POST_LAUNCH_ROADMAP.md`](./POST_LAUNCH_ROADMAP.md) | Planned post-launch work |
| [`TECH_DEBT.md`](./TECH_DEBT.md) | Known shortcuts and their payoff conditions |
