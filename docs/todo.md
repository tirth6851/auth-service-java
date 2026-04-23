# Phase 1 — Task List

## Done (implementation complete)

- [x] Spring Boot project structure and pom.xml
- [x] User entity with BCrypt-hashed password storage
- [x] SignupRequest / LoginRequest / AuthResponse DTOs with Bean Validation
- [x] UserRepository (Spring Data JPA)
- [x] AuthService — signup (409 on duplicate, email normalised) and login (401 on bad credentials)
- [x] JwtUtil — HS256 token generation and parsing
- [x] JwtAuthenticationFilter — Bearer token → SecurityContext
- [x] SecurityConfig — stateless, CSRF off, /auth/** public
- [x] application.properties — H2, JWT config
- [x] CLAUDE.md
- [x] README.md
- [x] `src/test/java/com/authplatform/security/JwtUtilTest.java`
- [x] `src/test/java/com/authplatform/service/AuthServiceTest.java`
- [x] `src/test/java/com/authplatform/controller/AuthControllerIntegrationTest.java`
- [x] `docs/spec.md`
- [x] `docs/done-criteria.md`

## Remaining

None — Phase 1 complete.
