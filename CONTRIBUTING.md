# Contributing to auth-service-java

Thank you for your interest in contributing. This is a small, self-hostable authentication service
(Spring Boot 3.2, Java 17) with a Next.js web portal. Contributions of every size are welcome, including
typo fixes and doc improvements.

---

## New here? Start here

1. **Pick an issue labelled [`good first issue`](https://github.com/tirth6851/auth-service-java/labels/good%20first%20issue).**
   Each one says what to change, where, and how to know it is done.
2. **Comment to claim it.** Ask to be assigned before you start, and wait for the assignment. This stops
   two people doing the same work. Unclaimed PRs for issues that someone else holds may be closed.
3. **Get it running** with the quick start below. `mvn test` should pass before you change anything.
4. **Ask questions early** in the issue thread, or in
   [Discussions](https://github.com/tirth6851/auth-service-java/discussions) for general usage questions.
   A half-finished draft PR is fine.
5. **Open a PR** from a fork. You will get a first response within the target below.

Not sure where to begin? A docs fix, a missing test, or a clearer error message are all good first
contributions, and you do not need an assigned issue for a docs typo.

### Response time

I aim to give **every issue and pull request a first human response within 3 business days.** This is a
one-person project, so it is a target, not a guarantee. If you have heard nothing after a week, a polite
ping in the thread is welcome.

---

## Quick Start for Contributors

**Prerequisites:** JDK 17+, Maven 3.8+. No database setup required, because H2 runs in memory automatically.

```bash
git clone https://github.com/<your-username>/auth-service-java.git
cd auth-service-java

# Any 32+ character string works in dev
export JWT_SECRET="dev-secret-minimum-32-characters-1234"

mvn test                 # unit + integration tests, must stay green
mvn spring-boot:run      # start on http://localhost:8080
```

API docs (dev only): `http://localhost:8080/swagger-ui.html`

The Docker-gated Postgres test is excluded from `mvn test`. Run it with `mvn -Pintegration-test verify`
if you have Docker available.

The portal lives in `web/` and has its own setup in [`web/README.md`](web/README.md).

---

## Ways to Contribute

- **Bug reports:** use the [bug report template](.github/ISSUE_TEMPLATE/bug_report.md)
- **Feature requests:** use the [feature request template](.github/ISSUE_TEMPLATE/feature_request.md)
- **Code:** see the workflow below
- **Documentation:** typo fixes, clearer examples, missing runbook steps
- **Tests:** several gaps are listed as `good first issue`s
- **Security reports:** never as a public issue; see [SECURITY.md](SECURITY.md)

---

## Developer Certificate of Origin (DCO)

Instead of a CLA, this project uses the [Developer Certificate of Origin](https://developercertificate.org/).
By signing off a commit you confirm that you wrote it, or otherwise have the right to submit it under the
project's [MIT license](LICENSE).

Add a sign-off with `-s`:

```bash
git commit -s -m "fix: return 401 for expired refresh tokens"
```

This appends a line to the commit message:

```
Signed-off-by: Your Name <you@example.com>
```

Use your real name and an email address you control. A CI check requires every commit in a PR to carry
this line. If you forgot, fix it with:

```bash
git rebase --signoff main      # signs off every commit on your branch
git push --force-with-lease
```

---

## Branch Naming

```
fix/<short-description>          # bug fix
feat/<short-description>         # new feature
docs/<short-description>         # documentation only
refactor/<short-description>     # refactor, no behaviour change
```

Base all branches from `main`.

---

## Code Change Workflow

1. Fork the repository and create your branch from `main`
2. Write tests first (or alongside code); aim for 80%+ service-layer coverage
3. Run `mvn test`; all tests must pass before you open a PR
4. Update any affected docs in the same commit (`API_CONTRACT.md`, `ARCHITECTURE.md`, etc.)
5. Sign off every commit (`git commit -s`)
6. Open a pull request using the [PR template](.github/PULL_REQUEST_TEMPLATE.md)
7. CI runs automatically; the PR cannot merge while CI is red

Keep PRs small and focused on one change. A reviewable 50-line PR merges faster than a 500-line one.

---

## AI-assisted contributions

Using AI tools is fine. You remain responsible for what you submit:

- You must understand every line, be able to explain it in review, and have run the tests yourself.
- Do not submit generated code you have not read, and do not paste unverified claims about the code
  into issues or PR descriptions.
- Say so in the PR description if a substantial part was AI-generated.
- Because this is security-sensitive code, generated changes to authentication, token handling, or
  error responses get the same (or stricter) scrutiny as any other.

PRs that look like unreviewed bulk output may be closed without detailed feedback.

---

## Coding Rules (Non-Negotiable)

| Rule | Rationale |
|------|-----------|
| **Controllers are HTTP-only** | All business logic lives in `AuthService` or a dedicated service class. Controllers call the service and return `ResponseEntity`. |
| **DTOs for all HTTP payloads** | Never expose JPA entities in request or response bodies. Add new DTOs in `com.authplatform.dto`. |
| **No hardcoded secrets** | Use environment variables. `JWT_SECRET` must never appear as a literal value in any committed file. |
| **BCrypt for passwords** | `PasswordEncoder` (BCrypt) is injected wherever password hashing is needed. Never store or log plaintext passwords. |
| **Tests required** | New service methods need unit tests; new endpoints need integration tests. |
| **No comments explaining what the code does** | Code should be self-explanatory. Add a comment only when the *why* is non-obvious: a subtle invariant, a security constraint, a workaround for a known bug. |

More detail: [`docs/ENGINEERING_STANDARDS.md`](docs/ENGINEERING_STANDARDS.md).

---

## Test Expectations

| Layer | Test type | Location |
|-------|-----------|----------|
| `JwtUtil`, `JwtAuthenticationFilter` | Unit (no Spring context) | `JwtUtilTest`, `JwtAuthenticationFilterTest` |
| `AuthService`, `RefreshTokenService` | Unit (Mockito mocks) | `AuthServiceTest`, `RefreshTokenServiceTest` |
| New services | Unit (Mockito mocks) | `<ServiceName>Test` |
| Controllers / endpoints | Integration (`@SpringBootTest` + MockMvc + H2) | `AuthControllerIntegrationTest` |
| Database migrations on PostgreSQL | Integration (Testcontainers, needs Docker) | `PostgresFlywayIntegrationTest` |

Run tests: `mvn test`
Run with coverage: `mvn clean test jacoco:report`, then open `target/site/jacoco/index.html`

---

## Proposing Architectural Changes (ADR Process)

Significant decisions (new auth strategy, third-party integrations, breaking API changes, data-model
changes) need an Architecture Decision Record (ADR) before any code is written.

1. Open a GitHub Issue describing the proposal
2. Discuss and reach consensus in the issue thread
3. Create `docs/ADR/NNN-short-title.md` using the [existing ADR format](docs/ADR/001-jwt-subject-is-userid.md)
4. Include the ADR in the same PR as the code change

Existing ADRs explain decisions already made. Read them before proposing changes that touch the same area.

---

## Docs Update Expectations

Every code change that affects observable behaviour must update the relevant docs in the same commit:

- New or changed endpoint: `docs/API_CONTRACT.md`
- Architecture change: `docs/ARCHITECTURE.md`
- New config property: `docs/ENVIRONMENTS.md` and the `README.md` configuration table
- New dependency or build step: `docs/RUNBOOK.md`

Docs-only PRs are welcome without any of the above; just make sure they are accurate.

---

## Commit Message Style

```
feat: add rate limiting to /auth/signup
fix: return 401 instead of 403 for unauthenticated requests
docs: update API_CONTRACT with /auth/me endpoint
refactor: extract token hashing into helper method
```

- Imperative mood, present tense
- Under 72 characters on the subject line
- Reference an issue number if one exists: `fix: ... (closes #42)`
- End with the DCO sign-off (`git commit -s` adds it)

---

## How contributors grow into maintainers

| Role | How you get there | What you can do |
|------|-------------------|-----------------|
| **Contributor** | Have a PR merged | Everything above |
| **Triager** | A few merged PRs and a pattern of helpful issue replies; just ask | Label and close issues, ask for repro steps |
| **Committer** | Sustained, high-quality review and contributions | Review and merge PRs |

This ladder is new and untested. If you are interested in triaging or reviewing, say so in an issue or
discussion and we will work it out.

---

## Security Vulnerabilities

Please **do not** open a public issue. Follow [SECURITY.md](SECURITY.md) to report privately.

---

## Code of Conduct

Everyone taking part is expected to follow the [Code of Conduct](CODE_OF_CONDUCT.md).

---

## License

By contributing, you agree that your contributions will be licensed under the [MIT License](LICENSE),
and you certify the [Developer Certificate of Origin](https://developercertificate.org/) with your sign-off.
