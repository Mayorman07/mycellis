# Runbook

## When to use this

3am, alert fires, brain not working. Follow these in order. Don't improvise.

## First response

1. Check platform status: `fly status -a <app-name>`
2. Check your dashboards (UptimeRobot, Cloudflare Pages, Neon, Fly.io — bookmarks/private notes for account-specific URLs).

### Is the platform down?

If the symptoms point at Fly, Neon, or Cloudflare rather than the app itself, check their public status pages before doing anything else:

- Fly.io status: https://status.flyio.net/
- Neon status: https://neonstatus.com/
- Cloudflare status: https://www.cloudflarestatus.com/

If one of these shows an active incident: wait and monitor. Notify the user group: "We're aware, watching upstream." Don't panic-fix someone else's infra.

## Common incidents

### Backend is 500-ing on every request

```bash
fly logs -a <app-name> --no-tail | Select-Object -Last 100   # PowerShell
fly logs -a <app-name> --no-tail | tail -100                  # bash/zsh
```

Look for the stack trace at the bottom. Common causes: DB connection lost, a required secret missing, or a deploy regression.

### Backend is unreachable

1. `fly status -a <app-name>` — is the machine down?
2. If yes: `fly machine start <machine-id> -a <app-name>`
3. If the machine won't start: roll back to the last good image.
4. Rollback: `fly releases -a <app-name>` → find the last green release → `fly deploy --image <image>`

### Login broken / CORS errors

1. `fly secrets list -a <app-name>` — check CORS config is set and includes the origin that's failing.
2. Test the preflight manually with `curl -i -X OPTIONS`.
3. Common cause: forgot to add www or the apex domain to allowed origins.

### DB is corrupted / user data missing

1. Neon console → project → Branches → Restore from history.
2. Free tier retention is limited. Restore to a point-in-time before the incident.
3. **Warning**: this creates a new branch. Update the connection string secret to the new branch.
4. Redeploy or update the secret; Fly restarts machines automatically on secret change.

### Emails not sending

1. Check the email provider's dashboard for delivery failures.
2. Confirm relevant secrets are set and haven't been rotated out from under the running app.
3. Sending domain must be verified with the provider.

### Frontend won't load / white page

1. Check the CDN deployment status for the project.
2. Check the browser console for errors — most likely an import path issue or a missing/misconfigured API base URL.
3. If it's a bad deploy: use the CDN dashboard to rollback to the last good deployment.

## Killing everything (last resort)

```bash
fly scale count 0 -a <app-name>
```

Stops all machines. App is down, but bleeding stops. Diagnose, then:

```bash
fly scale count 1 -a <app-name>
```

## Rotating leaked credentials

**Admin password.** Depending on your seeder logic, the admin-password secret may only seed a brand-new super-admin on first boot rather than rotating an existing one. To actually change an existing admin's password, use the app's own Settings → Change Password flow.

**External API keys.** Rotate in the provider's dashboard, then update the Fly secret.

**Database password.** Reset in the database provider's console, then update the connection-string secret.

## Escalation

If it's an infra provider outage, their status pages tell you. Notify the user group: "We're aware, watching upstream." Don't panic-fix someone else's infra.