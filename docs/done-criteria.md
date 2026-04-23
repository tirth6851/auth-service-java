# Phase 1 — Done Criteria

- [x] `mvn compile` passes with no errors
- [x] `mvn spring-boot:run` starts successfully on port 8080
- [x] `POST /auth/signup` with valid body returns 200 + JWT
- [x] `POST /auth/signup` duplicate email returns 409
- [x] `POST /auth/signup` invalid email or short password returns 400
- [x] `POST /auth/login` with valid credentials returns 200 + JWT
- [x] `POST /auth/login` with wrong password returns 401
- [x] `POST /auth/login` with unknown email returns 401
- [x] Passwords stored as BCrypt hash, never plaintext
- [x] `mvn test` passes — 17 tests (5 JwtUtil, 5 AuthService, 7 AuthController)
- [x] README includes run instructions, endpoints, and sample requests
