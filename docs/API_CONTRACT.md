# API Contract

## Overview

Auth Platform exposes two stateless HTTP endpoints for user signup and login. Both return JWT tokens. No authentication is required for these endpoints.

## Endpoints

### POST /auth/signup

Register a new user and receive a JWT token.

**Request:**
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123!"
}
```

**Headers:**
```
Content-Type: application/json
```

**Query Parameters:** None

**Auth:** Not required

**Validation:**
- `email`: required, must be valid email format, must be unique (not already registered)
- `password`: required, minimum 6 characters

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresAt": 1718821200,
  "email": "user@example.com"
}
```

**Errors:**

| Status | Error Code | Message | Cause |
|--------|-----------|---------|-------|
| 400 | Bad Request | Email is required | Missing email field |
| 400 | Bad Request | Invalid email format | Email doesn't match pattern |
| 400 | Bad Request | Password is required | Missing password field |
| 400 | Bad Request | Password must be at least 6 characters | Password too short |
| 409 | Conflict | Email already registered | Email exists in database |
| 500 | Internal Server Error | An unexpected error occurred | Server-side exception |

**Example curl:**
```bash
curl -X POST http://localhost:8080/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"SecurePassword123!"}'
```

---

### POST /auth/login

Authenticate a user with email and password, receive a JWT token.

**Request:**
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123!"
}
```

**Headers:**
```
Content-Type: application/json
```

**Query Parameters:** None

**Auth:** Not required

**Validation:**
- `email`: required, must be valid email format
- `password`: required, non-empty

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresAt": 1718821200,
  "email": "user@example.com"
}
```

**Errors:**

| Status | Error Code | Message | Cause |
|--------|-----------|---------|-------|
| 400 | Bad Request | Email is required | Missing email field |
| 400 | Bad Request | Invalid email format | Email doesn't match pattern |
| 400 | Bad Request | Password is required | Missing password field |
| 401 | Unauthorized | Invalid email or password | Email not found or password mismatch |
| 500 | Internal Server Error | An unexpected error occurred | Server-side exception |

**Example curl:**
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"SecurePassword123!"}'
```

---

## JWT Token Format

**Type:** Bearer token (include in `Authorization` header for protected endpoints)

**Algorithm:** HS256 (HMAC SHA-256)

**Header:**
```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

**Payload:**
```json
{
  "sub": "42",                    // user ID (subject claim)
  "email": "user@example.com",    // custom email claim
  "iat": 1718817600,              // issued at (seconds since epoch)
  "exp": 1718821200               // expires at (seconds since epoch)
}
```

**Example Authorization header:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI0MiIsImVtYWlsIjoidXNlckBleGFtcGxlLmNvbSIsImlhdCI6MTcxODgxNzYwMCwiZXhwIjoxNzE4ODIxMjAwfQ.SIGNATURE
```

---

## Error Response Format

All errors return a standardized JSON error object:

```json
{
  "error": "Conflict",
  "message": "Email already registered",
  "timestamp": "2024-06-19T12:34:56Z",
  "path": "/auth/signup"
}
```

**Fields:**
- `error`: HTTP status text (e.g., "Bad Request", "Unauthorized", "Conflict")
- `message`: Human-readable error description
- `timestamp`: ISO 8601 timestamp of error
- `path`: Request path that caused error

---

## Authentication for Protected Routes

For future endpoints that require authentication, include JWT token in header:

```
Authorization: Bearer <token>
```

**Valid token:** Request proceeds, user info available in SecurityContext

**Invalid/expired token:** Returns 401 Unauthorized
```json
{
  "error": "Unauthorized",
  "message": "Invalid or expired token",
  "timestamp": "2024-06-19T12:34:56Z",
  "path": "/protected/endpoint"
}
```

**Missing header:** Returns 401 Unauthorized

---

## Rate Limiting

None in Phase 1.

---

## Versioning

API is at v1 (implicit). Future versions may use path prefix `/api/v2/auth/...` or header-based versioning.

---

## Data Types

- `string`: JSON string (quoted)
- `integer`: JSON number (unquoted integer)
- `timestamp`: Unix timestamp in seconds (integer)

---

## Content Negotiation

- **Request**: JSON only (`Content-Type: application/json`)
- **Response**: JSON only (`Content-Type: application/json`)

---

## CORS

None configured in Phase 1. Add `spring-boot-starter-web` + `WebMvcConfigurer` if needed.
