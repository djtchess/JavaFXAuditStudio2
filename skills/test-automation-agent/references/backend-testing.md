# Backend Testing

Use this reference when the target is Java/Spring Boot.

## Priorities

1. Cover `domain` and `application` with TU that run without Spring.
2. Cover adapters-in/out with TI only when the adapter boundary is meaningful.
3. Assert architectural rules when the project already uses dedicated checks; do not invent a second architecture contract.

## Patterns

- Test one use case per class where practical.
- Mock output ports in application tests.
- Test REST mapping, validation, and error translation at controller/integration level.
- Keep fixtures explicit and small.

## Blockers

- If Maven wrapper or build chain is broken, document the blocker and still prepare compilable test skeletons where possible.
- Do not move business logic into adapters to make tests easier.
