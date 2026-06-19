# Runbook

## Local Development

### First Run

1. **Clone and enter project:**
   ```bash
   cd auth-service-java
   ```

2. **Verify Java and Maven:**
   ```bash
   java -version          # JDK 17+
   mvn -version          # Maven 3.6+
   ```

3. **Build and run:**
   ```bash
   mvn clean package     # Compile, test, package
   mvn spring-boot:run   # Start app on localhost:8080
   ```

4. **Verify startup:**
   - App logs: `Started AuthPlatformApplication`
   - H2 console: http://localhost:8080/h2-console
   - API ready: POST to http://localhost:8080/auth/signup

### Typical Workflow

```bash
# 1. Make code changes
# 2. Run tests
mvn test

# 3. If tests pass, start app
mvn spring-boot:run

# 4. Manually test endpoints
curl -X POST http://localhost:8080/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"SecurePass123"}'

# 5. Commit changes
git add .
git commit -m "feat: add new feature"
git push
```

---

## Testing

### Run All Tests
```bash
mvn test
```

**Output:** Summary of unit and integration tests. All must pass.

### Run Single Test Class
```bash
mvn test -Dtest=AuthServiceTest
```

### Run with Coverage Report
```bash
mvn clean test jacoco:report
# Report: target/site/jacoco/index.html
```

### Common Test Failures

| Issue | Cause | Fix |
|-------|-------|-----|
| Tests fail on startup | H2 in-memory DB conflict | Kill any running app (`mvn spring-boot:stop`) |
| Test timeout | Slow test | Increase timeout in `pom.xml` or debug test |
| Port already in use | Port 8080 taken | Change `server.port` in `application-test.properties` |

---

## Debugging

### Enable Debug Logging
```bash
LOGGING_LEVEL_COM_AUTHPLATFORM=DEBUG mvn spring-boot:run
```

### Attach IDE Debugger
1. Start app in debug mode:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"
   ```

2. In IDE (IntelliJ/Eclipse): Run → Edit Configurations → Debug → Attach to localhost:5005

### H2 Console
- URL: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:authdb`
- User: `sa` (no password)
- Browse schema, run queries: `SELECT * FROM user;`

### Request/Response Logging
Enable in `application.properties`:
```properties
logging.level.org.springframework.web=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
```

---

## Build & Package

### Compile Only (No Tests)
```bash
mvn clean compile -DskipTests
```

### Package into JAR
```bash
mvn clean package
# Output: target/auth-platform-0.0.1-SNAPSHOT.jar
```

### Run JAR
```bash
java -jar target/auth-platform-0.0.1-SNAPSHOT.jar
```

### With Custom Port
```bash
java -jar target/auth-platform-0.0.1-SNAPSHOT.jar --server.port=9090
```

---

## CI/CD Pipeline (GitHub Actions)

### How It Works

GitHub Actions automatically runs on every push and pull request:

1. **Trigger**: Push to `main` or `claude/**` branch, or any PR to `main`
2. **Runner**: Ubuntu 22.04 (ubuntu-latest)
3. **Build**: Java 17 (Temurin) + Maven
4. **Steps**:
   - Checkout code
   - Set up Java 17
   - Cache Maven dependencies (fast builds)
   - Run `mvn -B verify` (compile + test)
   - Upload coverage reports as artifacts
5. **Result**: Green check (passed) or red X (failed)

### CI Status

Check CI status in GitHub:
- **PR view**: Look for "All checks passed" or red X below PR title
- **Actions tab**: View full logs of every run
- **Coverage reports**: Download JaCoCo HTML report from Artifacts

### When CI Fails

**Local diagnosis:**
```bash
mvn clean verify
# Re-run the exact CI command locally
```

**Common failures:**
- Tests failing: Run `mvn test`, fix failures, push again
- Compilation error: Run `mvn compile`, fix syntax, push again
- Dependency issue: Run `mvn dependency:resolve`, update pom.xml if needed

**After fixing locally:**
```bash
git add .
git commit -m "fix: [reason]"
git push
# CI automatically re-runs
```

### Before Merging

- ✅ CI must be green (all checks passing)
- ✅ Coverage must be ≥ 80% (check artifact report)
- ✅ Code review approved
- ✅ No merge if CI is red

### CI Configuration

Located in `.github/workflows/ci.yml`. To modify:
- Change Java version: update `java-version: '17'` in ci.yml
- Add new build step: edit ci.yml and push to trigger CI
- Disable cache: remove `cache: maven` line
- Change verification goal: update `mvn -B verify` command

---

## Release Checklist

Before cutting a release:

- [ ] All tests pass: `mvn test`
- [ ] App compiles: `mvn compile`
- [ ] App runs: `mvn spring-boot:run` (test manually)
- [ ] Manual smoke test:
  - [ ] POST /auth/signup → returns JWT
  - [ ] POST /auth/login → returns JWT
  - [ ] Invalid credentials → 401
  - [ ] Duplicate email → 409
- [ ] Verify passwords are hashed (BCrypt in DB, never in response)
- [ ] Verify JWT token is valid (decode on jwt.io)
- [ ] No secrets in code (`grep -r "password=\|secret=" src/`)
- [ ] README updated (if behavior changed)
- [ ] Docs updated (ARCHITECTURE.md, API_CONTRACT.md, etc.)
- [ ] Git tags created: `git tag -a v0.1.0 -m "Release 0.1.0"`

---

## Common Commands

| Command | Purpose |
|---------|---------|
| `mvn clean` | Remove target/ directory |
| `mvn compile` | Compile source code |
| `mvn test` | Run all tests |
| `mvn package` | Compile + test + create JAR |
| `mvn spring-boot:run` | Run app in development mode |
| `mvn help:describe -Dplugin=org.springframework.boot:spring-boot-maven-plugin` | Maven plugin docs |

---

## Troubleshooting

### Port 8080 Already in Use
```bash
# Find process on port 8080
lsof -i :8080                 # macOS/Linux
netstat -ano | findstr :8080  # Windows

# Kill process
kill <PID>                     # macOS/Linux
taskkill /PID <PID> /F        # Windows
```

### Maven Dependency Issues
```bash
mvn clean dependency:resolve
mvn clean install -U          # Update snapshots
```

### Tests Failing with H2 Lock Errors
- Stop any running app: `mvn spring-boot:stop`
- Clear H2 temp files: `rm -rf target/classes/.h2*`
- Run tests again: `mvn test`

### JWT Secret Too Short
Error: `JWT signing key is too weak`

Fix: Ensure `app.jwt.secret` is at least 32 characters in `application.properties`.

### Authentication Filter Not Working
- Verify filter chain in `SecurityConfig`
- Check `JwtAuthenticationFilter` is registered
- Verify token is in `Authorization: Bearer <token>` header (case-sensitive)

---

## Monitoring & Observability

### Log Levels
- **DEBUG**: Low-level request/response details (dev only)
- **INFO**: Application events, startup messages
- **WARN**: Potential issues (deprecated APIs, missing configs)
- **ERROR**: Failures requiring attention

Configure in `application.properties`:
```properties
logging.level.com.authplatform=INFO
```

### Structured Logging
Use SLF4J (already included):
```java
log.info("User signup", "email={}", email);
log.error("Signup failed", "error={}", exception.getMessage());
```

Never log passwords or tokens.

---

## Incidents

### Service Down
1. Check app is running: `curl http://localhost:8080/auth/signup`
2. Check logs: `mvn spring-boot:run` (stdout)
3. Check database: H2 console or DB connection string
4. Restart: `mvn spring-boot:stop && mvn spring-boot:run`

### High Error Rate
1. Check logs for exceptions
2. Verify database is accessible
3. Verify JWT secret is correct
4. Check load balancer / reverse proxy (if in production)

### Performance Degradation
1. Check DB query performance: `SHOW SLOW QUERIES` (if using MySQL)
2. Monitor BCrypt iterations: currently Spring Security default (~10)
3. Check for memory leaks: `jmap -heap <PID>`
4. Profile with JProfiler or YourKit if available

---

## Rollback

If a deployment fails:

1. **Immediate rollback**: Deploy previous working JAR
   ```bash
   git checkout <previous-commit>
   mvn clean package
   java -jar target/auth-platform-*.jar
   ```

2. **Database rollback**: 
   - If using Flyway/Liquibase: `mvn flywayundo` (set undo back to working state)
   - If using Hibernate auto-DDL: manual SQL to revert schema changes

3. **Notify stakeholders**: Explain incident and ETA for fix

---

## Deployment to Production

1. **Build JAR:**
   ```bash
   mvn clean package -DskipTests -Pnoprod
   ```

2. **Tag release:**
   ```bash
   git tag -a v0.1.0 -m "Release 0.1.0"
   git push origin v0.1.0
   ```

3. **Deploy to server:**
   ```bash
   scp target/auth-platform-0.0.1-SNAPSHOT.jar user@server:/opt/app/
   ssh user@server "java -jar /opt/app/auth-platform-0.0.1-SNAPSHOT.jar \
     --spring.profiles.active=prod \
     --app.jwt.secret=$JWT_SECRET \
     --spring.datasource.url=$DB_URL"
   ```

4. **Verify deployment:**
   ```bash
   curl https://api.example.com/auth/signup   # Should return 400 (bad request)
   ```

5. **Monitor logs:** Check centralized logging (CloudWatch, ELK, DataDog)
