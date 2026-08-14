# Post-Launch Roadmap

## Onboarding v2 (Week 3+ after launch)

The Aug 8 launch shipped with a single-page /guide. This works but
progressive disclosure would be stronger:

- Multi-screen onboarding wizard: Welcome → What's a stalk → Create form
- Inline tooltips over dashboard KPIs (first empty-state visit)
- Terminology translation layer (hover-glossary):
  - Stalk → endpoint
  - Pulse → health check
  - Breathing → operational
  - Stressed → degraded
  - Incident → detected problem
  - Ecosystem → monitored infrastructure

DO NOT build until we know from real beta users:
- Where they actually get stuck
- Which metaphors land vs confuse
- Whether the /guide sees any traffic at all (add analytics before deciding)

Product principle preserved: "stalk / breathing / ecosystem" is Mycellis's
proprietary product language. Never remove it. But provide a translation
layer for newcomers who need to map it to industry-standard terms.

## If repo goes public (unplanned, but if)

Currently private. If that ever changes, unplanned or otherwise:

1. Redact URLs containing account/project IDs from `RUNBOOK.md` — specifically the dashboard links in "First response":
   - `dashboard.uptimerobot.com/monitors/*`
   - `dash.cloudflare.com/[account-id]/*`
   - `console.neon.tech/app/projects/*`
   - `fly.io/apps/mycellis-backend`
2. Move the real URLs to a team wiki (Notion or a private GitHub wiki) instead.
3. Consider redacting `COPYRIGHT.md`'s named copyright holder if privacy is a concern.
4. Review `backend/.env.example` and `frontend/.env.example` for anything beyond generic placeholders and the product's own public domain (`mycellis.dev`) — as of this writing, neither references a private service or account name, so no action needed unless that's changed since.
