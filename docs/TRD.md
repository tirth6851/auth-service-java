# Technical Requirements Document (TRD)

## Overview

This document formalizes the technical requirements that satisfy the Product Requirements Document (PRD). It specifies how the authentication system must be built, including architecture, security controls, persistence, API design, and operational requirements.

## System Requirements

### Functional Requirements

1. **User Signup**
   - Accept email and password via HTTP POST
   - Validate email format and uniqueness
   - Hash password with BCrypt (cost factor ≥ 10)
   - Create user record in persistent storage
   - Return JWT token with 1-hour expiry

2. **User Login**
   - Accept email and password via HTTP POST
   - Retrieve user by email
   - Compare submitted password with hashed password (BCrypt.matches)
   - Return JWT token if match
   - Return 401 if mismatch

3. **Token Validation (Protected Routes)**
   - Extract bearer token from `Authorization` header
   - Validate token signature (HS256)
   - Validate token expiry
   - Extract user ID from token subject
   - Allow request if valid; return 401 if invalid

4. **Stateless Authentication**
   - No server-side session storage
   - Token is self-contained (subject + claims)
   - Any server instance can validate any token
   - Token revocation deferred to Phase 2 (refresh tokens)

### Non-Functional Requirements

| Requirement | Target | Rationale |
|-------------|--------|-----------|
| Availability | 99.5% uptime | SaaS baseline |
| Response time | < 200ms p95 | User experience |
| Throughput | 1000 req/sec minimum | Expected load |
| Data consistency | ACID | User data integrity |
| Security | OWASP Top 10 compliance | Industry standard |
| Scalability | Horizontal (stateless) | Multi-region future |

---

## Architecture Requirements

### Layered Design

**Mandatory layers:**
1. HTTP Controller layer (request parsing, routing)
2. Service layer (business logic, validation, exceptions)
3. Repository layer (data access, Spring Data JPA)
4. Domain model (JPA entities)
5. DTO layer (request/response objects)
6. Security layer (JWT, authentication filters)

**Rule**: Controllers delegate all logic to services; services never expose entities.

### Technology Stack

| Component | Technology | Requirement |
|-----------|-----------|------------|
| Framework | Spring Boot 3.2+ | JDK 17+, embedded Tomcat |
| Language | Java 17 | LTS release, strong types |
| Persistence | Spring Data JPA | ORM, database-agnostic |
| Authentication | Spring Security + JJWT | HS256 tokens, filter-based |
| Database | PostgreSQL 14+ (prod) | H2 in-memory (dev) |
| Build | Maven 3.6+ | Reproducible builds |
| Testing | JUnit 5 + Mockito + Spring Test | 80%+ coverage |

### Deployment Model

- **Container**: Docker (future)
- **Orchestration**: Kubernetes (future, optional)
- **Load balancer**: nginx or cloud LB (TLS termination)
- **Database**: Managed service (RDS, Cloud SQL, Azure Database)
- **Secrets**: HashiCorp Vault or cloud provider secrets manager
- **Logging**: Centralized (ELK, CloudWatch, DataDog)
- **Monitoring**: Health checks, metrics, alerting

---

## Security Requirements

### Authentication & Authorization

1. **Token Format**: HS256 JWT with 1-hour expiry (configurable)
2. **Token Subject**: User ID (Long as string)
3. **Token Claims**: Email (custom claim for convenience only)
4. **Protected Routes**: All non-`/auth/**` routes require valid token
5. **Token Transmission**: HTTP `Authorization: Bearer <token>` header
6. **No Logout**: Phase 1 (tokens valid until expiry)

### Password Security

1. **Hashing Algorithm**: BCrypt with cost factor ≥ 10
2. **Storage**: Never store plaintext passwords
3. **Return Policy**: Never return password in responses or logs
4. **Comparison**: Use BCrypt.checkpw (constant-time, not ==)

### Data Protection

1. **In Transit**: HTTPS required (enforced by reverse proxy in prod)
2. **At Rest**: Database encryption (per environment setup)
3. **Sensitive Fields**: Email, password_hash; all others non-sensitive
4. **PII Handling**: Email is the only PII stored; compliant with minimal data collection

### Error Handling

1. **No Stack Traces**: Error responses never expose internal exceptions
2. **Auth Errors**: 401 responses must not reveal whether email exists or password is wrong
3. **Validation Errors**: 400 responses can be specific (e.g., "Email is required")
4. **JSON Format**: All errors return consistent ErrorResponse (error, message, timestamp, path)

### Secrets Management

1. **Hardcoded Secrets**: FORBIDDEN (zero tolerance)
2. **Environment Variables**: All secrets injected at runtime
3. **Minimum Length**: JWT secret ≥ 32 characters
4. **Rotation**: Supported (restart app with new secret)
5. **Audit**: Never log secrets (tokens, passwords, keys)

---

## API Requirements

### REST Conventions

- **Verb mapping**: POST for creation/action, GET for retrieval
- **Status codes**: 200 (success), 400 (bad request), 401 (unauthorized), 409 (conflict), 500 (error)
- **Content-Type**: `application/json` for all responses
- **Error format**: Standardized ErrorResponse (error, message, timestamp, path)

### Endpoint Contract

| Method | Path | Auth | Request | Response |
|--------|------|------|---------|----------|
| POST | /auth/signup | No | SignupRequest | AuthResponse |
| POST | /auth/login | No | LoginRequest | AuthResponse |
| *(future)* | /* | Bearer token | — | user-specific |

**See `docs/API_CONTRACT.md` for detailed specs.**

### Validation

- **Email**: required, valid email format, unique (application-level check)
- **Password**: required, minimum 6 characters
- **JWT**: required format (3 dot-separated base64 parts), valid signature, not expired

---

## Persistence Requirements

### Phase 1 (Current)

- **Database**: H2 in-memory (dev), PostgreSQL 14+ (future prod)
- **Schema**: Single `user` table (id, email, password_hash, created_at, updated_at)
- **Migrations**: Hibernate auto-DDL (`spring.jpa.hibernate.ddl-auto=update` for dev)
- **Transactions**: Auto-committed by Spring, no explicit @Transactional on controllers

### Phase 2+ (Planned)

- **Migrations**: Flyway or Liquibase for versioned schema changes
- **Tables**: Add `token_blacklist` for logout/revocation
- **Audit**: Add `audit_log` for compliance/security events
- **Sharding**: Consider if multi-region deployment needed

---

## Testing Requirements

### Coverage Targets

| Layer | Target | Type |
|-------|--------|------|
| Service (AuthService) | 85%+ | Unit + integration |
| Controller (AuthController) | 80%+ | Integration |
| Security (JwtUtil, Filter) | 90%+ | Unit + integration |
| **Overall** | **80%+** | All layers |

### Test Categories

1. **Unit Tests**: Isolated service logic, mocked dependencies
2. **Integration Tests**: Full stack (controller → service → repository → H2)
3. **Security Tests**: Password hashing, token validation, error leakage
4. **Smoke Tests**: Manual or CI-based end-to-end verification

### Quality Gates

- All tests must pass: `mvn test`
- Coverage must be ≥ 80%: `mvn jacoco:report`
- No test skips or @Ignore without documented reason

---

## Configuration Requirements

### Environment-Specific

| Property | Dev | Test | Prod |
|----------|-----|------|------|
| `server.port` | 8080 | 8080 | 8080 |
| `spring.datasource.url` | H2 in-mem | H2 in-mem | PostgreSQL |
| `spring.jpa.hibernate.ddl-auto` | update | create-drop | validate |
| `spring.h2.console.enabled` | true | false | false |
| `app.jwt.secret` | demo (unsafe) | test-secret | ENV var |
| `app.jwt.expiration-ms` | 3600000 | 3600000 | 3600000 |
| `logging.level.root` | INFO | WARN | WARN |

### Injection Method

```bash
# Production: via environment variables
export APP_JWT_SECRET="strong-secret-minimum-32-chars-12345"
export SPRING_DATASOURCE_URL="jdbc:postgresql://db.example.com:5432/authdb"
export SPRING_DATASOURCE_USERNAME="dbuser"
export SPRING_DATASOURCE_PASSWORD="dbpass"

java -jar auth-platform.jar --spring.profiles.active=prod
```

---

## Operational Requirements

### Build & Release

1. **Build artifacts**: JAR file, no Docker image (Phase 1)
2. **Version management**: Semantic versioning (major.minor.patch)
3. **Release process**: Tag git, build, document
4. **Rollback**: Previous version JAR available for quick rollback

### Monitoring & Observability

1. **Health endpoint**: Standard Spring Actuator (add spring-boot-starter-actuator)
2. **Logging**: Structured logs (JSON) via SLF4J
3. **Metrics**: Request count, latency, error rates
4. **Alerting**: High error rate (>5%), latency spike (>1s p95)

### Disaster Recovery

1. **RTO** (Recovery Time Objective): < 5 minutes
2. **RPO** (Recovery Point Objective): < 1 hour (stateless app, DB backup)
3. **Backup**: Daily DB backups, retained 30 days
4. **Failover**: Any instance can serve any request (stateless)

---

## Compliance & Standards

### Code Standards

- **Language**: Java 17 conventions
- **Style**: Google Java Style Guide (or enforce via Checkstyle)
- **Naming**: descriptive; avoid abbreviations
- **Comments**: minimal; explain WHY, not WHAT

### Documentation Standards

- README.md: quick start, endpoints, auth format
- docs/ARCHITECTURE.md: system design, data flow
- docs/API_CONTRACT.md: endpoint specs
- docs/ENGINEERING_STANDARDS.md: coding rules
- docs/ADR/: decisions (one per file)

### Dependency Management

- No snapshot dependencies in prod
- All dependencies have security advisories checked (`mvn dependency:check-update`)
- Lock file: pom.xml (Maven, equivalent to package-lock.json)

---

## Performance Requirements

| Metric | Target | Method |
|--------|--------|--------|
| Signup latency | < 200ms p95 | Load test with 100 concurrent users |
| Login latency | < 150ms p95 | Load test with 100 concurrent users |
| Token validation | < 10ms | In-memory signature check |
| Database lookup | < 50ms | Indexed email column |

**Bottleneck**: BCrypt hashing (cost=10 ≈ 100ms/request); acceptable for auth endpoints.

---

## Roadmap Alignment

### Phase 1 (Current)
✓ Signup/login with JWT  
✓ Stateless auth  
✓ H2 in-memory  
✓ Single user table  

### Phase 2 (Planned)
- [ ] Refresh tokens (separate short-lived + long-lived)
- [ ] Token revocation/blacklist
- [ ] User profiles (name, avatar, preferences)
- [ ] Password reset (email verification)
- [ ] Rate limiting

### Phase 3+ (Future)
- [ ] OAuth2 / OIDC providers
- [ ] Multi-factor authentication
- [ ] Role-based authorization
- [ ] API keys for service accounts
- [ ] Audit logging

---

## Deviations & Exceptions

None currently documented. Any deviation from this TRD must be recorded in `docs/ADR/` with rationale.
