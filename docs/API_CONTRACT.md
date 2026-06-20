# API Contract

## Overview

Auth Platform exposes stateless HTTP endpoints for user authentication. All requests require `Content-Type: application/json`. All responses are JSON.

**Public endpoints** (no token required): `/auth/signup`, `/auth/login`, `/auth/refresh`, `/auth/logout`, `/actuator/health`  
**Protected endpoints** (require `Authorization: Bearer <token>`): all others

---

## Endpoints

### POST /auth/signup

Register a new user and receive a token pair.

**Request:**
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123!"
}
```

**Validation:**
- `email`: required, valid email format, must be unique
- `password`: required, minimum 6 characters

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Errors:**

| Status | Message | Cause |
|--------|---------|-------|
| 400 | Validation failed | Missing/invalid field |
| 409 | Email already registered | Email exists in database |
| 500 | An unexpected error occurred | Server error |

**Example:**
```bash
curl -X POST http://localhost:8080/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"SecurePassword123!"}'
```

---

### POST /auth/login

Authenticate an existing user and receive a token pair.

**Request:**
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123!"
}
```

**Validation:**
- `email`: required, valid email format
- `password`: required, non-empty

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Errors:**

| Status | Message | Cause |
|--------|---------|-------|
| 400 | Validation failed | Missing/invalid field |
| 401 | Invalid credentials | Email not found or wrong password (non-enumerating) |
| 500 | An unexpected error occurred | Server error |

**Example:**
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"SecurePassword123!"}'
```

---

### POST /auth/refresh

Exchange a valid refresh token for a new token pair (rotates the refresh token).

**Auth:** Not required (refresh token is the credential)

**Request:**
```json
{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Validation:**
- `refreshToken`: required, non-blank

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "refreshToken": "new-uuid-here"
}
```

The submitted refresh token is immediately revoked; the response contains a new pair. Do not reuse the old refresh token.

**Errors:**

| Status | Message | Cause |
|--------|---------|-------|
| 400 | Validation failed | Missing refreshToken field |
| 401 | Invalid credentials | Token not found, already revoked, or expired |
| 500 | An unexpected error occurred | Server error |

**Example:**
```bash
curl -X POST http://localhost:8080/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"550e8400-e29b-41d4-a716-446655440000"}'
```

---

### POST /auth/logout

Revoke a refresh token, ending the session.

**Auth:** Not required (refresh token is the credential)

**Request:**
```json
{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response (204 No Content):** Success — refresh token revoked.

The active JWT access token is NOT revoked (it expires naturally per `app.jwt.expiration-ms`, default 1 hour). Clients should discard the access token on logout.

**Errors:**

| Status | Message | Cause |
|--------|---------|-------|
| 400 | Validation failed | Missing refreshToken field |
| 401 | Invalid credentials | Token not found in database |
| 500 | An unexpected error occurred | Server error |

**Note:** Logout with an already-revoked token returns 204 (idempotent — the session was already ended).

**Example:**
```bash
curl -X POST http://localhost:8080/auth/logout \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"550e8400-e29b-41d4-a716-446655440000"}'
```

---

### GET /actuator/health

Health check endpoint for load balancers, monitoring, and orchestration systems.

**Auth:** Not required

**Response (200 OK):**
```json
{
  "status": "UP"
}
```

Full component details (DB status, etc.) are shown only to authenticated requests (`management.endpoint.health.show-details=when-authorized`).

**Response (503 Service Unavailable):** If database or other critical components are down.

**Example:**
```bash
curl http://localhost:8080/actuator/health
```

**Use cases:** Docker `HEALTHCHECK`, Kubernetes liveness probes, load balancer health checks.

---

## JWT Access Token Format

**Type:** Bearer token — include in `Authorization` header for protected endpoints

**Algorithm:** HS256 (HMAC SHA-256)

**Payload:**
```json
{
  "sub": "42",
  "email": "user@example.com",
  "iat": 1750000000,
  "exp": 1750003600
}
```

- `sub`: user ID (string)
- `email`: custom claim
- `iat` / `exp`: Unix timestamps (seconds)
- Default TTL: 1 hour (`app.jwt.expiration-ms=3600000`)

**Authorization header:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

---

## Refresh Token

An opaque UUID v4. Stored as a SHA-256 hash in the database. TTL: 7 days (`app.refresh-token.ttl-ms=604800000`).

- Rotate on every `/auth/refresh` call — always use the latest token
- Revoked on `/auth/logout`
- Revoked tokens cannot be used again (returns 401)

---

## Error Response Format

All errors return a standardized JSON object:

```json
{
  "success": false,
  "error": "Human-readable error message"
}
```

Validation errors include a `details` array:

```json
{
  "success": false,
  "error": "Validation failed",
  "details": [
    "email: must be a well-formed email address",
    "password: size must be between 6 and 2147483647"
  ]
}
```

**Fields:**
- `success`: always `false` for error responses
- `error`: short error message (safe to display to end users)
- `details`: array of field-level messages (validation errors only, omitted otherwise)

---

## Authentication for Protected Routes

```
Authorization: Bearer <access-token>
```

| Condition | Response |
|-----------|----------|
| Valid token | 200 (or endpoint's normal response) |
| Missing header | 401 `{"success":false,"error":"Unauthorized"}` |
| Invalid / expired token | 401 `{"success":false,"error":"Unauthorized"}` |

---

## CORS

Configured via `app.cors.allowed-origins` (comma-separated list).

**Defaults:**
- Allowed origins: `http://localhost:3000`, `http://localhost:5173`
- Allowed methods: `GET, POST, PUT, DELETE, OPTIONS`
- Allowed headers: `Authorization, Content-Type`
- Credentials: allowed

Override per environment:
```properties
# application-prod.properties
app.cors.allowed-origins=https://your-frontend-domain.com
```

---

## Rate Limiting

`POST /auth/login` — 10 attempts per 10 minutes per IP address. Returns 429 Too Many Requests with a `Retry-After` header when exceeded. (Implemented on the `claude/rate-limit-login` branch, pending merge.)

---

## Content Negotiation

- **Request**: `Content-Type: application/json`
- **Response**: `Content-Type: application/json`

---

## Versioning

API is at v1 (implicit). Future versions may use path prefix `/api/v2/auth/...`.
