# MISSION CONTROL

**Central operating policy for Claude and contributors.** This document defines how to work within auth-service-java: reading order, decision rules, escalation, task workflow, and documentation discipline.

---

## Required Reading Order (Before Any Code Changes)

1. **`CLAUDE_SESSION_START.md`** — session context, what you're working on, why
2. **`PROJECT_CONTEXT`** — business model, user personas, core concepts
3. **`PROJECT_PROGRESS`** — what's been built, what's working, known issues
4. **`PROJECT_DECISIONS`** — how decisions were made, what constraints exist
5. **`PROJECT_BACKLOG`** — prioritized work, roadmap, phase boundaries
6. **`docs/PRD.md`** — product goals, scope, non-goals, success criteria
7. **`docs/TRD.md`** — technical requirements, architecture, constraints
8. **Relevant domain doc**: architecture, standards, API, environments, test strategy (see below)

**Violation**: Starting work without reading order is a blocker. If context is stale, update project docs FIRST, then code.

---

## Decision Authority & Escalation

### Low-Risk (Claude Can Decide)

- Bug fixes in existing code (not changing API, not touching auth)
- Code formatting, comments, documentation improvements
- Adding tests (increased coverage without scope change)
- Refactoring within a layer (service logic, not architecture)

**Rule**: Fix, commit, add to PR description. Run security-review if touching auth or errors.

### Medium-Risk (Discuss Before Code)

- Adding new endpoints or changing API contract
- Changing validation rules or error responses
- Database schema changes (add columns, new tables)
- Dependency updates (new packages, major version bumps)
- Configuration changes (new properties, secrets)

**Rule**: Propose in comment or chat first. Verify against PRD + TRD. If alignment unclear, add ADR decision. Code only after confirmation.

### High-Risk (Escalate First)

- Architecture changes (layering, new patterns)
- Removing features or backward-incompatible changes
- Security model changes (JWT, password handling, auth flow)
- Database migration strategy changes
- Phase scope changes (adding Phase 2+ features early)

**Rule**: Stop. Create `docs/ADR/NNN-decision-title.md` first (draft). Justify against PRD + TRD. Confirm feasibility. Only code after approval.

---

## Code Change Workflow

### 1. Scope Check
- [ ] Read relevant domain doc (ARCHITECTURE, API_CONTRACT, ENGINEERING_STANDARDS, etc.)
- [ ] Confirm alignment with PRD (feature is in scope, not deferred to Phase 2+)
- [ ] Confirm alignment with TRD (meets technical requirements)
- [ ] Check PROJECT_BACKLOG (task is prioritized, not a distraction)

### 2. Design Check (If Medium/High Risk)
- [ ] Sketch which layers are affected (controller, service, repo, model, DTO, security, config)
- [ ] Identify docs that will need updating (API_CONTRACT, ARCHITECTURE, ENGINEERING_STANDARDS, etc.)
- [ ] Check for conflicts with existing decisions (docs/ADR/)
- [ ] Verify no secrets, no hardcoding, no anti-patterns

### 3. Implementation

**Coding rules** (non-negotiable):
- Controllers are HTTP-only; all logic in service
- DTOs for all HTTP requests/responses; never expose entities
- Passwords hashed with BCrypt; never returned or logged
- Errors in consistent JSON format; no stack traces to client
- Tests required; min 80% coverage per ENGINEERING_STANDARDS
- No TODO/FIXME/pseudo-code in committed code
- Comments only for WHY; code must be self-documenting

**Forbidden** (zero tolerance):
- Hardcoded secrets (passwords, API keys, JWT secrets)
- Plaintext passwords anywhere (storage, logs, responses)
- Raw SQL queries (use Spring Data JPA)
- Business logic in controllers
- JPA entities exposed in HTTP responses
- Skipped tests or @Ignore without justification

### 4. Testing

**Before code review:**
- [ ] All tests pass: `mvn test`
- [ ] Coverage ≥ 80%: `mvn jacoco:report`
- [ ] Manual smoke test: signup, login, invalid credentials, duplicate email
- [ ] Passwords verified hashed, tokens verified valid
- [ ] Error responses verified (no leakage, consistent format)

**If touching auth/config/errors:**
- [ ] Run `/security-review` skill (full security audit)

### 5. Documentation Update

**Update docs in same commit/PR as code change:**

- **API changed?** → Update `docs/API_CONTRACT.md` (endpoints, request/response, error codes)
- **Architecture changed?** → Update `docs/ARCHITECTURE.md` (layers, flow, design)
- **Rules changed?** → Update `docs/ENGINEERING_STANDARDS.md` (patterns, forbiddens, examples)
- **Config changed?** → Update `docs/ENVIRONMENTS.md` (new properties, profiles, secrets)
- **Test expectations changed?** → Update `docs/TEST_STRATEGY.md` (coverage, test cases)
- **New design decision?** → Create `docs/ADR/NNN-title.md` (context, decision, rationale, consequences)
- **User-facing change?** → Update `README.md` (endpoints, auth format, quick start)

**Rule**: Code without doc updates is incomplete. Docs must be refreshed in same change set.

### 6. Code Review & Merge

**Checklist before marking READY FOR REVIEW:**
- [ ] Feature is implemented and works locally
- [ ] All tests pass, coverage ≥ 80%
- [ ] Relevant docs are updated
- [ ] Commit messages are clear and descriptive
- [ ] No secrets, no anti-patterns, no forbidden code
- [ ] If touching auth/config: security-review passed

**Reviewer's job** (whether Claude or human):
- Verify adherence to ENGINEERING_STANDARDS
- Confirm API contract is respected (if changed, API_CONTRACT.md is updated)
- Check security (run `/security-review`)
- Confirm tests exist and cover behavior
- Verify docs are updated

**Merge criteria:**
- All checks pass (tests, coverage, style)
- Security review approved (if applicable)
- Docs are in sync with code
- No unresolved feedback

---

## Documentation Discipline

### When Docs Are Stale

If you observe code-doc mismatch:
1. **Note it** in the current task (don't ignore it)
2. **Update docs** before proceeding (docs are the source of truth)
3. **Commit separately** if large refactor: "docs: refresh X for current state"
4. **Flag in PR** if doc update is substantial

### Document Ownership

| Document | Owner | Update Trigger |
|----------|-------|----------------|
| CLAUDE.md | Claude (root rules) | Rarely; major workflow changes |
| PROJECT_* | Project lead | At end of session (progress, decisions, backlog) |
| docs/PRD.md | Product lead | Product scope change |
| docs/TRD.md | Architect | Technical requirement change |
| docs/ARCHITECTURE.md | Claude (during work) | Architecture changes, layer changes |
| docs/ENGINEERING_STANDARDS.md | Claude (during work) | New rules, new patterns, anti-patterns |
| docs/API_CONTRACT.md | Claude (during work) | Endpoint changes, response changes |
| docs/ENVIRONMENTS.md | Claude (during work) | Config changes, new profiles, secrets |
| docs/RUNBOOK.md | Claude (during work) | New steps, new commands, deployment changes |
| docs/TEST_STRATEGY.md | Claude (during work) | Coverage changes, test strategy shifts |
| docs/ADR/ | Claude (during work) | New major decisions |

### Document Validation

Before committing code:
```bash
# 1. No broken doc links
grep -r "docs/.*\.md" docs/ | grep -v "docs/ADR/" | awk -F: '{print $2}' | sort -u | \
  while read link; do [ ! -f "$link" ] && echo "BROKEN: $link"; done

# 2. No TODOs in critical docs
grep -r "TODO\|FIXME" docs/ARCHITECTURE.md docs/API_CONTRACT.md docs/ENGINEERING_STANDARDS.md

# 3. Verify docs/ADR/ exists if architecture changed
ls -la docs/ADR/ | wc -l  # Should be > 0
```

---

## Security & Secrets Handling

### Zero-Tolerance Rules

1. **Never hardcode secrets**: Use environment variables for JWT secret, DB password, API keys
2. **Never log secrets**: Passwords, tokens, keys must never appear in logs
3. **Never expose passwords**: Never return in API response, never store plaintext
4. **Never trust user input**: Validate all DTO fields; use BCrypt for passwords
5. **Never skip security review**: If touching auth, config, or errors, run `/security-review`

### Pre-Commit Verification

```bash
# Check for hardcoded secrets
grep -r "secret=\|password=\|key=" src/main/resources/application.properties
grep -r "password.*=\|secret.*=" src/main/java/ | grep -v ".passwordHash\|.password_hash"

# Check for plaintext passwords in responses
grep -r "password" src/main/java/com/authplatform/dto/

# Result should be zero secrets, zero plaintext passwords
```

---

## Task Workflow Template

When starting a task:

1. **Read** required docs (order above)
2. **Assess** risk level (low/medium/high → decision authority)
3. **Design** (medium/high risk → sketch in ADR draft or comment first)
4. **Implement** (code, tests, docs all together)
5. **Verify** (mvn test, security-review, docs updated)
6. **Review** (self-review against ENGINEERING_STANDARDS before marking done)
7. **Update PROJECT_PROGRESS** at end of session (what's done, what's next, blockers)

---

## Red Flags (Stop & Review)

If you encounter ANY of these, stop and ask for clarification:

- Code contradicts ENGINEERING_STANDARDS (e.g., logic in controller)
- Feature is in Phase 2+ but requested in Phase 1 (check PRD scope)
- Security concern not addressed (no BCrypt, plaintext password, hardcoded secret)
- Doc mismatch (code doesn't match API_CONTRACT or ARCHITECTURE)
- Test coverage < 80% (coverage check before commit)
- **CI is red** (GitHub Actions failed; never merge or declare done while CI fails)
- Dependency or config change without TRD alignment
- Time estimate exceeds task scope (re-scope or defer to later)

**Action**: Pause, document the flag in comments, ask for guidance. Don't force forward.

### CI Status Check

Before declaring work done or ready for merge:
```bash
# Check GitHub Actions status
# 1. Look at PR (must show green check)
# 2. Or check Actions tab for latest run
# 3. Or run locally: mvn clean verify
```

**Never** suggest merging, closing PR, or calling work "done" if CI is red. Always wait for green before declaring complete.

---

## Skills & Automation

Available skill automations (invoke when relevant):

| Skill | When to Use |
|-------|------------|
| `/spring-auth-feature` | Implementing new auth endpoints or service features |
| `/java-test-first` | Before code: plan tests, then implement |
| `/security-review` | After code: audit auth, secrets, errors, logging |
| `/release-checklist` | Before merge/release: verify tests, docs, config, security |

**Rule**: Use skills liberally. They enforce consistency and catch issues early.

---

## Escalation Examples

### Example 1: Adding a New Endpoint (Medium Risk)

**You want to**: Add `POST /auth/password-reset`

**Decision authority**: This is new scope (Phase 2), not Phase 1.

**Action**:
1. Check PROJECT_BACKLOG → "password reset" is Phase 2, not prioritized now
2. Check PRD.md → "non-goals: email verification" covers this
3. **Escalate**: Ask project lead if scope is changing
4. If yes: Create ADR, update PRD, proceed
5. If no: Defer to Phase 2 backlog

### Example 2: Fixing a Bug (Low Risk)

**You want to**: Fix error message that leaks database table names

**Decision authority**: This is a bug fix in error handling, medium-risk (error responses).

**Action**:
1. Read docs/ENGINEERING_STANDARDS.md (error handling rules)
2. Fix the error message
3. Update test to verify new message
4. Run `/security-review` (touching errors)
5. Commit with clear message
6. No ADR needed (not a decision, just a fix)

### Example 3: Refactoring Service (Low Risk)

**You want to**: Extract password validation logic into a helper method

**Decision authority**: This is refactoring within service layer (low risk).

**Action**:
1. Create new private method in AuthService
2. Extract validation logic
3. Update tests to cover new method (or rely on existing tests)
4. Verify all tests still pass
5. Commit with clear message (e.g., "refactor: extract password validation logic")
6. No doc updates needed (internal refactor)

---

## Summary

**Golden Rule**: Documentation and code are always in sync. If you change code, update docs in the same commit. If you see stale docs, refresh them before proceeding.

**Operating Principle**: Read first, code second, test third, document last. Never skip any step.

**Escalation Path**: Low-risk fixes alone → medium-risk features with discussion → high-risk decisions with ADR and approval.

**Quality Bar**: 80% tests, zero secrets, consistent errors, clear docs, zero TODOs in committed code.
