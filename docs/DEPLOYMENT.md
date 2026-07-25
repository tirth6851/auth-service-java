# Deployment Guide

How to ship auth-service-java. See [RUNBOOK.md](RUNBOOK.md) for day-to-day operations, incidents, and debugging.

---

## Environment Variables

All secrets and environment-specific values are supplied via environment variables. **Never commit these to version control.**

| Variable | Required | Description | Example |
|----------|----------|-------------|---------|
| `JWT_SECRET` | Yes | HMAC-SHA256 signing key. Minimum 32 characters. | `openssl rand -base64 48` |
| `POSTGRES_JDBC_URL` | Prod only | Full JDBC connection string | `jdbc:postgresql://db:5432/authdb` |
| `POSTGRES_USER` | Prod only | Database username | `authuser` |
| `POSTGRES_PASSWORD` | Prod only | Database password | `<strong random value>` |
| `SPRING_PROFILES_ACTIVE` | Prod only | Must be `prod` to activate production config | `prod` |
| `APP_CORS_ALLOWED_ORIGINS` | Optional | Override CORS origins (comma-separated) | `https://app.example.com` |
| `APP_JWT_EXPIRATION_MS` | Optional | Access token TTL in ms (default: 3600000 = 1 hr) | `900000` (15 min recommended for prod) |

Generate a strong JWT secret:
```bash
openssl rand -base64 48
```

---

## Profiles

| Profile | Database | Flyway | H2 Console | Swagger UI | Use for |
|---------|----------|--------|------------|------------|---------|
| *(default)* | H2 in-memory | Disabled | Disabled | Enabled | Local dev, CI |
| `dev` | H2 in-memory | Disabled | Enabled | Enabled | Local dev with H2 console |
| `prod` | PostgreSQL | Enabled | Disabled | Disabled | Any shared / public deployment |

Activate a profile:
```bash
# Via environment variable (preferred)
SPRING_PROFILES_ACTIVE=prod java -jar auth-platform-*.jar

# Via Maven (dev only)
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## Docker Deployment (Recommended)

### Prerequisites

- Docker 20.10+
- Docker Compose 2.0+

### Steps

1. **Copy and fill in the environment file:**
   ```bash
   cp .env.example .env
   # Edit .env — set JWT_SECRET, POSTGRES_PASSWORD, and any others
   ```

2. **Build and start:**
   ```bash
   docker-compose up --build -d
   ```

3. **Verify health:**
   ```bash
   curl http://localhost:8080/actuator/health
   # Expected: {"status":"UP"}
   ```

4. **View logs:**
   ```bash
   docker-compose logs -f app
   ```

### What docker-compose runs

| Service | Image | Port | Notes |
|---------|-------|------|-------|
| `postgres` | `postgres:16-alpine` | 5432 | Persistent volume `postgres_data` |
| `app` | Built from `Dockerfile` | 8080 | Waits for `postgres` healthcheck |

### Flyway migrations

When the app starts in `prod` profile, Flyway automatically applies any pending migrations from `src/main/resources/db/migration/`:

| Migration | Description |
|-----------|-------------|
| `V1__create_users_table.sql` | `users` table with LOWER(email) unique index |
| `V2__create_refresh_tokens_table.sql` | `refresh_tokens` table with FK to users |

Migrations are append-only — never edit an existing migration file. Create a new `V3__...` file for schema changes.

---

## JAR Deployment (Without Docker)

### Build

```bash
mvn clean package -DskipTests
# Output: target/auth-platform-0.0.1-SNAPSHOT.jar
```

### Run

```bash
SPRING_PROFILES_ACTIVE=prod \
JWT_SECRET="your-strong-secret" \
POSTGRES_JDBC_URL="jdbc:postgresql://localhost:5432/authdb" \
POSTGRES_USER="authuser" \
POSTGRES_PASSWORD="your-db-password" \
java -jar target/auth-platform-0.0.1-SNAPSHOT.jar
```

### Verify

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}

curl -X POST http://localhost:8080/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"smoke@example.com","password":"SmokeTest123!"}'
# {"token":"eyJ...","tokenType":"Bearer","refreshToken":"..."}
```

---

## Deploying to Cloud Platforms

### Render / Fly.io (general pattern)

These platforms build from your `Dockerfile` and inject environment variables via their dashboard.

1. Set environment variables in the platform dashboard (see table above)
2. Set `SPRING_PROFILES_ACTIVE=prod`
3. The platform builds the image and deploys; Flyway migrations run on startup
4. Health check path: `/actuator/health`

### Render: backend + Auth Portal (two services)

Once the [Auth Portal](../web/README.md) exists, a full deployment is **two separate Render Web
Services in one project** — the Java backend and the Next.js BFF — plus a managed Render
Postgres instance. They do not share a runtime; the portal talks to the backend over the public
internet exactly like any other client.

> **One-click alternative:** the repo root `render.yaml` is a
> [Render Blueprint](https://render.com/docs/blueprint-spec) that provisions all three resources
> (Postgres, backend, portal) from Render's dashboard (New → Blueprint) in one pass, matching the
> manual steps below. It still prompts for the secrets that must never be committed
> (`JWT_SECRET`, `SESSION_SECRET`), and `POSTGRES_JDBC_URL` / `AUTH_API_URL` need a one-time manual
> check after first deploy since Render can't auto-format a JDBC URL or concatenate a URL scheme
> onto a hostname in a blueprint — see the comments in `render.yaml` for details. The manual steps
> below still apply if you'd rather configure each service by hand.

**1. Postgres instance:**

- Create a **Render Postgres** database first (Dashboard → New → PostgreSQL). Render provisions
  it with an **Internal Database URL** (for services in the same region) and an **External
  Database URL**; use the internal one from the backend service to avoid egress and latency.
- The internal URL is `postgres://<user>:<password>@<host>/<db>` — Spring/JDBC needs the
  `jdbc:postgresql://` form, so split it into the three vars below rather than pasting the URL
  directly into `POSTGRES_JDBC_URL`.

**2. Backend service** (`auth-service-java`, Web Service, builds from the repo root
`Dockerfile`):

- Set Root Directory to the repo root; Render detects and builds the `Dockerfile` automatically.
- Environment variables (Dashboard → Environment; pull the DB values from the Postgres
  instance's "Connect" tab):
  ```
  SPRING_PROFILES_ACTIVE=prod
  JWT_SECRET=<openssl rand -base64 48>
  POSTGRES_JDBC_URL=jdbc:postgresql://<internal-host>/<database>
  POSTGRES_USER=<user>
  POSTGRES_PASSWORD=<password>
  ```
- Health check path: `/actuator/health` (set under Settings → Health Check Path so Render's
  zero-downtime deploys wait for it). Flyway migrations run automatically on startup.
- Note the service's public `onrender.com` URL — the portal needs it next.
- Render's free-tier Web Services spin down after 15 min idle and cold-start on the next
  request; the first login after idle will be slow. Fine for a demo, not for anything
  latency-sensitive without a paid plan.

**3. Portal service** (`web/`, Web Service, Node runtime — set Root Directory to `web` in the
service's Settings):

- Build command: `npm install && npm run build`. Start command: `npm run start`.
- Environment variables:
  ```
  AUTH_API_URL=https://<backend-service>.onrender.com
  SESSION_SECRET=<node -e "console.log(require('crypto').randomBytes(32).toString('hex'))">
  ```
- Because the browser only ever talks to the portal (same-origin BFF), the **backend's
  `app.cors.allowed-origins` is irrelevant to the portal** — that setting only matters if some
  *other*, non-BFF client calls the API directly from a browser. The portal's own outbound calls
  to `AUTH_API_URL` are server-to-server and aren't subject to CORS at all.
- Render auto-assigns a public `onrender.com` URL for this service; that's what end users visit.

**Deploy order:** Postgres instance first, then the backend (so you have its URL for
`AUTH_API_URL`), then the portal.

### AWS ECS / Google Cloud Run

1. Build and push image:
   ```bash
   docker build -t auth-platform:latest .
   docker tag auth-platform:latest <registry>/<repo>:latest
   docker push <registry>/<repo>:latest
   ```
2. Set the environment variables in your task definition / Cloud Run service
3. Configure the load balancer health check on `/actuator/health`
4. Set the active profile to `prod`

---

## Production Checklist

Before exposing to any real traffic:

- [ ] `JWT_SECRET` is a random 48+ byte value (not the placeholder)
- [ ] `SPRING_PROFILES_ACTIVE=prod` — H2 console and Swagger are disabled
- [ ] PostgreSQL password is strong and not reused
- [ ] TLS is terminated at the load balancer / reverse proxy (nginx, Caddy, ALB)
- [ ] CORS `app.cors.allowed-origins` is set to your frontend domain(s) only
- [ ] `app.jwt.expiration-ms=900000` (15 minutes recommended; refresh tokens handle renewal)
- [ ] `/actuator/health` is reachable by load balancer, all other `/actuator/**` are blocked
- [ ] `.env` is in `.gitignore` and never committed
- [ ] Flyway migrations applied cleanly (`status: UP` on health check)
- [ ] Smoke test: signup → login → refresh → logout flow works end-to-end

If deploying the [Auth Portal](../web/README.md) alongside the backend:

- [ ] `SESSION_SECRET` is a random 32+ byte value, distinct per environment
- [ ] `AUTH_API_URL` points at the backend's real deployed URL (not `localhost`)
- [ ] Portal is served over HTTPS — the session cookie's `secure` flag is enabled automatically
      when `NODE_ENV=production`, which drops the cookie silently over plain HTTP

---

## Rollback

1. Deploy the previous container image tag (or previous JAR)
2. If a Flyway migration was applied:
   - Flyway does not support automatic undo for community edition
   - Restore from a database backup taken before the migration
   - Or write a compensating `V3__rollback_*.sql` migration (additive changes only)
3. Verify health check returns `{"status":"UP"}` after rollback

---

## What's Not Production-Ready Yet

| Gap | Impact | Fix |
|-----|--------|-----|
| No TLS in application | Credentials travel in plaintext | Terminate TLS at load balancer or set `server.ssl.*` |
| No token denylist | Logout doesn't invalidate JWT (only refresh token) | Add Redis-based JWT denylist or shorten access token TTL to ≤15 min |
| H2 used in tests | DB behaviour may differ from PostgreSQL | Add Testcontainers with PostgreSQL for integration tests |
| Rate limiting not on `main` | `/auth/login` is unbounded | Merge `claude/rate-limit-login` branch |
| Access token TTL is 1 hour | Long window for stolen token misuse | Set `APP_JWT_EXPIRATION_MS=900000` in prod config |
