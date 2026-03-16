# Frontend Testing

Use this reference when the target is Angular.

## Priorities

1. Cover facades, services, and forms with TU.
2. Cover critical user paths with component/integration tests.
3. Validate loading, empty, and error states explicitly.

## Patterns

- Mock the centralized API adapter, not ad hoc HTTP calls.
- Keep component tests focused on rendered behavior and form interactions.
- Use integration tests for route-to-screen flows only when the path is user-critical.

## Blockers

- If the frontend build is green but test tooling is absent or unstable, document the gap and leave a clear next step.
- Do not encode backend business rules exclusively in frontend tests.
