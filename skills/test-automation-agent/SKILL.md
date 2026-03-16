---
name: test-automation-agent
description: Create or update a reusable agent workflow for generating frontend and backend unit tests and integration tests. Use when Codex needs a repeatable process to add or maintain TU/TI coverage across Angular frontend and Spring Boot hexagonal backend without inventing contracts or breaking architecture rules.
---

# Test Automation Agent

Create tests only after reading the project contracts, the relevant production code, and the current orchestration rules.

## Workflow

1. Read `AGENTS.md`, `agents/orchestration.md`, `agents/contracts.md`, and the target code before proposing tests.
2. Distinguish backend and frontend responsibilities.
3. Prefer unit tests for domain/application logic and focused integration tests for adapters, API boundaries, and UI flows.
4. Keep contracts stable: tests consume the validated API and architecture, they do not redefine them.
5. Report facts, hypotheses, and test gaps explicitly when code is not testable yet.

## Backend Focus

- Verify hexagonal boundaries first.
- Create TU for `domain` and `application` without Spring whenever possible.
- Create TI for REST adapters, configuration wiring, and persistence adapters only when the infrastructure path exists.
- Flag blockers when wrapper/build tooling is broken instead of faking coverage.

See [references/backend-testing.md](references/backend-testing.md) when generating backend tests.

## Frontend Focus

- Cover reactive forms, error states, loading states, routing, facade behavior, and contract usage.
- Prefer isolated component/service tests first, then integration-style tests for critical user flows.
- Keep API calls centralized and mock the API adapter rather than duplicating HTTP logic in tests.

See [references/frontend-testing.md](references/frontend-testing.md) when generating frontend tests.

## Output

Produce:

- the test files or agent specification requested;
- a short list of covered risks;
- blockers and residual gaps if execution is partial.
