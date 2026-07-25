package com.authplatform.integration;

import com.authplatform.dto.AuthResponse;
import com.authplatform.dto.LoginRequest;
import com.authplatform.dto.SignupRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real-Postgres integration test.
 *
 * <p>Unlike the rest of the suite (which runs against H2, see {@code src/test/resources/application.properties}),
 * this test spins up an actual PostgreSQL container, applies every Flyway migration in
 * {@code src/main/resources/db/migration} in order, and does a signup + login smoke check through the
 * real repository/service/controller stack. H2 is lenient about SQL that Postgres would reject
 * (e.g. Postgres-specific types, syntax, or constraint behavior), so this test is the only place that
 * validates the migrations actually run cleanly against the production database engine.</p>
 *
 * <p>Tagged {@code integration} and excluded from the default {@code mvn test} / {@code mvn verify} run
 * (see the {@code excludedGroups} config in pom.xml's surefire plugin) because it requires a running
 * Docker daemon and is slower than the H2-backed suite. Run it explicitly with:</p>
 *
 * <pre>mvn -Pintegration-test verify</pre>
 */
@Tag("integration")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PostgresFlywayIntegrationTest {

    // Matches the Postgres version used in production (see docker-compose.yml: postgres:16-alpine).
    @Container
    static PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("authdb_it")
            .withUsername("authuser")
            .withPassword("authpass");

    @BeforeAll
    static void startContainer() {
        POSTGRES.start();
    }

    @AfterAll
    static void stopContainer() {
        POSTGRES.stop();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        // Let Flyway run all migrations (V1, V2, ...) against the real Postgres container,
        // then have Hibernate validate the resulting schema rather than generate it.
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DataSource dataSource;

    @Test
    void flywayMigrationsApplyCleanlyAgainstRealPostgres() throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            // Flyway's own bookkeeping table proves migrations ran (and V1/V2 both succeeded,
            // since a failed migration would have aborted startup before this test runs).
            try (ResultSet rs = statement.executeQuery(
                    "SELECT version, success FROM flyway_schema_history ORDER BY installed_rank")) {
                boolean sawV1 = false;
                boolean sawV2 = false;
                while (rs.next()) {
                    assertThat(rs.getBoolean("success")).isTrue();
                    String version = rs.getString("version");
                    if ("1".equals(version)) sawV1 = true;
                    if ("2".equals(version)) sawV2 = true;
                }
                assertThat(sawV1).as("V1__create_users_table migration ran").isTrue();
                assertThat(sawV2).as("V2__create_refresh_tokens_table migration ran").isTrue();
            }

            // Confirm the tables Flyway created actually exist with the expected shape.
            try (ResultSet rs = statement.executeQuery(
                    "SELECT column_name FROM information_schema.columns WHERE table_name = 'users'")) {
                assertThat(rs.next()).as("users table exists").isTrue();
            }
            try (ResultSet rs = statement.executeQuery(
                    "SELECT column_name FROM information_schema.columns WHERE table_name = 'refresh_tokens'")) {
                assertThat(rs.next()).as("refresh_tokens table exists").isTrue();
            }
        }
    }

    @Test
    void signupThenLogin_againstRealPostgres_succeeds() {
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setEmail("pg-smoke-test@example.com");
        signupRequest.setPassword("Str0ngPass!23");

        ResponseEntity<AuthResponse> signupResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/auth/signup", signupRequest, AuthResponse.class);

        assertThat(signupResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(signupResponse.getBody()).isNotNull();
        assertThat(signupResponse.getBody().getToken()).isNotBlank();

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("pg-smoke-test@example.com");
        loginRequest.setPassword("Str0ngPass!23");

        ResponseEntity<AuthResponse> loginResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/auth/login", loginRequest, AuthResponse.class);

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResponse.getBody()).isNotNull();
        assertThat(loginResponse.getBody().getToken()).isNotBlank();
    }
}
