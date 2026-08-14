# Runbook

> Note: This file contains real account/project IDs in dashboard URLs. Repo is private — that's fine. If this repo ever becomes public, see POST_LAUNCH_ROADMAP.md for the redaction checklist.

## When to use this

3am, alert fires, brain not working. Follow these in order. Don't improvise.

## First response

1. Check status: `fly status -a mycellis-backend`
2. Check your dashboards:
   - UptimeRobot: https://dashboard.uptimerobot.com/monitors/803717436
   - Cloudflare Pages: https://dash.cloudflare.com/98b19a5d2cd58ac17ffbf1e35b6da70b/pages/view/mycellis
   - Neon: https://console.neon.tech/app/projects/curly-star-98427043
   - Fly.io: https://fly.io/apps/mycellis-backend

### Is the platform down?

If the symptoms point at Fly, Neon, or Cloudflare rather than the app itself, check their public status pages before doing anything else:

- Fly.io status: https://status.flyio.net/
- Neon status: https://neonstatus.com/
- Cloudflare status: https://www.cloudflarestatus.com/

If one of these shows an active incident: wait and monitor. Post to the beta WhatsApp group: "We're aware, watching upstream." Don't panic-fix someone else's infra.

## Common incidents

### Backend is 500-ing on every request

```bash
fly logs -a mycellis-backend --no-tail | Select-Object -Last 100   # PowerShell
fly logs -a mycellis-backend --no-tail | tail -100                  # bash/zsh
```

Look for the stack trace at the bottom. Common causes: DB connection lost, a required secret missing (see `backend/.env.example` for which ones have no default and will crash boot — `FRONTEND_URL`, `BACKEND_URL`, `APP_BASE_URL`), or a deploy regression.

### Backend is unreachable (UptimeRobot red, connection refused)

1. `fly status -a mycellis-backend` — is the machine down?
2. If yes: `fly machine start <machine-id> -a mycellis-backend`
3. If the machine won't start: roll back to the last good image.
4. Rollback: `fly releases -a mycellis-backend` → find the last green release → `fly deploy --image <image>`

### Login broken / CORS errors

1. `fly secrets list -a mycellis-backend` — check `CORS_ALLOWED_ORIGINS` is set and includes the origin that's failing.
2. Test the preflight manually:
   ```bash
   curl -i -X OPTIONS https://api.mycellis.dev/api/auth/login \
     -H "Origin: https://mycellis.dev" \
     -H "Access-Control-Request-Method: POST" \
     -H "Access-Control-Request-Headers: Content-Type"
   ```
   Look for `Access-Control-Allow-Origin` echoing back the `Origin` you sent. If it's missing, that origin isn't in `CORS_ALLOWED_ORIGINS`.
3. Common cause: forgot to add `www` or the apex domain to allowed origins — the default is `https://mycellis.dev` only (see `SecurityConfig.java`), comma-separate multiple origins.

### DB is corrupted / user data missing

1. Neon console → project → Branches → Restore from history.
2. Free tier: 6 hours retention. Restore to a point-in-time *before* the incident.
3. **Warning**: this creates a new branch. Update the `DATABASE_URL` Fly secret to the new branch's connection string — the backend parses username/password directly out of that single URL at boot (see `DatabaseUrlConfiguration.java`), so this one variable is normally all you need to update.
4. `fly deploy -a mycellis-backend` (or just `fly secrets set DATABASE_URL=...` — Fly restarts machines automatically on a secret change, but redeploying is the safer explicit step).

### Emails not sending (Resend)

1. Check the Resend dashboard for delivery failures.
2. `fly secrets list -a mycellis-backend` — confirm `MYCELIS_EMAIL_PROVIDER` is `resend`.
3. Confirm `RESEND_API_KEY` is set and hasn't been rotated out from under the running app.
4. The sending domain (`send.mycellis.dev`) must be verified in Resend, and `MYCELIS_EMAIL_FROM` must match a verified sender on that domain.

### Frontend won't load / white page

1. Check the Cloudflare Pages deployment status for the project.
2. Check the browser console for errors — most likely an import path issue or a missing/misconfigured `VITE_API_BASE_URL`.
3. If it's a bad deploy: `npx wrangler pages deployment list --project-name=mycellis` to find the last good deployment, then use the Cloudflare dashboard's Pages project → Deployments → select that deployment → "Rollback to this deployment."

## Killing everything (last resort)

```bash
fly scale count 0 -a mycellis-backend
```

Stops all machines. App is down, but bleeding stops. Diagnose, then:

```bash
fly scale count 1 -a mycellis-backend
```

## Rotating leaked credentials

**Admin password.** `MYCELIS_ADMIN_PASSWORD` only seeds a *brand-new* super-admin on first boot — `InitialDataSeeder` skips silently if a user with that email already exists, so setting a new Fly secret does **not** rotate an existing admin's password. To actually change it: log into the app → Settings → Change Password.

**Resend API key.** Resend dashboard → API keys → rotate → `fly secrets set RESEND_API_KEY="new"`.

**Database password.** Neon console → Roles → reset. Production reads a single `DATABASE_URL` with the password embedded in it (see the DB-corruption section above) — update that Fly secret with the new connection string rather than a separate `DATABASE_PASSWORD` secret, unless you know the deployment is set up to use the separate `DATABASE_USERNAME`/`DATABASE_PASSWORD` fallback instead.

## Escalation

If it's a Fly / Neon / Cloudflare outage, their status pages tell you. Post to the beta WhatsApp group: "We're aware, watching upstream." Don't panic-fix someone else's infra.
