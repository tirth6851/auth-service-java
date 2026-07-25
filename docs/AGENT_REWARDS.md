# Agent Task Rewards — 2026-07-25 backlog sweep

Scorecard for the 4 agents dispatched in parallel (worktree-isolated) to clear the outstanding
backlog from `docs/HANDOFF.md`. Reward = 1.0 for a fully correct, verified, merged result; partial
credit for correct-but-incomplete work; 0 for anything that broke the build or was discarded.

| Agent | Task | Verification | Reward |
|---|---|---|---|
| `a6844e1525d828e03` | Rate-limit `POST /auth/signup` | Reused existing Bucket4j pattern correctly, separate bucket per endpoint (verified with an explicit independence test), +4 tests, merged cleanly onto branch tip, 72/72 passing post-merge | **1.0** |
| `a264f1e0c9e05144d` | `LOWER(email)` index migration | Did not blindly add a duplicate migration — verified the index already existed in `V1`, traced why (checked repository/service normalization logic and `PROJECT_DECISIONS.md` D-008 before deciding not to change query semantics), corrected 3 stale docs instead. Correct judgment call, no code risk introduced | **1.0** |
| `aa93cbb6085773451` | Testcontainers Postgres integration test | Test is well-scoped (`@Tag("integration")`, Docker-gated via Maven profile, doesn't slow default `mvn test`), compiled and merged cleanly, existing suite unaffected (68→72 later). Honestly flagged that it couldn't execute the new test itself (no Docker daemon in this environment) rather than claiming false verification | **0.9** — full credit withheld only because the new test's actual execution is still unverified pending a Docker-enabled run |
| `a8c4d211f9e9b94dc` | `render.yaml` Render blueprint | Matched `docs/DEPLOYMENT.md` field-for-field, correctly refused to fabricate values Render's blueprint spec can't actually auto-derive (JDBC URL format, `https://` scheme concatenation) and left those as flagged manual/`sync:false` steps instead of guessing | **1.0** |

**Process note:** two of the four worktrees (`a264f1e0c9e05144d`, `aa93cbb6085773451`) initially branched
from a stale point in history missing the refresh-token hardening + Portal M1 commits (46 tests instead
of 68). This was caught via the test-count mismatch before merging, each worktree was synced to the
branch tip and re-verified, and no incorrect base state reached `claude/auth-portal-m1`.
