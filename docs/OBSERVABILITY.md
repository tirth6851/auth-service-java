# Observability Guide

Lean observability baseline for auth-service-java. The goal is to catch auth failures, abuse patterns, and service degradation without building a full telemetry stack.

---

## Logging

### Current setup

Spring Boot's default Logback configuration is active. Log output goes to stdout — the standard for containerised deployments (Docker, Kubernetes, Cloud Run) where the platform captures stdout and routes it to a log aggregator.

Default format:
```
2026-06-19T17:45:00.123-07:00  INFO 1234 --- [main] c.a.s.AuthService : User registered: user@example.com
```

### Dev vs prod log level

Set in `application.properties` (or via environment variable `LOGGING_LEVEL_COM_AUTHPLATFORM`):

```properties
# Development (verbose)
logging.level.com.authplatform=DEBUG

# Production (default — INFO only)
logging.level.com.authplatform=INFO
```

### Production structured logging (next step)

For production deployments sending logs to an aggregator (ELK, CloudWatch, Datadog), add JSON output via Logstash Logback Encoder:

**pom.xml:**
```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

**`src/main/resources/logback-spring.xml`:**
```xml
<configuration>
  <springProfile name="prod">
    <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
      <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
    </appender>
    <root level="INFO"><appender-ref ref="JSON"/></root>
  </springProfile>

  <springProfile name="!prod">
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    <include resource="org/springframework/boot/logging/logback/console-appender.xml"/>
    <root level="INFO"><appender-ref ref="CONSOLE"/></root>
  </springProfile>
</configuration>
```

### Recommended log events

The service currently emits Spring Boot's default logs (HTTP requests, startup, errors). The table below shows what `AuthService` **should** log once structured logging is added — none of these are implemented yet. See `Audit Logging` in `docs/PROJECT_BACKLOG.md` to track this.

| Event | Level | Notes |
|-------|-------|-------|
| Successful signup | INFO | Include masked email (`al***@example.com`), user ID |
| Successful login | INFO | Include masked email, IP |
| Failed login (wrong credentials) | WARN | Include masked email, IP — helps detect brute force |
| Token refresh | INFO | Include user ID |
| Logout | INFO | Include user ID |
| Rate limit exceeded | WARN | Include IP, endpoint |
| Startup (JWT secret validated) | INFO | Confirm secret length, never the value |
| Unexpected exception | ERROR | Include stack trace |

**Never log:**
- Plaintext passwords
- Raw JWT tokens (access or refresh)
- Full email addresses in production (mask the local part)
- Database passwords or JWT secrets

---

## What to Monitor in Production

### Key metrics (via Spring Boot Actuator + Prometheus/Grafana or cloud dashboard)

| Signal | Alert threshold | Why |
|--------|----------------|-----|
| HTTP 401 rate on `/auth/login` | Spike > 5× baseline | Credential stuffing / brute force in progress |
| HTTP 429 rate on `/auth/login` | Non-zero sustained | Rate limiter is triggering — may indicate attack |
| HTTP 500 rate | Any | Application error; check logs |
| `/actuator/health` status | `DOWN` | DB unreachable or critical component failed |
| JVM heap usage | > 80% | Memory leak or undersized container |
| DB connection pool saturation | pool-usage > 80% | Scale connections or investigate slow queries |

### Actuator endpoints (prod)

Only the health endpoint is exposed in production:

```properties
management.endpoints.web.exposure.include=health
management.endpoint.health.show-details=when-authorized
```

Response without auth: `{"status":"UP"}`  
Response with valid JWT: includes DB status and disk space.

To add Prometheus metrics export (next step):
```properties
management.endpoints.web.exposure.include=health,prometheus
```
And add `spring-boot-starter-actuator` + `micrometer-registry-prometheus` to `pom.xml`.

---

## Auth Event Logging (Future)

For compliance and incident response, each auth event should eventually be persisted to an audit table:

| Event | Fields |
|-------|--------|
| `SIGNUP` | timestamp, email (hashed), IP, user agent, outcome |
| `LOGIN_SUCCESS` | timestamp, user ID, IP, user agent |
| `LOGIN_FAILURE` | timestamp, email (hashed), IP, user agent, reason |
| `TOKEN_REFRESH` | timestamp, user ID |
| `LOGOUT` | timestamp, user ID |

This is tracked in `docs/PROJECT_BACKLOG.md` (Audit Logging).

---

## Health Check

`GET /actuator/health` — no authentication required, safe for load balancer probes.

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

HTTP 200 = healthy. HTTP 503 = unhealthy (DB down or other critical component failure).

Docker Compose health check:
```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
  interval: 30s
  timeout: 10s
  retries: 3
```
