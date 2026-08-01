# Production Environment Variables

Required for deploying Mycellis backend to Fly.io.

Set via `fly secrets set VAR_NAME=value` before deploy.

## Database (from Fly Postgres)

- `DATABASE_URL` — Postgres connection string
- `DATABASE_USERNAME`
- `DATABASE_PASSWORD`

**Verify before Commit 23**: `application-prod.properties` expects these as
three separate values, but `fly postgres attach` (as of this writing) sets a
single `DATABASE_URL` secret in the standard `postgres://user:pass@host:port/db`
URI form — it does not split out separate username/password secrets. That URI
form also isn't a valid `spring.datasource.url` value as-is; Spring's
PostgreSQL driver needs a `jdbc:postgresql://host:port/db` prefix. Nothing in
this repo currently converts one to the other. Confirm the actual shape
`fly postgres attach` produces in this Fly account before deploy, and either
adjust these three properties to parse `DATABASE_URL` at boot, or set
`DATABASE_URL`/`DATABASE_USERNAME`/`DATABASE_PASSWORD` manually to match what
the properties file expects.

## App URLs

- `FRONTEND_URL` — https://mycellis.dev
- `BACKEND_URL` — https://api.mycellis.dev
- `APP_BASE_URL` — https://api.mycellis.dev (deprecated, kept for compat)

## Email (Resend)

Sign up at resend.com, verify the send.mycellis.dev sending domain, get an
API key.

**Wired as of Commit 21 as a direct REST API call** (`RestClient` POST to
`https://api.resend.com/emails`, Bearer-token auth) — not an SMTP relay.
Commit 20's original entry here assumed Resend would be used via its SMTP
endpoint (`smtp.resend.com`); that never got wired. `MAIL_HOST`/`MAIL_PORT`/
`MAIL_USERNAME`/`MAIL_PASSWORD` are no longer required in prod — they still
exist as properties (with safe inert defaults) only because Spring Boot's
own mail autoconfiguration binds `spring.mail.*` eagerly regardless of which
`EmailService` implementation is actually active.

- `MYCELIS_EMAIL_FROM` — `Mycellis <noreply@send.mycellis.dev>` (must be a
  verified sender on the Resend account)
- `RESEND_API_KEY` — `re_...` (Resend API key — **never commit this value**)

If either is blank while `mycelis.email.provider=resend` (the prod default),
the app now fails to start with a clear error naming the missing var, rather
than booting with a broken email path.

## Super Admin Seed

**Does not currently apply to production.** `InitialDataSeeder` is annotated
`@Profile({"dev", "local"})` — it is never instantiated under the `prod`
profile, so setting these as Fly secrets would have no effect as the code is
written today. This needs an explicit decision before Commit 23:

- extend the seeder's `@Profile` to include `prod` (an idempotent seeder
  running on every prod boot may or may not be the intended posture), or
- build a separate, deliberate one-time bootstrap step for the first
  production super admin (e.g. a one-off script/endpoint, not an
  every-boot seeder).

Until that decision is made, these env vars are listed here for reference
only — they are not consumed in production:

- `MYCELIS_ADMIN_EMAIL`
- `MYCELIS_ADMIN_PASSWORD`
- `MYCELIS_ADMIN_MOBILE`
- `MYCELIS_ADMIN_FIRST_NAME` — optional, defaults to "Admin" (dev profile
  currently hardcodes "Mayowa"/"Olajide" instead of using this env var with a
  default — out of scope for this audit since dev's behavior isn't allowed to
  change here, but worth cleaning up whenever the seeder's prod story is decided)
- `MYCELIS_ADMIN_LAST_NAME` — optional, defaults to "User"

## CORS

**Not implemented anywhere in the backend yet** — this is more than "hardcoded
to localhost"; there is no `@Bean CorsConfigurationSource`, no `.cors(...)` in
`SecurityConfig`, and no code anywhere that reads a CORS-related property.
Dev works today only because Vite's dev-server proxy makes frontend→backend
calls same-origin from the browser's perspective. Once the frontend is a
separate Cloudflare Pages origin (`mycellis.dev`) calling a separate Fly.io
origin (`api.mycellis.dev`), every authenticated request will fail the
browser's CORS preflight until `SecurityConfig` gains real CORS handling.

That's a `SecurityConfig` change, explicitly out of scope for this audit
commit (config-only). Tracked as a TODO for a follow-up commit before
Commit 23's actual deploy — likely Commit 23 itself, or a dedicated commit
just before it.

Once implemented, the expected value is:

- `CORS_ALLOWED_ORIGINS` — https://mycellis.dev,https://www.mycellis.dev

## Actuator

- `MANAGEMENT_PORT` — optional, defaults to 8081. `application-prod.properties`
  now binds the actuator to `127.0.0.1` on this port, separate from the public
  API port (8080) — metrics/prometheus/loggers endpoints are no longer
  reachable from outside the Fly VM at all in prod.

## Known deploy-day gotchas

- **Same-site cookie**: `application-prod.properties` uses `same-site=strict`.
  If login works locally but fails cross-subdomain (frontend at mycellis.dev,
  backend at api.mycellis.dev) with 401s despite valid credentials, that's
  almost certainly `SameSite=Strict` blocking the cookie on the cross-subdomain
  request. Consider changing to `lax`, or adding
  `server.servlet.session.cookie.domain=.mycellis.dev` to allow the cookie
  across both subdomains. Not changed now — this is a flag for deploy-day
  debugging, not a fix applied in this commit.
- **CORS is entirely unimplemented** (see above) — expect every authenticated
  frontend→backend call to fail immediately after the first deploy to separate
  origins, independently of the cookie issue above. This needs to be fixed
  before the same-site issue is even reachable in testing.
- **Super admin seeder is dev/local-only** (see above) — don't expect a
  super-admin account to exist after first prod boot without a separate
  decision and possibly a separate bootstrap mechanism.
- **Database URL shape** (see above) — confirm `fly postgres attach`'s actual
  output shape against what `application-prod.properties` expects before
  relying on it.
- **DNS propagation**: after pointing mycellis.dev to Cloudflare Pages, allow
  15 minutes to 24 hours for global propagation.
- **Fly cold starts**: with `auto_stop_machines = "stop"`, the first request
  after an idle period takes 2-5 seconds. Frontend should show a loading state
  rather than assuming an instant response.

## Fly.io deploy commands (reference)

Not for execution here — for Commit 23:

```bash
fly launch --dockerfile backend/Dockerfile --name mycellis-backend
fly postgres create --name mycellis-db
fly postgres attach mycellis-db
fly secrets set RESEND_API_KEY=re_... MYCELIS_EMAIL_FROM="Mycellis <noreply@send.mycellis.dev>"
fly secrets set FRONTEND_URL=https://mycellis.dev BACKEND_URL=https://api.mycellis.dev APP_BASE_URL=https://api.mycellis.dev
fly secrets set CORS_ALLOWED_ORIGINS=https://mycellis.dev,https://www.mycellis.dev
fly deploy
```
