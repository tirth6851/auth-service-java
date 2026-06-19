# Product Requirements Document (PRD)

## Overview

Auth Platform Phase 1 is a standalone authentication service providing user signup and login via JWT tokens. It is the foundational auth layer for future multi-tenant SaaS applications.

## Goals

1. **User registration** with email, secure password storage (BCrypt), and automatic JWT issuance
2. **User login** with email/password validation and JWT issuance
3. **Stateless JWT validation** for downstream protected APIs
4. **Consistent error responses** in JSON with appropriate HTTP status codes
5. **Zero external dependencies** for auth (no OIDC, SAML, or SSO in Phase 1)

## Scope

- POST /auth/signup: register user, return JWT token
- POST /auth/login: validate credentials, return JWT token
- JWT structure: HS256, subject = user ID, custom email claim, configurable expiry
- Password hashing: BCrypt with Spring Security
- HTTP error responses: 400 (bad request), 401 (unauthorized), 409 (conflict), 500 (server error)
- H2 in-memory database for local development

## Non-Goals (Phase 1)

- Email verification or OTP
- Refresh token mechanism
- Role-based authorization or permissions
- API keys or service accounts
- Multi-factor authentication
- OAuth2 / OpenID Connect providers
- Social login (Google, GitHub, etc.)
- Scheduled token cleanup or revocation lists
- Audit logging or compliance reporting
- Custom user attributes beyond email

## Success Criteria

- All unit and integration tests pass
- Signup and login endpoints return valid JWT tokens
- Invalid credentials return 401
- Duplicate email returns 409
- Passwords are never exposed in responses or logs
- API contract is documented and stable
- All code follows layered architecture rules
- Security review confirms no secrets in codebase

## Timeline

Phase 1: MVP complete when all success criteria met. No fixed deadline; driven by feature completeness.

## Assumptions

- Users authenticate with email/password only (no username)
- One user per email address
- JWT secret is at least 32 characters and stored securely
- Token expiry is configurable and defaults to 1 hour
- Database resets on application restart (in-memory, Phase 1 only)
