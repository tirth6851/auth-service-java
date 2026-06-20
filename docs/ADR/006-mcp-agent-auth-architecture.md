# ADR-006: MCP / Agent-Auth Architecture Note

**Date**: 2026-06-19  
**Status**: Proposed (no code changes this session)

## Context

AI agents (LLMs with tool-calling) need to authenticate to APIs autonomously, without a human entering credentials in a browser. The Model Context Protocol (MCP) defines a standard interface for exposing capabilities as agent-callable tools. This ADR documents how the current REST auth service relates to a future MCP layer and what constraints today's decisions impose on that future.

## Decision

### Dual-mode architecture

The REST auth service is the source of truth. An MCP server wraps it as an agent-callable tool surface.

```
Agent (LLM)
    │
    ▼  MCP tool calls
MCP Auth Server  (adapter, no business logic)
    │
    ▼  HTTP requests
REST Auth Service  (source of truth)
```

The MCP server translates tool invocations to HTTP. It does not replicate the user table, JWT signing, or password hashing.

### How this sprint's decisions affect agent auth

| Decision made | Agent-auth impact |
|---|---|
| Refresh tokens (7-day TTL) | Agents can maintain long-running sessions across access-token expiry — critical for unattended workflows that outlast 1-hour JWTs |
| Token rotation on refresh | Agents should store and use the **new** refresh token returned by each `/auth/refresh` call; reusing an old one triggers revocation |
| `POST /auth/logout` revokes refresh token | Agents can explicitly clean up sessions when a workflow ends — important in multi-agent systems where many concurrent sessions accumulate |
| `POST /auth/refresh` returns new pair | MCP layer can auto-refresh before expiry, transparent to the agent using higher-level tools |
| SHA-256 hashed storage | Internal implementation detail; no agent-visible impact |
| CORS configured | Not needed for server-to-server MCP; useful if agents run browser-side (uncommon) |

### Future work (out of scope today)

1. **API keys for service accounts** — Long-lived opaque keys (`Authorization: ApiKey <key>`) for non-interactive agents. Eliminates JWT bootstrap; keys stored hashed. Implement separately.
2. **Scope claims in JWT** — Today's JWT has no scope. Add `"scopes": ["data:read"]` claim when implementing RBAC. Agents request minimal scope at login.
3. **MCP tool naming** — `login` → `auth_login`, `refresh_token` → `auth_refresh`, following MCP snake_case naming convention.
4. **MCP session state** — The MCP server holds the current token pair in session context; agents invoke `auth_refresh` implicitly or explicitly before any protected tool call.

## Alternatives considered

- **Agent uses OAuth2 client credentials** — more standard; adds OAuth2 server infra. Deferred.  
- **MCP server manages its own identity store** — violates single-source-of-truth principle. Rejected.

## Consequences

No code changes in this sprint. This ADR establishes architectural intent and constraints to guide future MCP integration work.

## Where to Start (for contributors)

The REST auth service is the source of truth and does not change. A future MCP server is a **separate project** that wraps it:

1. Create a new repository (e.g. `auth-mcp-server`) using the MCP SDK for your language
2. Implement tool functions that call this service's HTTP endpoints
3. The MCP server holds the token pair in session context and auto-refreshes before expiry
4. No user table, no password hashing, no JWT signing in the MCP layer

**Suggested first MCP tools:**

| MCP tool name | HTTP call |
|---------------|-----------|
| `auth_login` | `POST /auth/login` |
| `auth_refresh` | `POST /auth/refresh` |
| `auth_logout` | `POST /auth/logout` |
| `auth_validate` | Any protected endpoint; 401 = expired |

See `docs/OBSERVABILITY.md` for what to monitor when agents are running concurrent sessions.

## Relationship to README

The README Roadmap section lists "MCP adapter layer" as a future Priority 3 item with a pointer here. Keep this ADR as the single source for MCP architecture decisions — do not duplicate the dual-mode diagram in multiple places.
