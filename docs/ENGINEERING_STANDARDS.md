# Engineering Standards

## Code Organization

**Layered architecture** is mandatory:

```
com.authplatform
├── controller      → HTTP endpoints only (no business logic)
├── service         → Business logic, validation, exceptions
├── repository      → Spring Data JPA interfaces (data access)
├── model           → JPA entities (domain model)
├── dto             → Request/response POJOs
├── security        → JWT, authentication filters
├── config          → Spring configuration (SecurityConfig, etc.)
└── exception       → Custom exceptions, global error handler
```

**No violations**:
- Controllers never call repository directly; always via service
- Services never return JPA entities in DTOs; always map to DTOs
- Business logic never lives in controllers
- Repository layer never contains business logic

## Code Quality Rules

- **Small methods**: max ~15 lines; split complex logic
- **Avoid abstractions**: no interfaces unless needed (don't pre-abstract)
- **Comments**: minimal; only explain WHY if non-obvious (not WHAT)
- **No placeholder code**: no TODOs, FIXMEs, or pseudo-code in committed code
- **Clear naming**: self-documenting variable/method names
- **No duplication**: extract 3+ similar lines into shared method
- **DTOs for HTTP**: never expose `User` entity in responses; always use `AuthResponse`

## Testing Standards

### Unit Tests
- Test business logic in isolation
- Mock external dependencies (repository, utils)
- Min 80% coverage for service/controller layers
- File: `src/test/java/.../service/AuthServiceTest.java`

### Integration Tests
- Test full request→response cycle
- Use `@SpringBootTest` with embedded H2
- Test happy path + error paths (401, 409, 400)
- File: `src/test/java/.../controller/AuthControllerIntegrationTest.java`

### Security Tests
- Verify BCrypt hashing (never plaintext)
- Verify JWT validation (invalid token → 401)
- Verify unauthorized paths require auth
- Verify error responses don't leak secrets

### Test Requirements
- All tests must pass before PR merge
- Run with: `mvn test`
- New features must include tests (test-first preferred)
- Integration tests use H2 in-memory, no external DB needed

## Commits & PRs

- **Commit message**: concise, describe WHY (not WHAT)
- **One feature per PR** unless dependent
- **Update docs**: if behavior changes, update README, API_CONTRACT, or relevant docs
- **Tests included**: no PRs without passing tests
- **CI must pass**: GitHub Actions (`.github/workflows/ci.yml`) runs on every push and PR; all checks must pass before merge
- **No secrets**: never commit API keys, passwords, or signed keys
- **Linear history**: prefer rebase over merge commits

## CI/CD Pipeline

**GitHub Actions** (`ci.yml`) automatically:
- Runs on every push to main and feature branches
- Runs on every pull request
- Compiles: `mvn clean compile`
- Tests: `mvn test` (must all pass)
- Uploads coverage reports as artifacts

**Required gate**: CI must pass (green check) before PR can be merged. Red CI means the change is not acceptable and must be fixed.

**Coverage reports**: Available in GitHub Actions artifacts for each run (view in PR or Actions tab).

## Security Standards

### Secrets Management
- JWT secret: environment variable `APP_JWT_SECRET`, min 32 chars
- Never hardcode secrets in code or properties files
- Never log passwords or tokens

### Password Handling
- Always hash with BCrypt (Spring Security bean provided)
- Never store plaintext passwords
- Never return password in responses or logs
- Field naming: use `password_hash` in DB, `password` in DTO

### Error Responses
- Error messages must not expose internal details (no stack traces to client)
- Validation errors (400) can be more specific (e.g., "Email is required")
- Auth errors (401) must not reveal whether email exists or password is wrong
- Use GlobalExceptionHandler to standardize all error responses

### Token Security
- JWT tokens: HTTPS only in production (enforced by reverse proxy)
- Token expiry: default 1 hour, configurable via `app.jwt.expiration-ms`
- No revocation in Phase 1; tokens valid until expiry
- Token storage (client-side): bearer tokens in Authorization header, not cookies

### Database Security
- No sensitive data in logs (passwords, tokens, SSNs)
- H2 in-memory in dev; persistent DB in prod (not H2)
- SQL injection prevention: use Spring Data JPA (parameterized queries)
- Unique constraint on email (application-level uniqueness)

## Configuration Standards

### application.properties
- `app.jwt.secret`: required, min 32 chars, from environment
- `app.jwt.expiration-ms`: default 3600000 (1 hour), override in properties
- `spring.jpa.hibernate.ddl-auto=update`: for Phase 1 (dev/test only)
- `server.port=8080`: default

### Environment-Specific Config
- Local dev: `application.properties` with dummy secret (demo only)
- CI/Test: same as local (H2 in-memory)
- Production: `application-prod.properties` with secure secret injection

## Documentation Standards

- **README.md**: quick start, endpoints, auth header format
- **ARCHITECTURE.md**: system design, data flow, JWT structure
- **API_CONTRACT.md**: detailed endpoint specs, request/response, error codes
- **Test changes**: update TEST_STRATEGY.md
- **New decisions**: capture in docs/ADR/
- **Deployment changes**: update RUNBOOK.md and ENVIRONMENTS.md

## Release Process

Before cutting a release:
1. Run `/release-checklist` skill
2. All tests pass: `mvn test`
3. App runs: `mvn spring-boot:run`
4. Manual test: signup + login + protected route (if exists)
5. Verify passwords are hashed, tokens are valid
6. Verify error codes (401, 409, 400)
7. Update README if behavior changed
8. Commit everything, create git tag

## Forbidden Patterns

- No `System.out.println()` or `System.err.println()` (use logs)
- No `printStackTrace()` (use structured logging)
- No `@SuppressWarnings` without strong justification
- No package-private fields (use private or protected)
- No raw SQL; always use JPA or repositories
- No Thread.sleep() in tests (use assertj or wait utilities)
- No @Transactional on controllers
- No business logic in JPA entity constructors
