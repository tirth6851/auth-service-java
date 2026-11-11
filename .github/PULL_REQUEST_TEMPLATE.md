## Summary

<!-- What does this PR do? One or two sentences. Link the issue if one exists: "Closes #NNN" -->

## Changes

<!-- List the files changed and why. Be specific: "Added X because Y", not "Various changes". -->

- 

## Test plan

<!-- Describe how you tested this. Include test class names for new tests. -->

- [ ] `mvn test` passes (all N tests green)
- [ ] New or modified behaviour has test coverage
- [ ] Manual smoke test (if applicable):
  ```bash
  # Show the curl commands you ran
  ```

## Docs updated

<!-- Tick every doc you updated in this PR. Unticked = not affected. -->

- [ ] `docs/API_CONTRACT.md` — if endpoint behaviour changed
- [ ] `docs/ARCHITECTURE.md` — if layer or flow changed
- [ ] `docs/ENVIRONMENTS.md` — if new config property added
- [ ] `docs/RUNBOOK.md` — if build/deploy steps changed
- [ ] `README.md` — if public-facing behaviour changed
- [ ] ADR created in `docs/ADR/` — if this is an architectural decision

## Security checklist

- [ ] No secrets hardcoded (passwords, JWT keys, API keys)
- [ ] No passwords or raw tokens logged
- [ ] Error responses do not leak internal details
- [ ] If touching the auth flow, tokens, or error responses: re-read the affected code for security impact and note anything non-obvious in the PR description

## Contribution checklist

- [ ] Every commit is signed off (`git commit -s`), per the [DCO section of CONTRIBUTING.md](../blob/main/CONTRIBUTING.md#developer-certificate-of-origin-dco)
- [ ] I have read and can explain every line of this change
- [ ] A substantial part of this PR was AI-generated (tick if yes, and say which part above)

## Breaking changes

<!-- Does this change existing API contracts, config property names, or DB schema? -->

- [ ] Yes — describe what breaks and how to migrate:
- [ ] No
