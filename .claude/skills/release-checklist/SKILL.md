---
name: release-checklist
description: Verify docs, tests, config, and deploy readiness before release
tags: [release, quality-assurance, deployment]
---

# Release Checklist Skill

Pre-release verification that code, tests, documentation, and configuration meet quality standards.

## When to Run

- Before any merge to main
- Before creating release tag
- Before any deployment to staging or production

## Pre-Release Checklist

### Code Quality

- [ ] Project compiles: `mvn clean compile`
- [ ] All tests pass: `mvn test`
- [ ] Coverage ≥ 80%: `mvn jacoco:report` (check target/site/jacoco/)
- [ ] No compiler warnings
- [ ] No code style issues (if checkstyle configured)
- [ ] No dead code or unused imports

**Commands:**
```bash
mvn clean compile -Werror          # Fail on warnings
mvn test                            # All tests pass
mvn jacoco:report                   # Coverage report
```

### Security Review

- [ ] Run `/security-review` skill
- [ ] No hardcoded secrets in code
- [ ] No plaintext passwords in responses
- [ ] Error responses don't leak internals
- [ ] Passwords are BCrypt-hashed
- [ ] JWT tokens are valid and secure
- [ ] Logging doesn't expose secrets

### Manual Testing

- [ ] App starts without errors: `mvn spring-boot:run`
- [ ] Signup endpoint works: `POST /auth/signup` with valid email/password
- [ ] Signup returns valid JWT token
- [ ] Login endpoint works: `POST /auth/login`
- [ ] Login returns valid JWT token
- [ ] Invalid credentials return 401
- [ ] Duplicate email returns 409
- [ ] Invalid email format returns 400
- [ ] Missing required fields return 400
- [ ] Token can be decoded on jwt.io (valid structure)

**Test signup:**
```bash
curl -X POST http://localhost:8080/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"TestPass123"}'
# Should return: {"token":"...", "expiresAt":..., "email":"test@example.com"}

# Test login
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"TestPass123"}'
# Should return: {"token":"...", "expiresAt":..., "email":"test@example.com"}

# Test invalid credentials
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"WrongPassword"}'
# Should return 401: {"error":"Unauthorized","message":"Invalid email or password",...}
```

### Documentation

- [ ] README.md exists and is up-to-date
- [ ] README includes: quick start, endpoints, auth header format
- [ ] docs/ARCHITECTURE.md is current
- [ ] docs/API_CONTRACT.md matches actual endpoints
- [ ] docs/ENGINEERING_STANDARDS.md reflects current rules
- [ ] docs/TEST_STRATEGY.md describes test coverage
- [ ] docs/RUNBOOK.md has deployment steps
- [ ] docs/ENVIRONMENTS.md specifies config per environment
- [ ] docs/ADR/ contains decisions for any major changes
- [ ] CLAUDE.md is concise and links to all docs

**Check:**
```bash
# Verify all docs exist
ls -la docs/*.md
ls -la docs/ADR/

# Verify no TODOs in critical docs
grep -r "TODO\|FIXME" docs/ARCHITECTURE.md docs/API_CONTRACT.md docs/ENGINEERING_STANDARDS.md
```

### Configuration

- [ ] application.properties has default values
- [ ] application-prod.properties exists with production settings
- [ ] H2 console is disabled in prod: `spring.h2.console.enabled=false`
- [ ] JWT secret is configurable via environment: `APP_JWT_SECRET`
- [ ] Database config is environment-injectable
- [ ] No secrets committed to git

**Check:**
```bash
grep -r "secret=" src/main/resources/application.properties
# Should only show default/demo values, not real secrets

grep -r "spring.h2.console.enabled=false" src/main/resources/application-prod.properties
# Should exist and be true

cat .gitignore | grep -i "secret\|password\|key"
# Should have patterns to exclude secrets
```

### Dependencies

- [ ] pom.xml is clean (no unnecessary dependencies)
- [ ] All dependencies are at stable versions (not SNAPSHOT unless necessary)
- [ ] No security vulnerabilities: `mvn dependency:check-update`
- [ ] Dependencies are up-to-date (or pinned intentionally)

**Check:**
```bash
mvn dependency:tree | grep SNAPSHOT  # Should be empty or justified
mvn dependency:check-update          # Review available updates
```

### Git State

- [ ] Current branch is clean: `git status`
- [ ] No uncommitted changes
- [ ] Commits are on feature branch (not main)
- [ ] Commit messages are clear and descriptive
- [ ] Branch is up-to-date with main: `git log main..HEAD`

**Commands:**
```bash
git status                    # Should be clean
git log main..HEAD --oneline  # Show commits
```

### Build & Packaging

- [ ] Build creates JAR without errors: `mvn clean package`
- [ ] JAR file size is reasonable (~50-100MB)
- [ ] JAR can be run: `java -jar target/auth-platform-*.jar`
- [ ] App starts within 5 seconds

**Commands:**
```bash
mvn clean package -DskipTests  # Build without running tests
ls -lh target/auth-platform-*.jar  # Check size
java -jar target/auth-platform-*.jar &
sleep 2
curl http://localhost:8080/auth/signup  # Should get 400 (bad request)
pkill -f "java -jar"
```

### Release Notes

- [ ] Create CHANGELOG.md entry (if maintaining changelog)
- [ ] Document breaking changes (if any)
- [ ] Document new features
- [ ] Document bug fixes

**Format:**
```markdown
## v0.1.0 (2024-06-19)

### Features
- User signup with email and password
- User login with JWT token generation
- Stateless JWT authentication

### Bug Fixes
- Fixed GlobalExceptionHandler to return consistent JSON errors

### Security
- All passwords hashed with BCrypt
- JWT tokens signed with HMAC-SHA256
```

### Versioning

- [ ] Determine version number (semantic versioning: major.minor.patch)
- [ ] Update pom.xml version: `<version>0.1.0</version>`
- [ ] Create git tag: `git tag -a v0.1.0 -m "Release v0.1.0"`
- [ ] Push tag: `git push origin v0.1.0`

## Sign-Off

Before marking release as ready:

- [ ] All checklist items passed
- [ ] Code review approved (if using PR process)
- [ ] Security review approved
- [ ] Staging deployment successful (if applicable)
- [ ] No known critical issues

## Post-Release

- [ ] Push build artifacts to repository (if applicable)
- [ ] Update deployment runbooks
- [ ] Notify team of release
- [ ] Monitor logs for errors

## Rollback Plan

If issues found in production:

1. Identify issue in logs
2. Determine if rollback is needed or hotfix
3. If rollback: `java -jar auth-platform-0.0.0.jar` (previous version)
4. If hotfix: create fix, test, tag, and redeploy

## References

- [docs/RUNBOOK.md](../../docs/RUNBOOK.md) — release and deployment steps
- [docs/TEST_STRATEGY.md](../../docs/TEST_STRATEGY.md) — test requirements
- [docs/ENGINEERING_STANDARDS.md](../../docs/ENGINEERING_STANDARDS.md) — coding standards
- [docs/ARCHITECTURE.md](../../docs/ARCHITECTURE.md) — system design
