# TECH_DEBT.md



## Post-launch refactors



- **StalkTable/StalkRow dual-mount pattern:** Currently renders each stalk 
  twice in DOM (desktop table row + mobile card) with `hidden md:block` / 
  `md:hidden`. Fine for < 100 stalks per org. Refactor to single-mount when 
  we add row selection, dropdowns per row, inline editing, or virtualization.
  Options: useMediaQuery + hydration-safe fallback, react-responsive, or 
  headless table lib (@tanstack/react-table).



- **DashboardHeader "Overview" tab:** hardcoded active, doesn't navigate. 
  Fix to actually route to /dashboard and use useLocation for active state.



- **Desktop StalkRow keyboard operability:** currently click-only. Add 
  role="button", tabIndex, keyboard handler for consistency with mobile 
  card. Post-launch a11y polish.




- **Two-machine deploy:** Currently on `fly scale count 1` due to in-memory 
  session store. Add Spring Session + Upstash Redis to enable multi-machine 
  scaling. Free tier of Upstash on Fly Redis is sufficient.





- **SlugGenerator can produce slugs exceeding organizations.slug VARCHAR(60) 
  — 500 error on signup with long org names:** `generateUniqueSlug()` in 
  `OrganizationServiceImpl` slugifies the org name with no length cap before 
  inserting into `organizations.slug`, which is `VARCHAR(60)`. Signup with a 
  long enough org name throws `DataIntegrityViolationException` → 500 on 
  `/api/auth/verify` (org row insert happens during verification, not 
  signup itself). Found while writing Phase 3 PR #1 tests: org name is 
  built as `"Org " + label + " " + <UUID>` — a 20-char label alone produces 
  `4 + 20 + 1 + 36 = 61` characters, one over the 60-char column limit, and 
  the slug tracks that length. Fix: truncate the slugified base to ~55 
  characters in `generateUniqueSlug()` before appending the uniqueness 
  suffix. Not a launch blocker on its own, but a real prod bug — fires for 
  any real user who signs up with a sufficiently long org name. Fix before 
  public launch.



- **V10 leftover — rename idx_stalks_user_id to 
  idx_stalks_created_by_user_id:** V1 created 
  `idx_stalks_user_id ON stalks(user_id)`. V10 renamed the column 
  `user_id` → `created_by_user_id` but did not rename the index — Postgres 
  doesn't auto-rename indexes on `ALTER TABLE ... RENAME COLUMN`. The index 
  still exists and functions correctly (it indexes the renamed column just 
  fine), just with a misleading name. No functional impact. Fix: single 
  migration, `ALTER INDEX idx_stalks_user_id RENAME TO 
  idx_stalks_created_by_user_id;`. Priority: low — cleanup, not correctness.



- **No frontend test runner configured:** Frontend has no Vitest / Jest / 
  any test runner. Utility functions like `getApiErrorMessage` and all 
  components ship untested from the frontend side. Backend tests cover API 
  response shape, but pure-frontend logic (utilities, hooks, component 
  behavior) has no coverage. Fix: add Vitest + a minimal `vitest.config.ts` + a `test` script in `package.json`. Establish testing convention
  (co-located `*.test.ts` files vs. a `__tests__/` folder — decide at setup 
  time). Start with utility tests, expand to hooks and components 
  incrementally. Priority: medium. Not launch-blocking — ship before adding 
  a second engineer or before feature complexity grows further.



- **No health indicator for Resend email delivery:** Email sending via 
  `ResendEmailService` hits Resend's REST API directly, bypassing Spring's 
  `JavaMailSender`. Spring's built-in `MailHealthIndicator` was checking a 
  nonexistent localhost mailer and giving false DOWN signals — now 
  disabled (`management.health.mail.enabled=false`). Result: aggregate 
  `/actuator/health` no longer covers email delivery at all, so a Resend 
  outage would not surface in health checks. Fix: write a custom 
  `ResendHealthIndicator` that hits a lightweight Resend endpoint (e.g. 
  their `/domains` or account status endpoint) with a bounded timeout. 
  Register as a health indicator so aggregate health reflects real email 
  delivery capability. Priority: medium. Not launch-blocking — ship when 
  adding external service health monitoring generally (Neon connection 
  health, Cloudflare, etc.) so it's part of a coherent monitoring story, 
  not one-off.



- **Bucket4j in-memory map grows unbounded:** `Bucket4jRateLimitFilter` uses 
  a `ConcurrentHashMap<UUID, Bucket>` keyed on user ID with no eviction. 
  Every user who ever hits `POST /api/stalks` gets a permanent map entry 
  for the JVM's lifetime. At current scale (beta → early public) this is a 
  non-issue — even 100k users at ~1KB per bucket is 100MB, easily fits in 
  Fly memory. Becomes a real concern only at genuine scale. Fix: options 
  (pick when the time comes): (a) Caffeine cache with time-based eviction, 
  (b) migrate storage to Upstash Redis with TTLs (also solves the 
  horizontal-scale concern already flagged via TODO comment in the filter 
  itself), (c) both. Priority: low. Not launch-blocking — revisit when the 
  horizontal-scale TODO gets addressed.

