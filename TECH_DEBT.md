\# TECH\_DEBT.md



\## Post-launch refactors



\- \*\*StalkTable/StalkRow dual-mount pattern:\*\* Currently renders each stalk 

&#x20; twice in DOM (desktop table row + mobile card) with `hidden md:block` / 

&#x20; `md:hidden`. Fine for < 100 stalks per org. Refactor to single-mount when 

&#x20; we add row selection, dropdowns per row, inline editing, or virtualization.

&#x20; Options: useMediaQuery + hydration-safe fallback, react-responsive, or 

&#x20; headless table lib (@tanstack/react-table).



\- \*\*DashboardHeader "Overview" tab:\*\* hardcoded active, doesn't navigate. 

&#x20; Fix to actually route to /dashboard and use useLocation for active state.



\- \*\*Desktop StalkRow keyboard operability:\*\* currently click-only. Add 

&#x20; role="button", tabIndex, keyboard handler for consistency with mobile 

&#x20; card. Post-launch a11y polish.



\- \*\*`mycelis.io` typo:\*\* Every backend error response has 

&#x20; `"type":"https://mycelis.io/errors/..."` — single L. Fix in 

&#x20; application-prod.properties or wherever the problem-detail URI base 

&#x20; is configured.



\- \*\*Two-machine deploy:\*\* Currently on `fly scale count 1` due to in-memory 

&#x20; session store. Add Spring Session + Upstash Redis to enable multi-machine 

&#x20; scaling. Free tier of Upstash on Fly Redis is sufficient.



\- \*\*Rotate admin password:\*\* olajidemayorwa@gmail.com password was 

&#x20; compromised during launch debugging session (Aug 8, 2026). Change 

&#x20; via app UI, and update MYCELIS\_ADMIN\_PASSWORD Fly secret.



\- \*\*SlugGenerator can produce slugs exceeding organizations.slug VARCHAR(60) 

&#x20; — 500 error on signup with long org names:\*\* `generateUniqueSlug()` in 

&#x20; `OrganizationServiceImpl` slugifies the org name with no length cap before 

&#x20; inserting into `organizations.slug`, which is `VARCHAR(60)`. Signup with a 

&#x20; long enough org name throws `DataIntegrityViolationException` → 500 on 

&#x20; `/api/auth/verify` (org row insert happens during verification, not 

&#x20; signup itself). Found while writing Phase 3 PR #1 tests: org name is 

&#x20; built as `"Org " + label + " " + <UUID>` — a 20-char label alone produces 

&#x20; `4 + 20 + 1 + 36 = 61` characters, one over the 60-char column limit, and 

&#x20; the slug tracks that length. Fix: truncate the slugified base to ~55 

&#x20; characters in `generateUniqueSlug()` before appending the uniqueness 

&#x20; suffix. Not a launch blocker on its own, but a real prod bug — fires for 

&#x20; any real user who signs up with a sufficiently long org name. Fix before 

&#x20; public launch.



\- \*\*V10 leftover — rename idx\_stalks\_user\_id to 

&#x20; idx\_stalks\_created\_by\_user\_id:\*\* V1 created 

&#x20; `idx_stalks_user_id ON stalks(user_id)`. V10 renamed the column 

&#x20; `user_id` → `created_by_user_id` but did not rename the index — Postgres 

&#x20; doesn't auto-rename indexes on `ALTER TABLE ... RENAME COLUMN`. The index 

&#x20; still exists and functions correctly (it indexes the renamed column just 

&#x20; fine), just with a misleading name. No functional impact. Fix: single 

&#x20; migration, `ALTER INDEX idx_stalks_user_id RENAME TO 

&#x20; idx_stalks_created_by_user_id;`. Priority: low — cleanup, not correctness.



\- \*\*No frontend test runner configured:\*\* Frontend has no Vitest / Jest / 

&#x20; any test runner. Utility functions like `getApiErrorMessage` and all 

&#x20; components ship untested from the frontend side. Backend tests cover API 

&#x20; response shape, but pure-frontend logic (utilities, hooks, component 

&#x20; behavior) has no coverage. Fix: add Vitest + a minimal `vitest.config.ts` 

&#x20; + a `test` script in `package.json`. Establish testing convention 

&#x20; (co-located `*.test.ts` files vs. a `__tests__/` folder — decide at setup 

&#x20; time). Start with utility tests, expand to hooks and components 

&#x20; incrementally. Priority: medium. Not launch-blocking — ship before adding 

&#x20; a second engineer or before feature complexity grows further.



\- \*\*No health indicator for Resend email delivery:\*\* Email sending via 

&#x20; `ResendEmailService` hits Resend's REST API directly, bypassing Spring's 

&#x20; `JavaMailSender`. Spring's built-in `MailHealthIndicator` was checking a 

&#x20; nonexistent localhost mailer and giving false DOWN signals — now 

&#x20; disabled (`management.health.mail.enabled=false`). Result: aggregate 

&#x20; `/actuator/health` no longer covers email delivery at all, so a Resend 

&#x20; outage would not surface in health checks. Fix: write a custom 

&#x20; `ResendHealthIndicator` that hits a lightweight Resend endpoint (e.g. 

&#x20; their `/domains` or account status endpoint) with a bounded timeout. 

&#x20; Register as a health indicator so aggregate health reflects real email 

&#x20; delivery capability. Priority: medium. Not launch-blocking — ship when 

&#x20; adding external service health monitoring generally (Neon connection 

&#x20; health, Cloudflare, etc.) so it's part of a coherent monitoring story, 

&#x20; not one-off.



\- \*\*Bucket4j in-memory map grows unbounded:\*\* `Bucket4jRateLimitFilter` uses 

&#x20; a `ConcurrentHashMap<UUID, Bucket>` keyed on user ID with no eviction. 

&#x20; Every user who ever hits `POST /api/stalks` gets a permanent map entry 

&#x20; for the JVM's lifetime. At current scale (beta → early public) this is a 

&#x20; non-issue — even 100k users at ~1KB per bucket is 100MB, easily fits in 

&#x20; Fly memory. Becomes a real concern only at genuine scale. Fix: options 

&#x20; (pick when the time comes): (a) Caffeine cache with time-based eviction, 

&#x20; (b) migrate storage to Upstash Redis with TTLs (also solves the 

&#x20; horizontal-scale concern already flagged via TODO comment in the filter 

&#x20; itself), (c) both. Priority: low. Not launch-blocking — revisit when the 

&#x20; horizontal-scale TODO gets addressed.

