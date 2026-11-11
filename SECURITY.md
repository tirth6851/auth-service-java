# Security Policy

## Status: unaudited

This project has **not** had an independent security audit. It follows common practice (BCrypt password
hashing, hashed and rotated refresh tokens with reuse detection, rate limiting, no secrets in code), but
that is not a guarantee. Review it yourself before putting it in front of real users, and do not treat
it as a drop-in replacement for a maintained identity provider.

## Supported versions

There are no tagged releases yet. Only the `main` branch receives security fixes.

## Reporting a vulnerability

**Please do not open a public issue, discussion, or pull request for a security problem.**

Use GitHub's private reporting:

1. Go to the [Security tab](https://github.com/tirth6851/auth-service-java/security) of this repository.
2. Choose **Report a vulnerability** (or open
   [a new advisory directly](https://github.com/tirth6851/auth-service-java/security/advisories/new)).

If you cannot use GitHub, email `tirth2093@gmail.com` with the subject line `SECURITY: auth-service-java`.

Please include:

- what you found and which endpoint, file, or configuration is affected
- steps or a proof of concept to reproduce it
- the impact you believe it has
- the commit hash or branch you tested

## What to expect

This is maintained by one person, so the targets below are deliberately modest and ones I can keep:

| Step | Target |
|------|--------|
| Acknowledge your report | within 3 business days |
| Initial assessment (accepted, need more info, or not a vulnerability) | within 10 business days |
| Fix or mitigation for a confirmed issue | best effort, usually within 90 days |

I will keep you updated, credit you in the fix and advisory unless you prefer to stay anonymous, and ask
you to hold public disclosure until a fix is available or 90 days have passed, whichever comes first.
There is no bug bounty.

## Scope

In scope:

- the Spring Boot service in `src/` (authentication, token handling, rate limiting, error responses)
- the Auth Portal in `web/` (session handling, BFF routes)
- the deployment files (`Dockerfile`, `docker-compose.yml`, `render.yaml`) when they cause an insecure default

Out of scope:

- findings that need an already-compromised host, database, or `JWT_SECRET`
- denial of service by sheer traffic volume (a per-IP rate limit exists but is not a DDoS defence)
- missing features that are documented as gaps (see below)
- vulnerabilities in third-party dependencies that are already tracked upstream; report those to the
  dependency instead, though a heads-up here is welcome

## Known limitations

These are documented gaps, not undisclosed bugs. See [README](README.md#security-notes) and
[`docs/ADR/`](docs/ADR/) for the reasoning.

- **HS256 is symmetric.** Anyone holding `JWT_SECRET` can both verify and forge access tokens.
- **Access tokens cannot be revoked before they expire.** Production uses a 15-minute lifetime to limit
  this; refresh tokens are revocable and rotated.
- **Rate limiting is per instance and keyed on the socket address** (`getRemoteAddr()`, deliberately not
  the forgeable `X-Forwarded-For`). Behind a reverse proxy every client shares the proxy's address, and
  running several replicas multiplies the effective limit.
- **No TLS in the application.** Terminate TLS at a proxy or load balancer.
- **No password reset, email verification, or account lockout yet.**
