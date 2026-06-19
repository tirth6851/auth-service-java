---
name: security-review
description: Audit auth, secrets, password handling, error leakage, and security standards
tags: [security, audit, auth]
---

# Security Review Skill

Comprehensive security audit of authentication, secrets, error handling, and sensitive data exposure.

## When to Run

- Before any PR touching auth, config, or password handling
- Before any code merged to main
- Before any release
- Whenever error responses or logging changes

## Security Checklist

### Secrets & Configuration

- [ ] No hardcoded secrets in code (passwords, API keys, JWT secrets)
- [ ] JWT secret comes from environment variable `APP_JWT_SECRET`
- [ ] JWT secret is at least 32 characters
- [ ] No secrets in `application.properties` (only in `application-prod.properties` or env vars)
- [ ] H2 console disabled in production: `spring.h2.console.enabled=false` in prod profile
- [ ] All database credentials are environment-injected, not hardcoded

**Command:** `grep -r "secret=\|password=\|key=" src/main/resources/application.properties`

### Password Handling

- [ ] All passwords are BCrypt-hashed before storage
- [ ] Password field is never returned in API responses
- [ ] Password field is never logged
- [ ] Password comparison uses BCrypt.checkpw (constant-time, not ==)
- [ ] Plaintext password accepted only at HTTP request boundary (DTO)
- [ ] Password hash stored in database with `password_hash` column name

**Check:**
```bash
# Verify BCrypt usage
grep -r "BCrypt\|PasswordEncoder" src/main/java/
# Verify no plaintext returns
grep -r "password" src/main/java/com/authplatform/dto/ | grep -v "password_hash"
```

### JWT Token Security

- [ ] Token algorithm is HS256 (symmetric, not RS256 unless using KeyStore)
- [ ] Token signing key is derived from secure secret (not hardcoded)
- [ ] Token subject (`sub`) is user ID, not email
- [ ] Token includes `email` custom claim only for convenience, not trust
- [ ] Token expiry is set (default 1 hour)
- [ ] Token is validated on every request (signature + expiry)
- [ ] Invalid/expired tokens return 401 with minimal error message

**Check:**
```java
// Verify in JwtUtil
Jwts.builder()
    .subject(userId.toString())           // ✓ Not email
    .signWith(key)                        // ✓ Secure key
    .expiration(new Date(...))            // ✓ Has expiry
    .compact();
```

### Error Responses

- [ ] Error responses are JSON (consistent format)
- [ ] Error responses don't expose stack traces to client
- [ ] Validation errors (400) are specific enough to help user
- [ ] Auth errors (401) don't reveal whether email exists or password is wrong
- [ ] Database errors (500) don't expose table names, SQL queries, or schema
- [ ] Error response includes `error`, `message`, `timestamp`, `path` (no sensitive fields)

**Check:**
```bash
# Verify error handler
grep -A 20 "GlobalExceptionHandler" src/main/java/com/authplatform/exception/

# Verify error responses never leak password/token
grep -r "password\|token" src/main/java/com/authplatform/exception/
```

### Logging

- [ ] Logs never include passwords, tokens, or user secrets
- [ ] Logs use SLF4J (not System.out)
- [ ] Sensitive operations are logged at DEBUG level (not INFO)
- [ ] Errors are logged with enough context to debug, but not internals

**Check:**
```bash
grep -r "System.out\|System.err" src/main/java/
# Should find nothing

grep -r "password\|token" src/main/java/com/authplatform/service/ | grep -i "log\|print"
# Should find nothing
```

### Authentication & Authorization

- [ ] POST /auth/signup is public (permit-all)
- [ ] POST /auth/login is public (permit-all)
- [ ] All other endpoints require valid JWT token
- [ ] JwtAuthenticationFilter runs on every request
- [ ] SecurityContext is properly set with authenticated user
- [ ] CORS is disabled (or explicitly configured if needed)

**Check:**
```java
// In SecurityConfig
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/auth/**").permitAll()    // ✓ Public
    .requestMatchers("/h2-console/**").permitAll() // ✓ Dev-only
    .anyRequest().authenticated()                // ✓ Everything else requires auth
)
```

### Database Security

- [ ] Email is unique and non-null
- [ ] No sensitive data stored unencrypted (passwords are hashed)
- [ ] SQL injection is prevented (using Spring Data JPA, not raw SQL)
- [ ] No test/dummy data with known passwords in prod

**Check:**
```java
// Verify in User entity
@Column(unique = true, nullable = false)
private String email;

@Column(nullable = false)
private String passwordHash;  // Not password
```

### HTTP Security

- [ ] All responses include `Content-Type: application/json`
- [ ] HTTPS required in production (enforced by reverse proxy)
- [ ] No sensitive data in URL paths (use request body)
- [ ] Authorization header format is `Bearer <token>` (case-sensitive)

### Code Review

- [ ] No `@SuppressWarnings("unchecked")` or other suppression without justification
- [ ] No use of `@Transactional` on controllers (service only)
- [ ] No package-private fields (use private)
- [ ] No reflection or unsafe field access

## Security Test Cases

If none exist, recommend adding:

```java
@Test
void passwordIsNeverReturnedInResponse() { ... }

@Test
void invalidTokenReturns401() { ... }

@Test
void passwordHashIsNotPlaintext() { ... }

@Test
void errorResponseDoesNotExposeStackTrace() { ... }
```

## Output Format

Report findings as:

**✓ Pass:** [Check name]  
**⚠ Warning:** [Check name] — [description]  
**✗ Fail:** [Check name] — [description, requires fix before merge]

## Before Marking Complete

- [ ] All critical failures fixed (marked as ✗)
- [ ] Warnings reviewed and documented
- [ ] No new secrets introduced
- [ ] Password/token handling is correct
- [ ] Error responses are safe
- [ ] Logging is secure
- [ ] All security tests pass

## References

- [docs/ENGINEERING_STANDARDS.md](../../docs/ENGINEERING_STANDARDS.md#security-standards) — security standards
- [docs/ARCHITECTURE.md](../../docs/ARCHITECTURE.md#security-model) — security model
- [OWASP Top 10](https://owasp.org/www-project-top-ten/) — security threats
