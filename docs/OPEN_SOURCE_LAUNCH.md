# Open-Source Launch Plan

Working plan for turning `auth-service-java` into a healthy open-source project. It is derived from a
private research note on how Java security projects attract contributors. That note is not committed to
the repo, and several of its claims are flagged unverified below.

**Last updated:** 2026-11-10

## Decisions made

| Question | Decision |
|----------|----------|
| Positioning | **Self-hostable auth service**, not a library. No Maven Central publishing or adapter-module work. |
| License | **Keep MIT**, add a **DCO** (`Signed-off-by`) instead of a CLA. |
| First-response target | **3 business days** for issues, PRs, and security reports. Publish only what you can keep. |
| Tone of claims | Factual feature list and an explicit **"unaudited"** notice. No comparisons to other projects until the overlap check below is done. |

Current one-line pitch (in the README):

> A small, readable, self-hostable authentication service built with Spring Boot 3.2 and Java 17: email
> and password signup and login, short-lived JWT access tokens, rotating refresh tokens with reuse
> detection, and per-IP rate limiting, with an optional Next.js web portal.

**Not yet verified:** whether this fills a niche that Keycloak, Authelia, Ory Kratos, Spring
Authorization Server, or SuperTokens do not. Do that comparison before claiming a niche to awesome-java or
Hacker News. Until then the honest framing is "small and readable enough to understand end to end".

## Done in the repo (branch `claude/open-source-readiness`)

- `SECURITY.md`: private reporting, 3-business-day acknowledgement, scope, "unaudited" notice, known limitations
- `CONTRIBUTING.md`: newcomer section, response target, assigned-issue-before-PR rule, DCO, AI-assisted PR
  policy, contributor → triager → committer ladder
- `README.md`: rewritten against the current code (the old one claimed 37 tests, a one-field signup
  response, and rate limiting "pending merge")
- `.github/dependabot.yml`: monthly, grouped updates for Maven, npm (`web/`), Actions, Docker, Compose
- `.github/workflows/codeql.yml`: CodeQL for Java and JavaScript/TypeScript
- `.github/workflows/scorecard.yml`: OpenSSF Scorecard
- `.github/workflows/dco.yml`: checks every PR commit for `Signed-off-by`
- `.github/workflows/ci.yml`: read-only token permissions; actions pinned to commit SHAs
- PR template: DCO, AI-disclosure, and a generic security checklist; issue template config routes
  security reports and questions

None of the new workflows has run yet. Check the first runs after you push (see below).

## Your checklist on GitHub

Do these in order. Items 1 to 3 come first because the new files link to the pages they enable.

**Status (2026-11-10):** items 1–7, 9, and 11 are **done** (applied with `gh`, then verified): private
vulnerability reporting, Discussions, description and 15 topics, labels, Dependabot alerts and security
updates, secret scanning with push protection, read-only workflow permissions, first workflow runs (CI,
CodeQL, Scorecard green on `main`), and starter issues #26–#32. Still **open**: item 8 (branch protection,
deliberately not applied because it changes how you can push to `main`), item 10 (Scorecard badge and Best
Practices badge), and pinning a "Start here" discussion (item 2). The DCO check has not run on a real
contributor PR yet.

1. **Enable private vulnerability reporting.** Settings → Code security → *Private vulnerability
   reporting*. `SECURITY.md` and `ISSUE_TEMPLATE/config.yml` link to it.
2. **Enable Discussions.** Settings → General → Features. `CONTRIBUTING.md` and the issue config link to it.
   Pin a "Start here / usage questions" discussion.
3. **Set the repo description and topics** (the description is empty and there are no topics today).
   Suggested topics, 20 max, lowercase with hyphens: `java`, `spring-boot`, `spring-security`, `jwt`,
   `authentication`, `auth-service`, `refresh-token`, `rate-limiting`, `bucket4j`, `postgresql`, `flyway`,
   `docker`, `nextjs`, `self-hosted`, `openapi`.
4. **Create labels** used by the docs: `good first issue`, `help wanted`, `testing`, `documentation`,
   `observability`, `enhancement`.
5. **Turn on security features.** Dependabot alerts and security updates; secret scanning and push protection.
6. **Do not enable CodeQL "default setup".** `codeql.yml` already provides it, and running both makes the
   upload fail.
7. **Set workflow permissions to read-only.** Settings → Actions → General → Workflow permissions →
   *Read repository contents*.
8. **Protect `main`.** Require a pull request, require the `Build and Test`, `Signed-off-by check`, and
   `Analyze` checks, and block force pushes. As a solo maintainer, set required approvals to 0 for now;
   Scorecard's Code-Review check will score low until someone else reviews, which is expected.
9. **Push the branch, open the PR, and watch the first runs** of CI, DCO, CodeQL, and Scorecard. The DCO
   check applies to this PR too, so every commit on this branch must be signed off (`git commit -s`).
10. **After the first Scorecard run**, add its badge to the README
    (`https://api.scorecard.dev/projects/github.com/tirth6851/auth-service-java/badge`) and start the
    [OpenSSF Best Practices](https://www.bestpractices.dev/) badge. It is free and its baseline level can be
    pre-filled from the repo.
11. **Create the starter issues** below, labelled `good first issue` or `help wanted`.

## Repo content to decide before going public-facing

The repo is already public, but newcomers land on its file list. These files read as internal tooling or
noise to an outsider. None were removed; each is your call.

| File | Concern |
|------|---------|
| `CLAUDE.md`, `CLAUDE_SESSION_START.md`, `web/CLAUDE.md`, `web/AGENTS.md` | AI-assistant instructions; harmless, but may confuse contributors |
| `DOCUMENTATION_COMPLETE.md` | Status artifact, not documentation |
| `docs/AGENT_REWARDS.md` | Internal scorecard for AI agents; odd for newcomers |
| `docs/HANDOFF.md`, `docs/todo.md`, `docs/spec.md`, `docs/done-criteria.md` | Session logs and scratch notes |
| `docs/PROJECT_DECISIONS.md` D-009 | Says the `SecurityContext` principal is the email string. The code now uses `AuthenticatedUser`. Stale, and a good first issue |
| `.github/modernize/` | Untracked, so not in the repo today; confirm it stays ignored |

Options: move the internal ones under `docs/internal/` and link from `CLAUDE.md`, or leave them and add a
`docs/README.md` index that separates user docs from project-process docs.

## Starter issue drafts

Each draft says what to change, where, and how to know it is done. Verify the "current state" line before
posting, since the code moves.

### 1. Add a real BCrypt round-trip test: `good first issue`, `testing`

**Why:** `AuthServiceTest` stubs `PasswordEncoder` with Mockito. The integration tests exercise the real
encoder implicitly (signup then login), but no test inspects what is actually stored.
**Where:** `src/test/java/com/authplatform/service/` (new test) or `AuthControllerIntegrationTest`.
**Done when:** a test signs up via the real stack, reads the stored hash from the repository, asserts it is
a BCrypt hash (`$2a$`/`$2b$` prefix) that does not contain the plaintext, and that login still succeeds.
`mvn test` stays green.

### 2. Tune the HikariCP pool for `prod`: `good first issue`

**Where:** `src/main/resources/application-prod.properties`, `docs/ENVIRONMENTS.md`.
**Done when:** pool size, connection timeout, and max lifetime are set via documented properties with sane
defaults, and the new settings appear in the configuration docs.

### 3. Add a Bruno (or Postman) collection for the API: `good first issue`, `documentation`

**Where:** a new `docs/api-collection/` folder.
**Done when:** the collection covers signup, login, refresh, logout, and `/auth/me`; a test script saves the
access and refresh tokens to variables; and the README links to it.

### 4. Add a frontend test setup for `web/`: `help wanted`, `testing`

**Why:** the portal has no automated tests. Only manual checks exist.
**Done when:** a test runner (for example Vitest) is wired into `npm test` with a few tests for the helpers
in `web/lib/`, and CI runs it. Scope the first PR to the setup plus a handful of tests; end-to-end tests can
follow separately.

### 5. Fix the stale `SecurityContext` principal decision: `good first issue`, `documentation`

**Where:** `docs/PROJECT_DECISIONS.md` (D-009) and anywhere else that still says the principal is the email.
**Done when:** the docs describe the `AuthenticatedUser` principal and link to the relevant ADR or code. A
short superseding note is fine; do not delete history.

### 6. Verify the Docker Compose quick start on your OS: `good first issue`, `documentation`

**Why:** the README documents `cp .env.example .env && docker compose up --build`. It has not been confirmed
on every platform.
**Done when:** you follow the README on Windows, macOS, or Linux and either confirm it works or send a PR for
each step that does not. State your OS and Docker version.

### 7. Make rate limiting work correctly behind a reverse proxy: `help wanted`, `enhancement` (needs an ADR first)

**Why:** limits are keyed on `request.getRemoteAddr()`. Behind a proxy, all clients share the proxy's address.
`X-Forwarded-For` must not be trusted blindly because clients can forge it.
**Done when:** an ADR proposes trusted-proxy handling (for example Spring's forwarded-header support with an
explicit trusted-proxy setting) and, once accepted, the change ships with tests proving a forged header does
not bypass the limit. This is a design discussion, not a first issue.

Do **not** file issues for password reset, email verification, RBAC, sessions API, audit logging, or token
revocation as "ready to build". They are documented gaps and need design first.

## Launch gates

Launch announcements only after:

- [ ] Checklist items 1 to 9 above are done and the four workflows are green
- [ ] The README quick start has been followed from a clean clone on a machine other than yours
- [ ] The Postgres integration test has actually run once (`mvn -Pintegration-test verify`, needs Docker)
- [ ] The niche comparison is done and the pitch line is updated if needed
- [ ] You can reply to issues within 3 business days for the next month

A flop on a launch post costs little if the repo is already trustworthy. A security flaw found publicly on
launch day costs a lot. Hygiene first.

## Where to post (and where not to)

| Channel | Notes |
|---------|-------|
| GitHub topics, repo description | Free discovery; do it now |
| [up-for-grabs.net](https://up-for-grabs.net/) | Lists beginner-friendly projects; no stated activity threshold. Needs the starter issues first |
| OWASP Slack | Open to anyone. Ask for informal review of the design and threat model; do not pitch |
| awesome-java pull request | Unverified: criteria were read from a mirror, and the Security category and whether a *service* (not a library) fits were not checked. Read the live CONTRIBUTING first |
| Show HN | Repo link as landing page, direct title, no upvote requests, stay for hours. A hosted no-signup demo would help; note the Render free-tier cold start |
| r/java | Rules were never retrieved. Read them and message the mods first |
| r/netsec | Only a genuine technical write-up fits (for example refresh-token reuse detection), never a repo link |
| r/opensource | Reportedly requires a year of history; revisit later |
| Paid stars | Never |

## First 90 days

| When | Action |
|------|--------|
| Weeks 1–2 | Reply to every issue and PR within the target; thank and explain every rejection |
| Weeks 2–4 | If a launch post stalls, wait a few weeks, then retry with a technical write-up of a real pitfall |
| Weeks 3–6 | Add a `CONTRIBUTORS` file; offer triage access after a few merged PRs |
| Weeks 4–8 | Pitch a talk to a virtual Java User Group; check whether local JUGs and OWASP chapters are active |
| Weeks 6–12 | Open a Commonhaus discussion and confirm its current criteria (unverified) |
| Ongoing | Track median time to first response; check burnout risk monthly |

Later, not now: GSoC org application (needs 18+ months of public history, so January 2028 at the earliest),
goodfirstissue.dev (needs 10+ contributors), the GitHub Secure Open Source Fund (needs traction and
governance), and CNCF or Eclipse (premature).

## Unverified claims in the source research

Treat these as unchecked: r/opensource's rules, Hacker News's LLM-writing rule, awesome-java's current
criteria, Hacktoberfest 2026 changes, Outreachy requirements, local JUG and OWASP chapter activity,
Commonhaus admission criteria, and any first-year contributor data for comparable projects. The plan above
does not depend on them.
