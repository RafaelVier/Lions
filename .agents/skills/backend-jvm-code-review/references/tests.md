# Test Review Reference

Use this reference when the review asks whether the tests really catch the change, whether integration tests are missing, or whether important branches and returns remain uncovered.

## What to look for

- unit tests that do not fail when the changed behavior is broken
- branch-heavy code with missing assertions for final returns, guard clauses, and fallback logic
- framework-dependent changes covered only by unit tests
- repository/query/migration changes without integration coverage
- endpoint changes without RestAssured, MockMvc, WebTestClient, or controller integration tests
- tests that assert interactions/mocks but not observable business outcomes
- coverage that looks good numerically but misses the risky decision paths

## Review questions

1. What exact regression is most likely here?
2. Which test layer should catch it: unit, integration, or HTTP/API?
3. Do current unit tests assert outcomes or only mocked interactions?
4. Are new branches, guard clauses, and final returns explicitly asserted?
5. If JPA, serialization, validation, or controller wiring changed, is integration coverage present?
6. If the endpoint changed, is there API-level verification for status and payload?

## Strong finding patterns

### Weak unit protection
- **Risk:** the business rule can break while tests still pass because the suite only verifies collaborator calls
- **Proposed solution:** add focused unit tests asserting final outcome, returned value, persisted state, or emitted event

### Missing branch coverage
- **Risk:** a new `if/else`, guard clause, or fallback return can regress unnoticed
- **Proposed solution:** add targeted tests for each important decision path, especially final returns and error branches

### Missing integration coverage
- **Risk:** unit tests cannot detect broken JPQL/native queries, transaction behavior, validation binding, or serialization wiring
- **Proposed solution:** add repository/service/controller integration tests for the changed path with realistic data

### Missing API coverage
- **Risk:** endpoint contract changes pass service tests but still break status code, payload shape, validation error body, or headers
- **Proposed solution:** add RestAssured, MockMvc, or WebTestClient coverage for the affected endpoint behavior

### False confidence tests
- **Risk:** high coverage or many tests still miss real regressions because assertions are too weak or over-mocked
- **Proposed solution:** reduce mock dependence, strengthen assertions around business outcomes, and cover the real failure mode

## Category suggestions

Use categories such as:
- unit-test-gap
- integration-gap
- api-test-gap
- branch-coverage
- test-quality
- coverage-false-confidence

## Priority hints

- **CRITICAL**: high-probability escaped regression in a critical path with no effective automated protection
- **HIGH**: important behavior can regress while current tests still pass
- **MEDIUM**: meaningful confidence gap or weak coverage that should be improved soon
- **LOW**: hardening opportunity or assertion improvement

## Solution style

Always propose a practical fix, such as:
- add unit test for final outcome or decision path
- add integration test for repository/query/serialization flow
- add RestAssured or controller-level API test
- replace interaction-only assertions with behavior assertions
- cover null, empty, fallback, and exceptional paths
