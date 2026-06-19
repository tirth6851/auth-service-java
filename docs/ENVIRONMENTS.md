# Environments & Configuration

## Configuration Hierarchy

Properties are loaded in this order (later overrides earlier):

1. `application.properties` (default, bundled)
2. `application-{profile}.properties` (environment-specific, bundled)
3. Environment variables (highest priority, injected at runtime)

Active profile is controlled by `SPRING_PROFILES_ACTIVE` env var.

---

## Local Development

**Profile:** default (no profile suffix)

**`application.properties`:**
```properties
# Server
server.port=8080

# Database
spring.datasource.url=jdbc:h2:mem:authdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect

# H2 Console (dev only)
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# JWT (DEMO ONLY - NEVER USE IN PRODUCTION)
app.jwt.secret=this-is-a-demo-secret-minimum-32-characters-long-12345
app.jwt.expiration-ms=3600000

# Logging
logging.level.root=INFO
logging.level.com.authplatform=DEBUG
```

**Startup:**
```bash
mvn spring-boot:run
# or
java -jar target/auth-platform-0.0.1-SNAPSHOT.jar
```

**Access:**
- API: http://localhost:8080
- H2 Console: http://localhost:8080/h2-console
- H2 JDBC URL: `jdbc:h2:mem:authdb`
- H2 User: `sa` (no password)

---

## Testing (CI/CD)

**Profile:** test

**Environment variables:**
```bash
SPRING_PROFILES_ACTIVE=test
APP_JWT_SECRET=test-secret-minimum-32-characters-long-123456789012
```

**`application-test.properties`:**
```properties
server.port=8080

# H2 in-memory
spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect

# H2 Console disabled in test
spring.h2.console.enabled=false

# Logging
logging.level.root=WARN
logging.level.com.authplatform=INFO
```

**Run:**
```bash
mvn test
```

Database is created fresh for each test suite and dropped after (`create-drop`).

---

## Production

**Profile:** prod

**Environment variables:**
```bash
SPRING_PROFILES_ACTIVE=prod
APP_JWT_SECRET=<strong-random-secret-min-32-chars-from-vault>
SPRING_DATASOURCE_URL=<persistent-database-url>
SPRING_DATASOURCE_USERNAME=<db-user>
SPRING_DATASOURCE_PASSWORD=<db-password>
```

**`application-prod.properties`:**
```properties
# Server
server.port=8080

# Database (use persistent DB, NOT H2)
# Injected via environment variables:
# spring.datasource.url=<SPRING_DATASOURCE_URL>
# spring.datasource.username=<SPRING_DATASOURCE_USERNAME>
# spring.datasource.password=<SPRING_DATASOURCE_PASSWORD>
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
# or org.postgresql.Driver for PostgreSQL

# JPA
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect

# JWT (INJECTED FROM ENVIRONMENT)
# app.jwt.secret=<APP_JWT_SECRET>
app.jwt.expiration-ms=3600000

# Logging
logging.level.root=WARN
logging.level.com.authplatform=INFO

# Disable H2 console
spring.h2.console.enabled=false
```

**Startup:**
```bash
java -jar auth-platform-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  --spring.datasource.url=$SPRING_DATASOURCE_URL \
  --spring.datasource.username=$SPRING_DATASOURCE_USERNAME \
  --spring.datasource.password=$SPRING_DATASOURCE_PASSWORD \
  --app.jwt.secret=$APP_JWT_SECRET
```

**Assumptions:**
- Reverse proxy (nginx) enforces HTTPS and handles SSL/TLS termination
- Secrets managed by HashiCorp Vault, AWS Secrets Manager, or equivalent
- Persistent database: MySQL, PostgreSQL, or managed cloud service
- No H2 in production (in-memory, resets on restart)
- Horizontal scaling: stateless app, any instance can validate JWT

---

## Configuration Reference

### Core Properties

| Property | Default | Type | Description |
|----------|---------|------|-------------|
| `server.port` | 8080 | int | HTTP port |
| `app.jwt.secret` | (required) | string | HMAC secret, min 32 chars |
| `app.jwt.expiration-ms` | 3600000 | long | Token TTL in milliseconds |
| `spring.jpa.hibernate.ddl-auto` | update | string | DDL: `create-drop` (test), `update` (dev), `validate` (prod) |

### Database Properties (Injected)

| Property | Example | Type | Description |
|----------|---------|------|-------------|
| `spring.datasource.url` | jdbc:mysql://localhost:3306/authdb | string | JDBC connection URL |
| `spring.datasource.username` | root | string | DB user |
| `spring.datasource.password` | password | string | DB password |
| `spring.datasource.driver-class-name` | com.mysql.cj.jdbc.Driver | string | JDBC driver |

---

## Secret Management

### Development
- Store dummy secret in `application.properties` (never use in prod)
- Example: `app.jwt.secret=demo-secret-this-is-not-secure-123456789012345`

### Production
- **Never** commit real secrets to git
- Store in HashiCorp Vault, AWS Secrets Manager, or similar
- Inject at runtime via environment variables
- Rotate secrets periodically (update Vault, restart app)

### Example Vault Injection (HashiCorp)
```bash
vault kv get secret/auth-platform/prod
# Returns: key=APP_JWT_SECRET, value=<actual-secret>

export APP_JWT_SECRET=$(vault kv get -field=APP_JWT_SECRET secret/auth-platform/prod)
java -jar auth-platform.jar
```

---

## Database Migration

**Local/Test:**
- DDL auto-managed: `spring.jpa.hibernate.ddl-auto=create-drop` or `update`
- No manual migrations needed

**Production:**
- Use Flyway or Liquibase for versioned schema migrations
- Set `spring.jpa.hibernate.ddl-auto=validate` (no auto-changes)
- Run migrations before deploying new code

---

## Deployment Assumptions

1. **Container**: Docker/K8s or VM-based deployment
2. **Reverse proxy**: nginx/HAProxy terminates HTTPS
3. **Load balancer**: directs traffic to multiple app instances
4. **Logging**: centralized (ELK, CloudWatch, DataDog)
5. **Monitoring**: health checks at `/actuator/health` (add spring-boot-starter-actuator if needed)
6. **Database**: managed service (RDS, Cloud SQL, Azure Database)

---

## Actuator Configuration

Spring Boot Actuator is enabled in all profiles (dev, test, prod).

**Exposed endpoints:** `health` only (restrictive by default)

**Dev (`application.properties`):**
```properties
management.endpoints.web.exposure.include=health
management.endpoint.health.show-details=when-authorized
```

**Prod (`application-prod.properties`):**
```properties
management.endpoints.web.exposure.include=health
management.endpoint.health.show-details=when-authorized
```

**Health Check Endpoint:**
```bash
curl http://localhost:8080/actuator/health
# Response: {"status":"UP","components":{...}}
```

**Used by:**
- Docker health checks (see Dockerfile `HEALTHCHECK`)
- Kubernetes liveness/readiness probes
- Load balancer health checks
- Monitoring systems (Datadog, New Relic, etc.)
