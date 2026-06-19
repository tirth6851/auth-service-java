# CLAUDE.md

**Auth Platform** — stateless JWT authentication (Spring Boot 3.2, Java 17, Phase 1 complete).

## MANDATORY: Read in this order BEFORE any code changes

1. **`CLAUDE_SESSION_START.md`** ← first (session context + reading order)
2. **`docs/MISSION_CONTROL.md`** ← governance & decision authority
3. **`docs/PRD.md`** ← product scope, goals, non-goals
4. **`docs/TRD.md`** ← technical requirements, constraints
5. **`docs/ARCHITECTURE.md`** ← system design, layers, flows
6. **`docs/ENGINEERING_STANDARDS.md`** ← coding rules, forbidden patterns
7. **`docs/API_CONTRACT.md`** ← endpoint specs, error codes
8. Then: domain-specific docs (ENVIRONMENTS, RUNBOOK, TEST_STRATEGY, ADR/)
9. Then: project status (PROJECT_PROGRESS, PROJECT_DECISIONS, PROJECT_BACKLOG)

## Non-Negotiable Rules

1. **Read required docs** before coding (see above)
2. **Controllers = HTTP only** (all logic goes in service)
3. **DTOs for all HTTP** (never expose JPA entities)
4. **Tests required** (80%+ coverage, all tests must pass)
5. **No hardcoded secrets** (environment variables only)
6. **No plaintext passwords** (BCrypt hashing required)
7. **Update docs & code together** (same commit/PR for both)
8. **Update HANDOFF.md at session end** (next session depends on it)

## Quick Start

```bash
mvn test                          # Run all tests (must pass)
mvn spring-boot:run               # Start on localhost:8080
mvn clean package -DskipTests     # Build JAR
```

Auth: `POST /auth/signup`, `POST /auth/login` → returns JWT token  
Protected routes require: `Authorization: Bearer <token>`  
H2 console (dev only): `http://localhost:8080/h2-console`

## Available Skills

| Skill | When |
|-------|------|
| `/spring-auth-feature` | Implement new auth features |
| `/java-test-first` | Plan tests before code |
| `/security-review` | Audit auth, secrets, errors |
| `/release-checklist` | Pre-merge verification |

## Session Checklist

- [ ] Read CLAUDE_SESSION_START.md first
- [ ] Read HANDOFF.md (previous session state)
- [ ] Read task-relevant docs (see MISSION_CONTROL.md decision matrix)
- [ ] Before coding: check PRD scope alignment
- [ ] Code, test, document in same change set
- [ ] Run `/security-review` if touching auth/config/errors
- [ ] **Update docs/HANDOFF.md before stopping work** ← MANDATORY

## Definition of Done

- Tests pass: `mvn test` ✓
- Coverage ≥ 80% (service layer: 85%+)
- Docs updated (API_CONTRACT.md, ARCHITECTURE.md, etc.)
- No secrets in code
- Passwords verified hashed (BCrypt)
- Error responses verified safe
- If major change: ADR created (docs/ADR/)
- HANDOFF.md updated with next steps
