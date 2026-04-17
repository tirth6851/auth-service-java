# auth-service-java

**Phase 1** of a Spring Boot authentication API. Provides email/password signup,
login, BCrypt password hashing, and stateless JWT authentication.

> Scope: Phase 1 only. No email verification, OTP, API keys, or scheduling.

---

## Tech Stack

- Java 17+
- Spring Boot 3.2
- Spring Web, Spring Data JPA, Spring Security, Spring Validation
- H2 (in-memory database)
- JJWT 0.12.x (JWT)
- Maven

---

## Architecture

Layered, separation of concerns:

```
controller  → HTTP endpoints (thin)
service     → business logic (signup/login)
repository  → JPA data access
model       → JPA entities
dto         → request/response payloads
security    → JWT generation, parsing, filter
config      → Spring Security configuration
```

- Passwords hashed with BCrypt (`PasswordEncoder` bean).
- Stateless sessions — `SessionCreationPolicy.STATELESS`.
- `JwtAuthenticationFilter` populates `SecurityContext` from `Authorization: Bearer <token>`.
- `/auth/**` endpoints are public; everything else requires a valid JWT
  (ready for protected endpoints in later phases).

---

## File Structure

```
auth-service-java/
├── pom.xml
├── README.md
└── src/main/
    ├── java/com/authplatform/
    │   ├── AuthPlatformApplication.java
    │   ├── controller/
    │   │   └── AuthController.java
    │   ├── service/
    │   │   └── AuthService.java
    │   ├── repository/
    │   │   └── UserRepository.java
    │   ├── model/
    │   │   └── User.java
    │   ├── dto/
    │   │   ├── SignupRequest.java
    │   │   ├── LoginRequest.java
    │   │   └── AuthResponse.java
    │   ├── security/
    │   │   ├── JwtUtil.java
    │   │   └── JwtAuthenticationFilter.java
    │   └── config/
    │       └── SecurityConfig.java
    └── resources/
        └── application.properties
```

---

## Data Model — `User`

| Field         | Type      | Notes                          |
|---------------|-----------|--------------------------------|
| `id`          | Long      | Auto-generated primary key     |
| `email`       | String    | Unique, not null               |
| `passwordHash`| String    | BCrypt hash, not null          |
| `isVerified`  | boolean   | Defaults to `false`            |
| `createdAt`   | Instant   | Set on persist                 |

---

## Configuration

`src/main/resources/application.properties`:

- `app.jwt.secret` — HS256 signing key (must be ≥32 chars). **Replace in production.**
- `app.jwt.expiration-ms` — token TTL in ms (default 1 hour).
- H2 console at `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:authdb`).

---

## Run

```bash
mvn spring-boot:run
```

App starts on `http://localhost:8080`.

---

## API

### 1) Signup

`POST /auth/signup`

Request:
```http
POST /auth/signup HTTP/1.1
Content-Type: application/json

{
  "email": "alice@example.com",
  "password": "str0ngPassw0rd!"
}
```

curl:
```bash
curl -X POST http://localhost:8080/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"str0ngPassw0rd!"}'
```

Response (`200 OK`):
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwiZW1haWwiOiJhbGljZUBleGFtcGxlLmNvbSIsImlhdCI6MTcxNTAwMDAwMCwiZXhwIjoxNzE1MDAzNjAwfQ.SIGNATURE",
  "tokenType": "Bearer"
}
```

Errors:
- `400` — invalid email / password < 8 chars
- `409` — email already registered

---

### 2) Login

`POST /auth/login`

Request:
```http
POST /auth/login HTTP/1.1
Content-Type: application/json

{
  "email": "alice@example.com",
  "password": "str0ngPassw0rd!"
}
```

curl:
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"str0ngPassw0rd!"}'
```

Response (`200 OK`):
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9....",
  "tokenType": "Bearer"
}
```

Errors:
- `400` — validation failure
- `401` — invalid credentials

---

### Using the JWT

Send in the `Authorization` header on protected requests:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9....
```

The token subject is the user ID; a custom `email` claim is also included.

---

## What's intentionally NOT in Phase 1

- Email sending
- Email verification / OTP
- API keys
- Scheduled jobs / reminders
- Refresh tokens
- Role-based authorization

These are planned for later phases.
