# System Architecture

## Layered Architecture

```
┌─────────────────────────────────────────────────┐
│ HTTP Requests (Controller Layer)                │
│  - AuthController: /auth/signup, /auth/login    │
└──────────────────┬──────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────┐
│ Security Layer                                   │
│  - JwtAuthenticationFilter: per-request JWT     │
│  - JwtUtil: token generation/validation         │
│  - SecurityConfig: filter chain, BCrypt bean    │
└──────────────────┬──────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────┐
│ Business Logic (Service Layer)                   │
│  - AuthService: signup/login core logic          │
│  - Password validation, JWT creation            │
│  - Raises ResponseStatusException on errors     │
└──────────────────┬──────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────┐
│ Data Access (Repository Layer)                   │
│  - UserRepository: Spring Data JPA interface     │
│  - Queries: findByEmail, exists, save            │
└──────────────────┬──────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────┐
│ Domain Model                                     │
│  - User JPA entity: id, email, password hash    │
│  - DTOs: SignupRequest, LoginRequest, AuthResp  │
└─────────────────────────────────────────────────┘
```

## Request Flow

### Signup
1. Client POST `/auth/signup` with email, password in JSON body
2. AuthController validates DTO (Bean Validation)
3. AuthController delegates to AuthService.signup()
4. AuthService checks if email already exists (→ 409 Conflict)
5. AuthService hashes password with BCrypt
6. AuthService creates User entity and saves via UserRepository
7. AuthService generates JWT (subject = user ID, custom email claim)
8. AuthController returns AuthResponse (token, expiresAt) as JSON
9. GlobalExceptionHandler catches any exceptions and returns ErrorResponse

### Login
1. Client POST `/auth/login` with email, password
2. AuthController validates DTO
3. AuthController delegates to AuthService.login()
4. AuthService queries UserRepository.findByEmail()
5. AuthService compares submitted password with hashed password (BCrypt.matches)
6. If mismatch → 401 Unauthorized
7. If match: generates JWT and returns AuthResponse
8. GlobalExceptionHandler catches any exceptions

### Protected Route (future)
1. Client includes `Authorization: Bearer <token>` header
2. JwtAuthenticationFilter intercepts request
3. JwtUtil.validateToken() parses and validates JWT (signature, expiry)
4. On success: sets UsernamePasswordAuthenticationToken in SecurityContext
5. Request proceeds to endpoint
6. On failure (invalid/expired): Http401UnauthorizedEntryPoint returns 401 JSON

## JWT Structure

- **Algorithm**: HS256 (HMAC SHA-256)
- **Subject**: user ID as string (Long → String)
- **Claims**:
  - `sub`: user ID (standard claim)
  - `email`: custom claim (user's email)
  - `iat`: issued at (standard, auto-added by JJWT)
  - `exp`: expiry timestamp (standard, auto-added, default +1 hour)
- **Signing Key**: derived from `app.jwt.secret` property (must be ≥32 chars)

Example decoded JWT:
```json
{
  "sub": "42",
  "email": "user@example.com",
  "iat": 1718817600,
  "exp": 1718821200
}
```

## Database Schema

Single table: `user`
- `id` (BIGINT, PRIMARY KEY, auto-increment)
- `email` (VARCHAR, UNIQUE, NOT NULL)
- `password_hash` (VARCHAR, NOT NULL, BCrypt hashed)
- `created_at` (TIMESTAMP, auto-generated)
- `updated_at` (TIMESTAMP, auto-updated)

H2 in-memory; schema auto-created on startup via Hibernate DDL-auto = update.

## Security Model

- **Trust boundary**: JWT signature validates server-issued tokens
- **Threat**: token forging (mitigated by HMAC secret, never expose secret)
- **Threat**: brute-force password guessing (mitigated by BCrypt with salting)
- **Threat**: plaintext passwords in logs/responses (mitigated by never returning password field)
- **Threat**: token replay (mitigated by expiry; no revocation in Phase 1)
- **Threat**: man-in-the-middle (mitigated by HTTPS in production)

## Error Handling

All endpoint errors return JSON ErrorResponse via GlobalExceptionHandler:
```json
{
  "error": "Bad Request",
  "message": "Email is required",
  "timestamp": "2024-06-19T12:00:00Z",
  "path": "/auth/signup"
}
```

HTTP status codes:
- 400: Bad Request (validation errors, malformed JSON)
- 401: Unauthorized (invalid credentials, expired token)
- 409: Conflict (duplicate email)
- 500: Internal Server Error (unexpected exceptions)

## Deployment Model

### Local Development
- **Container**: None (direct JVM on host)
- **Database**: H2 in-memory
- **Config**: `application.properties`
- **Start**: `mvn spring-boot:run`

### Docker Compose (Local Prod-like)
- **Container**: Alpine JRE 17 (runtime image)
- **Database**: PostgreSQL 16 (separate container)
- **Config**: Environment variables + `application-prod.properties`
- **Start**: `docker-compose up --build`
- **Health checks**: Both services monitored; app depends_on postgres health

### Production
- **Container**: Docker image (built via Dockerfile)
- **Database**: Managed PostgreSQL (RDS, Cloud SQL, etc.)
- **Reverse proxy**: nginx/HAProxy (HTTPS termination)
- **Orchestration**: Kubernetes or container-based (ECS, Cloud Run, etc.)
- **Secrets**: HashiCorp Vault / Cloud Secrets Manager
- **Logging**: Centralized (CloudWatch, ELK, DataDog)
- **Monitoring**: Health check endpoint + APM

### Dockerfile Strategy
- **Stage 1**: Maven build (compile + test + package)
- **Stage 2**: Runtime (JRE 17 on Alpine, minimal attack surface)
- **Health check**: `wget --spider /actuator/health` every 30s

## Deployment Assumptions

- Spring Boot embedded Tomcat serves HTTP
- External reverse proxy (nginx/load balancer) enforces HTTPS in production
- JWT secret is injected via environment variable or secure config system
- H2 in-memory database is for dev/test only; production uses persistent DB
- Stateless design allows horizontal scaling (any instance can validate JWT)
- PostgreSQL is required for production (not H2)
