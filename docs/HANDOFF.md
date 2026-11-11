# Session Handoff

**Last updated**: 2026-11-10 (Open-source launch prep done, then **Spring Boot 3.5.16 upgrade merged via PR #37**)
**Branch**: `main` at `14aa202` (PR #37 merge `5b23bed` plus one docs commit), in sync with `origin/main`, clean working tree. No other branches matter. `Java auth open source growth.md` is the user's private research note, **untracked on purpose — never stage it**.
**Tests**: **72 backend tests pass on Boot 3.5.16** (`mvn clean test`, IntelliJ Maven + JBR 21), and the **3 Docker-gated Postgres tests now run in CI on every PR** (`Postgres Integration` job, a required check); they passed against real PostgreSQL 16 with Flyway 11.7.2 and `ddl-auto=validate` for the first time ever. Frontend: still no automated test suite (CI lint + builds it).
**Current goal**: turn the project into a healthy open-source project (`docs/OPEN_SOURCE_LAUNCH.md`). Repo files, GitHub settings, branch protection, starter issues, CI, and the Spring Boot upgrade are done. Remaining: the other Dependabot PRs, PR #14, badges, the niche comparison, and the launch itself.

---

## At a glance (2026-11-10 session, authoritative)

**Done this session (details in the sections below):** open-source launch prep (SECURITY.md, CONTRIBUTING with DCO, README rewrite, Dependabot/CodeQL/Scorecard/DCO workflows, GitHub settings, branch protection, 7 starter issues) and the **Spring Boot 3.2.5 → 3.5.16 upgrade** (PR #37).

**Files, grouped:** *source/build* `pom.xml` only. *Tests* `PostgresFlywayIntegrationTest` (+1 test). *CI/config* `.github/workflows/{ci,codeql,scorecard,dco}.yml`, `.github/dependabot.yml`, `.github/ISSUE_TEMPLATE/*`, PR template. *Web* `web/package-lock.json` (resynced for npm 10). *Docs* `SECURITY.md`, `CONTRIBUTING.md`, `README.md`, `CLAUDE.md`, `CLAUDE_SESSION_START.md`, and under `docs/`: `OPEN_SOURCE_LAUNCH.md` (new), `PROJECT_DECISIONS.md` (D-011, D-012), `PROJECT_PROGRESS.md`, `PROJECT_BACKLOG.md`, `TRD.md`, `PROJECT_CONTEXT.md`, `HANDOFF.md`. No application Java code changed.

**Tests:** last local run `mvn clean test` = 72 passed, 0 failed (IntelliJ JBR 21 + bundled Maven). Nothing in `src/`, `pom.xml`, `web/`, or `.github/` changed after that run. The 3 Postgres Testcontainers tests run in CI only (no Docker locally) and passed. CI is green on `main`.

**Key decisions:** self-hostable service, not a library (the code is a runnable service + portal); keep MIT and add a DCO (clearer provenance, no CLA admin); 3-business-day response target (one a solo maintainer can keep); no comparative claims until the niche check is done; Boot 3.5 (not 4) with Boot-managed Testcontainers; dependency PRs are not merged without discussion (`MISSION_CONTROL.md`); branch protection keeps admin bypass.

**Known gaps (do NOT invent):** no password reset, email verification, sessions/devices API, RBAC, audit logging, token revocation, or shared (Redis) rate limiting; portal has no runtime tests; rate limiting keys on `getRemoteAddr()` (all clients share a bucket behind a proxy).

**Open risks / deferred:** remaining Dependabot PRs (#36, #23, #33, #34) and PR #14 (unexplained) await the user; Docker Compose quick start and Dockerfile build image never exercised on Boot 3.5; Render services never provisioned; redundant `git stash` (`stash@{0}`) still present, the user can drop it; Scorecard/Best Practices badges, niche comparison (Keycloak, Authelia, Ory Kratos, Spring Authorization Server, SuperTokens), and launch posts not done.

**Exact next step:** ask the user what PR #14 is, then decide on the Dependabot PRs. After that: badges, niche comparison, launch.

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

### Third follow-up: Spring Boot upgrade (user said "do the spring boot upgrade") — merged as PR #37, `5b23bed`
- **Versions:** Boot 3.2.5 → **3.5.16**, JJWT 0.12.5 → **0.13.0**, springdoc 2.5.0 → **2.10.0**, Flyway 9.22.3 → **11.7.2**, Spring Security 6.2.4 → 6.5.11, Hibernate 6.4.4 → 6.6.53, H2 2.2 → 2.3. Testcontainers now Boot-managed 1.21.4 (the pinned 1.19.7 and its BOM import were removed; **2.0 deliberately not taken**, it renames artifacts). Java stays 17. Decision record: **D-012**. Supersedes Dependabot #35 (closed with an explanation).
- **The silent-break risk that was found and fixed:** Flyway 10+ ships PostgreSQL support in a separate module. Added `flyway-database-postgresql`; without it `prod` migrations fail at startup, and the H2 suite (Flyway off) cannot see it.
- **Verification:** `mvn clean test` 72/72 with `spring-boot-properties-migrator` temporarily enabled (no renamed properties; removed before commit); `dependency:tree` confirmed resolved versions; the packaged jar was run in the `dev` profile and the full API flow checked by hand (signup, `/auth/me`, missing and tampered token, duplicate signup, wrong password, refresh rotation, reuse detection and family revocation, logout, validation, Swagger UI, 404 vs 401); CI log confirmed Flyway migrated V1–V3 on Postgres 16 and 3/3 integration tests passed.
- **New/changed tests and CI:** `PostgresFlywayIntegrationTest` gained a refresh-rotation and reuse-detection test (it exercises the `@Version` column, timestamptz, and the bulk revoke query on real Postgres). New CI job `Postgres Integration` (`mvn -B -Pintegration-test verify`), added to the required checks after its first green run. The **DCO check passed on a real PR for the first time**.
- **Known leftovers (not caused by the upgrade):** `Bandwidth.classic` / `Refill` in `RateLimitInterceptor` are deprecated in Bucket4j 8.10; the Dockerfile's `maven:3.8-openjdk-17` build image was never exercised on 3.5 (unchanged); the README's "3 Postgres tests" is now accurate (it was 2 before; `@Testcontainers` had inflated an earlier grep).
- Repo description updated to say Spring Boot 3.5; version references updated in README, CONTRIBUTING, CLAUDE.md, CLAUDE_SESSION_START.md, TRD, PROJECT_CONTEXT, dependabot comment, launch doc. `docs/spec.md` is the original Phase 1 spec and was intentionally left as history.

### Exact next step
Ask the user to decide on the remaining open Dependabot PRs (#36 Actions group of 4 updates, #23 npm minor/patch, #33/#34 lockfile-only transitive patches), and about PR #14. Dependabot will now re-propose the other Maven updates (for example Bucket4j) against the new baseline. Then the Scorecard and Best Practices badges, the niche comparison, and the launch itself (`docs/OPEN_SOURCE_LAUNCH.md`).

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

> Continue the Java Auth Service project (open-source launch). Work from `main` (`14aa202` or later); there
> is no long-lived feature branch. This is the authoritative starting state; do not re-derive it.
>
> **Read first, in order:** `CLAUDE.md`, this `docs/HANDOFF.md`'s top section (everything below the
> "Third follow-up" section's "Exact next step" is history), `docs/OPEN_SOURCE_LAUNCH.md`, and
> `docs/PROJECT_DECISIONS.md` D-011 (open-source terms) and D-012 (Spring Boot upgrade).
>
> **State (done, do NOT redo):** Backend is on **Spring Boot 3.5.16 / Java 17**, JJWT 0.13.0, springdoc
> 2.10.0, Flyway 11.7.2 (+ `flyway-database-postgresql`), Testcontainers 1.21.4 (Boot-managed). 72 backend
> tests pass (`mvn clean test`; use IntelliJ's JBR 21 + bundled Maven, NOT system JDK 24), and 3 Postgres
> Testcontainers tests pass in CI. Open-source repo files, `SECURITY.md`, DCO, Dependabot (major bumps
> ignored), CodeQL, Scorecard, GitHub settings, labels, starter issues #26-#32, and branch protection on
> `main` are all done. CI jobs (all required checks): `Build and Test`, `Postgres Integration`, `Web Build`,
> both CodeQL `Analyze` jobs, `Signed-off-by check`. Branch protection has `enforce_admins=false`, so the
> owner can still push directly; prefer PRs for code changes. Use `git commit -s` on every commit.
>
> **Do NOT:** stage or move `Java auth open source growth.md` (the user's private research, untracked on
> purpose); remove internal-looking files (`docs/AGENT_REWARDS.md`, `DOCUMENTATION_COMPLETE.md`,
> `CLAUDE*.md`) without asking; make comparative or niche claims about other auth projects (the overlap check
> is not done); invent password reset, email verification, RBAC, sessions API, audit logging, or token
> revocation; merge dependency PRs without discussion (`MISSION_CONTROL.md`); touch PR #14 (scoped API keys,
> unexplained) without asking.
>
> **Known caveats:** rate limiting keys on `getRemoteAddr()`, so behind a reverse proxy all clients share one
> bucket (documented; starter issue #32 needs an ADR); `Bandwidth.classic`/`Refill` are deprecated in Bucket4j
> 8.10; the Docker Compose quick start and the Dockerfile build image were never exercised on Boot 3.5; the
> web portal has no runtime tests (CI only lints and builds it); the Render services were never provisioned.
>
> **What to do next:** (1) Ask the user about the remaining open Dependabot PRs: #36 (GitHub Actions group, 4
> updates), #23 (13 npm minor/patch bumps), #33/#34 (lockfile-only patches), and PR #14. Dependabot will
> re-propose other Maven updates (for example Bucket4j) against the new baseline. (2) Remaining launch work:
> Scorecard and Best Practices badges, the niche comparison, then the launch posts. Run `/handoff` before
> stopping.
