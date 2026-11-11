# Session Handoff

**Last updated**: 2026-11-10 (Open-source launch prep — repo files, GitHub settings, and starter issues done; pushed directly to `main` at the user's request, no PR)
**Branch**: work was done on `claude/open-source-readiness` (cut from `origin/main` `07d288f`, the PR #15 merge) and fast-forward pushed to `origin/main`. `Java auth open source growth.md` is the user's private research note, **untracked on purpose — never stage it** (deliberately left out of the push even though the user said "push all").
**Tests**: **72 backend tests passing (0 failures)**, re-run this session via IntelliJ Maven + JBR 21 (`mvn -B test`). The 3 Docker-gated Postgres tests are excluded and have still never run anywhere. No application code changed this session. Frontend: still no automated suite.
**Current goal**: turn the project into a healthy open-source project, following `docs/OPEN_SOURCE_LAUNCH.md`. Repo files, GitHub settings, and starter issues are done; remaining work is branch protection, badges, deciding on Dependabot PRs and PR #14, the niche comparison, and the launch itself.

---

## What Was Completed This Session (2026-11-10 — open-source launch prep)

Resumed from the 2026-07-25 handoff. **Drift found:** the handoff's next step ("open the PR") was stale — PR #15 had already been merged, and the committed `HANDOFF.md` was an older version than the working-tree rewrite.

**Decisions (user-confirmed, recorded as D-011):** position as a **self-hostable auth service** (not a library, so no Maven Central or adapter modules); **keep MIT + add DCO**; **3-business-day** response target; scope = **repo files only** (no GitHub settings changed, no posting, nothing pushed).

**Added or changed:**

| Area | Files |
|------|-------|
| Trust | `SECURITY.md` (new): private reporting via GitHub advisories, email fallback, 3-business-day ack, scope, "unaudited" notice, known limitations |
| Contribution | `CONTRIBUTING.md` (rewritten: newcomer section, assigned-issue-before-PR rule, DCO, AI-assisted PR policy, contributor→triager→committer ladder); `.github/PULL_REQUEST_TEMPLATE.md` (DCO, AI disclosure, generic security checklist); `.github/ISSUE_TEMPLATE/config.yml` (new), `bug_report.md` |
| README | `README.md` rewritten against current code (old one claimed 37 tests, one-field signup response, rate limiting "pending merge", and a hardcoded secret default) |
| Automation | `.github/dependabot.yml`; `.github/workflows/codeql.yml`, `scorecard.yml`, `dco.yml` (all new); `ci.yml` (read-only permissions, SHA-pinned actions). SHAs were resolved through the GitHub API, staying on the major versions CI already used |
| Docs | `docs/OPEN_SOURCE_LAUNCH.md` (new: GitHub checklist, 7 starter-issue drafts, launch gates, channels); `PROJECT_DECISIONS.md` (D-011), `PROJECT_BACKLOG.md`, `PROJECT_PROGRESS.md` |

**Verified:** all YAML files parse (`actionlint` not installed). README test counts, endpoints, DTO shapes, config properties, and reuse-detection behavior were checked against source. Starter-issue "current state" claims were grepped (no Hikari tuning, no API collection, no web tests; BCrypt only mocked in unit tests).

**Not verified:** the four workflows have **never run** (CodeQL `build-mode: none`, Scorecard `publish_results`, and the DCO check are untested until pushed). The Docker Compose quick start was not run (no Docker daemon).

**Finding (documented, not fixed):** rate limiting keys on `request.getRemoteAddr()` and no forwarded-header handling exists, so behind a reverse proxy all clients share one bucket. Recorded in README and `SECURITY.md`; starter issue 7 proposes an ADR.

### Open items / owner actions
- Review the diff, then commit (**with `git commit -s`**, or the new DCO check fails this branch's own PR), push, open a PR, and watch the four workflows
- Do the GitHub settings checklist in `docs/OPEN_SOURCE_LAUNCH.md` (private vulnerability reporting and Discussions first, since new files link to them)
- Decide what to do with internal-looking files (`docs/AGENT_REWARDS.md`, `DOCUMENTATION_COMPLETE.md`, `CLAUDE*.md`); none were removed
- `docs/PROJECT_DECISIONS.md` D-009 is stale (principal is `AuthenticatedUser`, not email); intentionally left as starter issue 5
- Do the niche comparison (Keycloak, Authelia, Ory Kratos, Spring Authorization Server, SuperTokens) before any public pitch
- Still open from before: run `mvn -Pintegration-test verify` once with Docker; Render services never provisioned; `git stash drop` of the redundant stash

### Post-push results (checked 2026-11-10)
- Push to `main` (`96c07d7`): **CI, CodeQL, and OpenSSF Scorecard all succeeded.** The DCO check has not been exercised on a real contributor PR (it was correctly *skipped* for Dependabot PRs).
- Dependabot accepted its config and immediately opened **10 PRs** (#16–#25), a one-time first-run flood. Two Maven major bumps fail CI as expected (springdoc 2.5→3.2, Testcontainers 1.19→2.0). Several others are risky major bumps that should probably be closed or ignored: Docker base images to Temurin 25 / Postgres 18 / `maven:3.8-openjdk-18` (the project targets Java 17), `eslint` 10, `iron-session` 9. Consider adding `ignore` rules for major versions to `.github/dependabot.yml`. None were merged or closed.
- **Unknown to the earlier handoffs:** open PR #14, "feat: Phase A — agent client access via scoped API keys" (`claude/repo-audit-status-pmqj4k`). It conflicts with the "do not invent API keys" gap list unless that was an intentional separate effort; ask the user before touching it.

### Follow-up done later the same day (user said "just do it")
- **Dependabot:** added `ignore` rules for major-version bumps (maven, npm, docker, docker-compose; GitHub Actions majors still allowed) in `ebd6637`. Dependabot auto-closed 6 major-bump PRs (#16, #18, #21, #22, #24, #25) and regrouped. I closed #17 (Maven build image to `openjdk-18`, which Dependabot did not classify as major) with an explanatory comment.
- **GitHub settings applied via `gh`, verified:** description, 15 topics, Discussions, private vulnerability reporting, Dependabot security updates, secret scanning + push protection, read-only default workflow permissions. Created labels `testing`, `observability` (a duplicate `docs` label was created then deleted; the repo's existing `documentation` label is used).
- **Starter issues created:** #26 BCrypt stored-hash test, #27 HikariCP tuning, #28 Bruno/Postman collection, #29 stale D-009 doc, #30 verify Docker Compose quick start, #31 frontend test setup, #32 rate limiting behind a proxy (needs ADR).
- **Not touched:** PR #14 (scoped API keys), still unexplained.

### Second follow-up (user said "upto u")
- **CI `Web Build` job added** (Node 22: `npm ci`, lint, build, with placeholder `SESSION_SECRET`/`AUTH_API_URL`; a clean build fails without them because the modules throw at load, which also matters for any deploy build). Its first run failed and exposed a real bug: `web/package-lock.json` was generated with npm 11 and lacked optional `@emnapi/*` / tailwind-oxide wasm entries, so `npm ci` failed on npm 10 (Node 22). Fixed by regenerating with npm 10 (verified: zero existing package versions changed; npm 10 and 11 `ci` both pass). Both CI jobs are green on `main` (`d2f1784`).
- **Branch protection applied to `main`:** PR required (0 approvals), required checks `Build and Test`, `Web Build`, `Analyze (java-kotlin)`, `Analyze (javascript-typescript)`, `Signed-off-by check`; no force pushes or deletions; `enforce_admins=false`, so the owner can still push directly (this handoff's own push tested that).
- **Dependabot PRs deliberately NOT merged**, per `MISSION_CONTROL.md` (dependency updates are "discuss before code"): #35 is **Spring Boot 3.2.5→3.5.16, JJWT 0.12.5→0.13.0, springdoc 2.5→2.10** (backend tests pass, but it is a security-sensitive upgrade that deserves a deliberate, reviewed change; 3.2 is out of support, so it is worth doing); #23 (13 npm minor/patch bumps, lint and build pass, no runtime tests); #33/#34 (lockfile-only transitive patches for `systeminformation` and `undici`; stale, will conflict after #23); #19 (GitHub Actions majors checkout v7 / setup-java v6 / upload-artifact v7; stale, Dependabot will recreate).

### Exact next step
Ask the user to decide on the Dependabot PRs: the Spring Boot / JJWT upgrade (#35) is the most important, and should be done on purpose with a security review. Then PR #14, the Scorecard and Best Practices badges, the niche comparison, and the launch itself (see `docs/OPEN_SOURCE_LAUNCH.md`).

---

## What Was Completed This Session (2026-07-25 — backlog sweep via 4 parallel agents) ⚠️ historical; its "open the PR" next step is superseded (PR #15 merged)

Prior session's handoff (see superseded section below) listed 5 open items. All were dispatched to isolated-worktree agents in parallel, merged back onto `claude/auth-portal-m1`, and re-verified:

1. **Rate-limit `POST /auth/signup`** — extended the existing Bucket4j mechanism (generalized `LoginRateLimitInterceptor` → `RateLimitInterceptor`, parametrized), wired a **separate bucket per endpoint** (10 attempts/10min/IP each) via `WebConfig.java` so exhausting login's limit doesn't block signup or vice versa (proven with an explicit independence test). +4 integration tests.
2. **`LOWER(email)` functional index** — investigated rather than blindly adding a migration: the index already existed in `V1__create_users_table.sql` (`idx_users_email_lower`), and repository lookups intentionally stay exact-match because `AuthService` normalizes email (`.trim().toLowerCase()`) before every lookup (see `docs/PROJECT_DECISIONS.md` D-008). Corrected 3 docs (`HANDOFF.md`, `PROJECT_BACKLOG.md`, `PROJECT_DECISIONS.md`) that had stale "not yet added" claims. No schema/code change needed.
3. **PostgreSQL Testcontainers integration test** — added `PostgresFlywayIntegrationTest` (spins up real `postgres:16-alpine`, runs all Flyway migrations, then a signup→login smoke test through the real stack). Tagged `@Tag("integration")`, gated behind a new `integration-test` Maven profile (`mvn -Pintegration-test verify`) so it does **not** run in default `mvn test`/CI — no Docker daemon was available in this session to actually execute it, only compile it. **Needs a one-time run on a Docker-enabled machine/CI before it can be trusted.**
4. **`render.yaml` Render Blueprint** — added at repo root implementing `docs/DEPLOYMENT.md`'s documented two-service deploy (Postgres database + Docker backend service + Node portal service). Deliberately left `JWT_SECRET`, `SESSION_SECRET`, and the full JDBC URL as `sync: false` manual placeholders rather than fabricating values Render's blueprint spec can't actually auto-derive; flagged in a `docs/DEPLOYMENT.md` callout.
5. **Stale stash cleanup** — verified `stash@{0}` ("pre-merge HANDOFF.md") was fully superseded by the already-committed `HANDOFF.md` rewrite; the actual `git stash drop` was blocked by the session's auto-mode classifier (destructive-op guard) — **still present, not yet dropped**, safe for the user to drop manually (`git stash drop`).

Two of the four worktree agents initially branched from a stale base (missing the refresh-token hardening + Portal M1 commits — caught via a 46-vs-68 test count mismatch), were synced to the branch tip before merging, and re-verified. See `docs/AGENT_REWARDS.md` for a per-agent scorecard of this sweep.

### Files created/modified this session
| Area | Files |
|------|-------|
| Source (signup rate limit) | `config/RateLimitInterceptor.java` (renamed from `LoginRateLimitInterceptor.java`), `config/WebConfig.java`, `exception/RateLimitExceededException.java`, `exception/GlobalExceptionHandler.java`, `application.properties` (main + test) |
| Tests | `controller/AuthControllerIntegrationTest.java` (+4 signup rate-limit tests), `integration/PostgresFlywayIntegrationTest.java` (new, Docker-gated) |
| Build | `pom.xml` (Testcontainers BOM + deps, `integration-test` profile, `excludedGroups` for default surefire) |
| Deploy | `render.yaml` (new) |
| Docs | `docs/API_CONTRACT.md`, `docs/PROJECT_BACKLOG.md`, `docs/PROJECT_DECISIONS.md`, `docs/DEPLOYMENT.md`, `docs/ADR/007-rate-limiting-strategy.md` (addendum), `docs/AGENT_REWARDS.md` (new) |

### Tests run and results
`mvn -B test` (JAVA_HOME = IntelliJ JBR 21) on the fully merged branch: **72 tests, 0 failures, 0 errors, BUILD SUCCESS.** The new `PostgresFlywayIntegrationTest` is excluded from this run by design (`excludedGroups=integration`) — not yet executed against real Docker/Postgres in this environment.

### Key decisions
- **Separate rate-limit buckets per endpoint** (not shared) — so a user hammering login doesn't get accidentally locked out of signup.
- **No new migration for the email index** — it already existed; avoided adding a duplicate/failing `CREATE INDEX` on Postgres.
- **Testcontainers test gated behind a Maven profile, not default `mvn test`** — keeps the fast H2 suite fast and CI-safe when no Docker is available; explicit opt-in via `-Pintegration-test`.
- **`render.yaml` leaves secrets as `sync:false`** — never fabricate/hardcode `JWT_SECRET`/`SESSION_SECRET`/JDBC URL; Render's blueprint spec genuinely can't auto-derive the JDBC form or concatenate `https://` onto a bare hostname.
- **GitHub PR open/merge deliberately NOT done** — visible action on shared state, left for explicit user confirmation.

### Known constraints / backend gaps (do NOT invent these)
- No password reset, no email verification (`isVerified` unused), no sessions/devices API, no RBAC.
- Frontend (`web/`) still has no automated test suite — manual curl verification only (from an earlier session).
- The new Testcontainers test has not actually been executed yet (no Docker daemon in this session) — don't assume it's proven, just compiled/merged correctly.

### Open risks / TODOs
- **Run `mvn -Pintegration-test verify` on a Docker-enabled machine/CI at least once** to confirm the new Postgres integration test actually passes for real (not just compiles).
- **GitHub PR still not opened/merged** — user was asked, hadn't answered as of this handoff.
- **Stray `git stash` entry** ("pre-merge HANDOFF.md") still exists — confirmed redundant, blocked from auto-dropping by the classifier; user can `git stash drop` it themselves.
- **Render services still not provisioned** — `render.yaml` blueprint now exists as a faster path, but nobody has actually deployed via Render dashboard yet.
- Older Phase-2-candidate backlog items remain open and untouched this session: audit logging, token revocation/denylist, Redis-backed shared rate limiting for horizontal scaling.

### Exact next step
Ask the user whether to run `gh pr create` (and eventually `gh pr merge`) for `claude/auth-portal-m1` → `main`. If yes, run it and report the PR URL. Separately, if the user has Docker available, run `mvn -Pintegration-test verify` once to validate the new Postgres integration test for real.

---

## What Was Completed This Session (2026-07-25 — commit, push, merge, Render switch) ⚠️ superseded by the section above

- **Committed the Auth Portal M1 frontend** (`web/`, ~41 files) as `c3b4dca` — previously built and manually verified in the prior session but left uncommitted. Verified `.env.local` and `node_modules` were correctly excluded via `web/.gitignore` before staging.
- **Committed `docs/DEPLOYMENT.md`** separately, then **rewrote its two-service deploy section from Railway to Render** (`3433f36`) — the user reported they're unable to deploy on Railway. New section documents: a Render Postgres instance (internal vs external URL), the backend as a Docker-based Web Service (`/actuator/health` as the Render health check path, env vars pulled from the Postgres "Connect" tab), and the portal as a Node-based Web Service (root dir `web`, `AUTH_API_URL`/`SESSION_SECRET` env vars). Notes the free-tier cold-start behavior (15 min idle → slow first request) and that CORS is irrelevant to the portal itself (same-origin BFF; only matters for non-portal browser clients hitting the API directly).
- **Pushed the branch** (`git push --set-upstream origin claude/auth-portal-m1` — it had no upstream yet).
- **Resolved a full merge with `origin/main`** (merge commit `924ffdf`), which had independently progressed 16+ commits and had done its *own* separate merge of the same rate-limit-login + auth-me feature set already present in this branch's lineage (via `a5fe5bb`). This produced 6 real textual conflicts, all resolved:
  - **`SecurityConfig.java`** — kept the explicit `permitAll()` path list (not a wildcard) plus the `// /auth/me intentionally NOT permitAll` comment from `origin/main`.
  - **`AuthController.java`** — kept THIS branch's `/auth/me` implementation (`AuthenticatedUser` principal → `authService.getCurrentUser(principal.userId())`), not `origin/main`'s independent merge, which had regressed to the buggy pre-refresh-tokens `authentication.getName()` lookup (see prior session's note — that bug was already caught and fixed once; `origin/main`'s parallel merge had reintroduced it). Verified against `AuthService.getCurrentUser(Long userId)` and `MeResponse`'s actual shape before deciding which side to keep.
  - **`docs/API_CONTRACT.md`** — merged wording from both sides; kept the fuller `origin/main` "not rate-limited" endpoint list.
  - **`docs/PROJECT_BACKLOG.md`** — dropped a duplicate "COMPLETE" block that both sides had independently added.
  - **`docs/PROJECT_PROGRESS.md`** — merged the "Latest Metrics" table into one accurate version (see that file's current top).
  - **`docs/HANDOFF.md`** — conflicted; resolved by taking `origin/main`'s version wholesale rather than hand-merging two divergent session logs (this rewrite replaces that placeholder now).
  - `docs/ADR/005...` → `007-rate-limiting-strategy.md` rename and `docs/agent-native-roadmap.md` addition auto-merged cleanly from `origin/main`, no action needed.
- **Full backend suite re-verified after the merge: 68/68 passing.** Committed and pushed the merge commit.

### Files created/modified this session
| Area | Files |
|------|-------|
| Frontend (new, committed) | Entire `web/` tree — `c3b4dca` |
| Docs (committed) | `docs/DEPLOYMENT.md` (Railway → Render rewrite, two commits: initial add, then the Render swap `3433f36`) |
| Merge (committed) | `SecurityConfig.java`, `AuthController.java`, `docs/API_CONTRACT.md`, `docs/PROJECT_BACKLOG.md`, `docs/PROJECT_PROGRESS.md`, `docs/HANDOFF.md` (this file) — merge commit `924ffdf` |

### Key decisions
- **Render over Railway** — user could not deploy to Railway; no further diagnosis was requested, just a platform switch. `docs/DEPLOYMENT.md`'s two-service section is now Render-specific end to end.
- **Kept this branch's `/auth/me` over `origin/main`'s parallel merge** — verified via the actual service/DTO signatures rather than assuming either side was correct, since both branches had independently merged the same logical feature set and one copy was stale/buggy.
- **Took `origin/main`'s `HANDOFF.md` wholesale during the merge** rather than hand-splicing two session logs — this rewrite is what restores this file to being authoritative again.

### Known constraints / backend gaps (do NOT invent these)
- No password reset, no email verification (`isVerified` unused), no sessions/devices API, no RBAC.
- Frontend (`web/`) has no automated test suite — manual curl verification only (from the prior session; not repeated this session since no portal code changed).
- No Testcontainers/PostgreSQL-backed test — H2 only.
- `/auth/me` exists but the portal intentionally doesn't call it (decodes its own server-held access token instead, per `docs/PORTAL_MILESTONE_1.md` §D/§G).

### Open risks / TODOs
- **Render services have not actually been provisioned.** `docs/DEPLOYMENT.md` documents the steps (Postgres instance → backend Web Service → portal Web Service), but no Render MCP/CLI/dashboard access exists in this session — the user needs to do this themselves, or ask for a `render.yaml` blueprint to speed it up (offered, not yet answered).
- **The PR itself has not been opened/merged on GitHub** — only pushed; `gh pr create` was not run. The user said they were "unable to merge," which the branch merge above should have fixed, but they still need to actually merge the PR on GitHub (or ask for `gh pr create`/`gh pr merge` to be run).
- A stray `git stash` entry (`pre-merge HANDOFF.md`, containing this session's own draft handoff notes from before the merge) was superseded by this rewrite and can be dropped (`git stash drop`) — holding off on dropping it automatically since stash operations are treated as reversible-but-worth-flagging; ask if it should stay.

### Exact next step
Either (a) generate a `render.yaml` blueprint so the user can one-click-deploy both services on Render, or (b) if they'd rather do it manually, walk through `docs/DEPLOYMENT.md`'s "Render: backend + Auth Portal" section with them step by step. Separately, confirm whether to open/merge the GitHub PR via `gh pr create`.

---

<!-- Sessions below are historical; the 2026-06-30 session's own "What Was Completed" narrative is preserved as-is. -->

## What Was Completed This Session (2026-06-30 — merge consolidation, superseded by 2026-07-25 above)

Several prior sessions had left valuable, tested work stranded in unmerged branches, and the project docs (`HANDOFF.md`, `PROJECT_PROGRESS.md`, `PROJECT_BACKLOG.md`) had drifted out of sync with what was actually merged. This session reconciled all of it:

### 1. Reviewed and merged PR #10 (`claude/refresh-tokens` → `main`)
- Refresh tokens (`POST /auth/refresh`), logout (`POST /auth/logout`), CORS, `/actuator/health` exposed publicly, OpenAPI/Swagger UI, contributor docs (`CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`, issue/PR templates), `docs/DEPLOYMENT.md`, `docs/OBSERVABILITY.md`
- Code review confirmed: thin controllers, service-layer logic, SHA-256 hashed refresh tokens (justified for high-entropy UUIDs in ADR-005), CORS configured with explicit origins (not wildcard+credentials), Swagger disabled in prod, no merge conflicts with `main`
- CI green (40/40 tests) before merge; verified again locally after merge

### 2. Closed stale PR #5
- Its only commit (401-instead-of-403 fix) was already merged independently via PR #6 (`8140b4e`). Closed with an explanatory comment — no code change needed.

### 3. Discovered, reviewed, and merged `claude/rate-limit-login` (no PR existed)
- This branch stacked on top of `claude/auth-me` (itself unmerged), so merging it brought in **both**:
  - `GET /auth/me` — returns `id`, `email`, `verified`, `createdAt` for the authenticated user
  - `POST /auth/login` rate limiting — Bucket4j, 10 attempts / 10 min / IP, 429 + `Retry-After` header
- This branch was cut before PR #10's refresh-token work merged, so merging it into the post-PR#10 `main` produced real conflicts. Resolved by hand:
  - **`SecurityConfig.java`**: the two branches' `permitAll()` lists conflicted (`/auth/**` wildcard vs. explicit signup/login). Resolved to an **explicit path list** (`/auth/signup`, `/auth/login`, `/auth/refresh`, `/auth/logout`, health/swagger paths) so `/auth/me` stays **protected** — a wildcard would have accidentally made it public. Verified against the branch's own tests (`authMe_returns401_whenNoToken`).
  - **`AuthController.java`**: combined all 5 endpoints (signup, login, refresh, logout, me); added an `@Operation`/`@ApiResponses` block to `/me` to match the OpenAPI documentation pattern already used on the other 4 endpoints.
  - **`AuthService.java`**: merged cleanly via git (no manual resolution needed) — `getCurrentUser()` and the refresh-token methods coexist correctly.
  - **`application.properties`** (main + test): combined refresh-token TTL, CORS, Jackson, and rate-limit settings — all four were additive, no real conflict.
  - **`docs/ADR/005-rate-limiting-strategy.md`** renumbered to **`007`** — it collided with the refresh-token design's ADR-005.
  - **Docs** (`API_CONTRACT.md`, `PROJECT_BACKLOG.md`, `PROJECT_PROGRESS.md`): rewritten to reflect the merged, accurate end state rather than line-by-line conflict resolution.

### 4. Refreshed stale project docs
`PROJECT_BACKLOG.md` still listed PostgreSQL/Docker/CI/CD as "High Priority — not done" despite being merged weeks earlier via PR #9. Rewrote the "Recently Completed" section to reflect true current state and removed completed items from "High Priority."

---

## Outstanding Risks or Blockers

- ⚠️ **Concurrent refresh race condition** — documented in ADR-005 (refresh tokens); acceptable for current scale, `@Version` optimistic locking is the future mitigation
- ⚠️ **`/auth/signup` not yet rate-limited** — only `/auth/login` is protected
- ⚠️ **Rate limiting is per-instance, not shared** — a horizontally-scaled deployment would need a Redis-backed Bucket4j extension for shared-state enforcement
- ⚠️ **Access token TTL still 1 hour in default config** — production should set `app.jwt.expiration-ms=900000` (15 min)
- ⚠️ **PostgreSQL Testcontainers** not yet in place — `V2` migration is untested against real Postgres locally

---

## Tests Run & Results

Final merged total: **46 tests, 0 failures** (`mvn clean test` — BUILD SUCCESS).

```
Tests run:  5, Failures: 0, Errors: 0 — AuthServiceTest
Tests run: 33, Failures: 0, Errors: 0 — AuthControllerIntegrationTest
Tests run:  8, Failures: 0, Errors: 0 — JwtUtilTest
─────────────────────────────────────────────
Tests run: 46, Failures: 0, Errors: 0 — BUILD SUCCESS
```

---

## Exact Next Steps

### Priority 1: Verify CI is green on `main`
```bash
# Check GitHub Actions tab, or:
mvn clean verify
```

### Priority 2: Production hardening (Medium Priority backlog)
- Set `app.jwt.expiration-ms=900000` (15 min) in prod env
- Configure `app.cors.allowed-origins` to the actual frontend domain before going live

### Priority 3: Next Phase 2 feature candidates (see `PROJECT_BACKLOG.md` — Medium Priority)
- Rate limiting on `/auth/signup`
- Audit logging (structured auth events)
- Token revocation / denylist (`jti` claim + short-TTL store)
- PostgreSQL Testcontainers for `V2` migration coverage

### Standard process
1. Read `CLAUDE.md` + this `HANDOFF.md` first
2. Run `mvn test` — confirm BUILD SUCCESS before making changes
3. For any auth/config change: run `/security-review`
4. Update `HANDOFF.md` before ending session

---

## Summary

This session's job was reconciliation, not new features: merged PR #10 (refresh tokens, CORS, OpenAPI, contributor docs) after code review, closed a stale superseded PR, discovered and merged an orphaned `claude/rate-limit-login` branch (which also carried `/auth/me`) that had no PR open, hand-resolved the resulting conflicts in `SecurityConfig`/`AuthController`/properties/docs, and refreshed `PROJECT_BACKLOG.md`/`PROJECT_PROGRESS.md` to match reality. `main` now has Phase 1 + PostgreSQL/Docker/CI + security hardening + refresh tokens + CORS + OpenAPI + `/auth/me` + login rate limiting, all merged and tested.

---

## 🔖 New Session Prompt (ready to paste)

> Continue the Java Auth Service project, open-source launch prep. The work is on `main` (pushed directly
> from `claude/open-source-readiness`, which was cut from `07d288f`). PR #15 is already merged, so do NOT
> open a PR for `claude/auth-portal-m1`.
> This is the authoritative starting state; do not re-derive it.
>
> **Read first, in order:** `CLAUDE.md`, this `docs/HANDOFF.md`'s top section ("2026-11-10 — open-source
> launch prep"; everything below it is historical), `docs/OPEN_SOURCE_LAUNCH.md`, and `docs/PROJECT_DECISIONS.md`
> D-011.
>
> **State:** Repo-file work is done and **committed on `main`**: `SECURITY.md`, rewritten `CONTRIBUTING.md` and
> `README.md`, Dependabot, CodeQL, Scorecard and DCO workflows, `ci.yml` hardening, PR/issue template
> changes, `docs/OPEN_SOURCE_LAUNCH.md`, plus D-011 and backlog/progress updates. 72 backend tests pass
> (`mvn -B test`, IntelliJ JBR 21 + bundled Maven, NOT system JDK 24). No application code changed.
> Decisions made with the user: self-hostable service positioning, MIT + DCO, 3-business-day response
> target, repo files only (no GitHub settings changed, nothing posted or pushed).
>
> **Do NOT:** stage or move `Java auth open source growth.md` (the user's private research, untracked on
> purpose); remove the internal-looking files without asking; make comparative or niche claims about other
> auth projects (the overlap check is not done); invent password reset, email verification, RBAC, sessions
> API, audit logging, or token revocation; push, open PRs, change GitHub settings, or post anywhere without
> explicit confirmation.
>
> **Caveats to keep in mind:** the four new workflows have never run (verify after push); the Docker
> Compose quick start and the Postgres Testcontainers test have never been executed; rate limiting keys on
> `getRemoteAddr()`, so behind a reverse proxy all clients share one bucket (documented, not fixed).
>
> **What to do next:** GitHub settings, labels, starter issues #26–#32, branch protection on `main`, and a
> CI `Web Build` job are all done; CI, CodeQL, and Scorecard are green on `main`. (1) Ask the user to decide on
> the open Dependabot PRs, deliberately NOT merged: #35 is a Spring Boot 3.2.5→3.5.16 + JJWT 0.12.5→0.13.0
> upgrade that needs a reviewed, security-conscious change; #23 (npm minor/patch), #33/#34 (stale lockfile
> patches), #19 (stale Actions majors). Also ask about the unexplained open PR #14 (scoped API keys). The DCO
> check is still untested on a real contributor PR. (2) Remaining launch work: Scorecard and Best Practices
> badges, the niche comparison, then the launch posts (`docs/OPEN_SOURCE_LAUNCH.md`). Use `git commit -s` for
> all future commits. Run `/handoff` before stopping.
