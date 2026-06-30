# Contributing to auth-service-java

Thank you for your interest in contributing. This is a stateless JWT authentication REST API built with Spring Boot 3.2 and Java 17.

---

## Quick Start for Contributors

**Prerequisites:** JDK 17+, Maven 3.6+. No database setup required — H2 runs in memory automatically.

```bash
git clone https://github.com/tirth6851/auth-service-java.git
cd auth-service-java

# Set the required JWT secret (any 32+ character string works in dev)
export JWT_SECRET="dev-secret-minimum-32-characters-1234"

mvn test                 # Run all 37 tests — must stay green
mvn spring-boot:run      # Start on http://localhost:8080
```

API docs (dev only): `http://localhost:8080/swagger-ui.html`

---

## Ways to Contribute

- **Bug reports** — use the [bug report template](.github/ISSUE_TEMPLATE/bug_report.md)
- **Feature requests** — use the [feature request template](.github/ISSUE_TEMPLATE/feature_request.md)
- **Code contributions** — see the workflow below
- **Documentation** — typo fixes, clearer examples, missing runbook steps — all welcome

Good first issues are labelled `good first issue` in GitHub Issues.

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
2. Write tests first (or alongside code) — aim for 80%+ service-layer coverage
3. Run `mvn test` — all tests must pass before you open a PR
4. Update any affected docs in the same commit (API_CONTRACT.md, ARCHITECTURE.md, etc.)
5. Open a pull request using the [PR template](.github/PULL_REQUEST_TEMPLATE.md)
6. CI runs automatically — the PR cannot merge while CI is red

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

---

## Test Expectations

| Layer | Test type | Location |
|-------|-----------|----------|
| `JwtUtil` | Unit (no Spring context) | `JwtUtilTest` |
| `AuthService` | Unit (Mockito mocks) | `AuthServiceTest` |
| New services | Unit (Mockito mocks) | `<ServiceName>Test` |
| Controllers / endpoints | Integration (`@SpringBootTest` + MockMvc + H2) | `AuthControllerIntegrationTest` |

Run tests: `mvn test`  
Run with coverage: `mvn clean test jacoco:report` → open `target/site/jacoco/index.html`

---

## Proposing Architectural Changes (ADR Process)

Significant decisions (new auth strategy, third-party integrations, breaking API changes, data-model changes) need an Architecture Decision Record (ADR) before any code is written.

1. Open a GitHub Issue describing the proposal
2. Discuss and reach consensus in the issue thread
3. Create `docs/ADR/NNN-short-title.md` using the [existing ADR format](docs/ADR/001-jwt-subject-is-userid.md)
4. Include the ADR in the same PR as the code change

Existing ADRs explain decisions already made — read them before proposing changes that touch the same area.

---

## Docs Update Expectations

Every code change that affects observable behaviour must update the relevant docs in the same commit:

- New or changed endpoint → `docs/API_CONTRACT.md`
- Architecture change → `docs/ARCHITECTURE.md`
- New config property → `docs/ENVIRONMENTS.md` + `README.md` Configuration table
- New dependency or build step → `docs/RUNBOOK.md`

Docs-only PRs are welcome without any of the above — just ensure accuracy.

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

---

## Security Vulnerabilities

Please **do not** open a public GitHub Issue for security vulnerabilities. Email `tirth2093@gmail.com` with details. We aim to respond within 48 hours.

---

## License

By contributing, you agree that your contributions will be licensed under the [MIT License](LICENSE).
