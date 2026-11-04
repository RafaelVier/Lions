---
name: backend-jvm-code-review
description: "Unified backend JVM code review skill for PR review or code review across whole repositories, modules, or snippets. Use this skill whenever the user asks for PR review, code review, repo-wide review, migration risk analysis, maintainability analysis, backward-compatibility review, test-gap review, or performance review in Kotlin/Java/JVM backends with Spring, JPA/Hibernate, SQL databases, REST APIs, messaging, or integration-heavy services. Triggers on: PR review, pull request review, code review, review this diff, review this repository, migration review, maintainability, clean code, DDD, coupling, naming, backward compatibility, retrocompatibility, breaking change, JPA, JsonB, Flyway, Liquibase, nullability, test coverage, unit tests, integration tests, RestAssured, performance, scalability, slow query, N+1, Hikari, Kafka, SQS."
---

# Backend JVM Review Expert

A unified review skill for backend JVM systems focused on PR review and code review across entire repositories, modules, or selected code snippets.

This skill is optimized for a user who works with Kotlin/Java/JVM backends and wants high-signal reviews that go beyond the changed lines. The expected context is production code with shared entities, partial test coverage, database coupling, legacy data, and critical endpoints.

## User Context

Assume the user usually wants one of these review modes:

- **PR review**: analyze the diff, then inspect nearby usages and blast radius
- **Repository review**: inspect architecture or risky areas across the codebase
- **Partial review**: inspect a service, module, file set, or selected code snippet

The review should stay focused on four verticals:

1. maintainability
2. backward compatibility
3. tests
4. performance

## Quick Reference

| Category | When to Use |
| --- | --- |
| [Maintainability](references/maintainability.md) | Complex methods, naming, spelling, responsibility drift, clean code, DDD, coupling, change complexity |
| [Backward Compatibility](references/retrocompatibility.md) | Migrations, JPA/Hibernate, JsonB, nullability, legacy data, rollout risk, feature toggles, hidden breakages |
| [Tests](references/tests.md) | Unit test quality, integration gaps, branch/decision coverage, RestAssured, MockMvc, regression safety |
| [Performance](references/performance.md) | Slow queries, N+1, transactions, connection pools, memory, messaging, coroutines, scalability |

## Core Review Rules

### 1. Review beyond the diff

Do not stop at the changed file. Look for usages, shared contracts, callers, consumers, repositories, tests, and integration points.

### 2. Use the right verticals

Load only the reference files relevant to the request. For broad PR review, usually inspect at least maintainability, backward compatibility, and tests. Add performance when the change touches queries, transactions, loops, messaging, caching, concurrency, or throughput-sensitive flows.

### 3. Categorize all findings by priority

Always use this priority system:

- **[CRITICAL]**: likely production breakage, major regression, critical-path outage risk, severe hidden coupling, or escaped bug with high blast radius
- **[HIGH]**: strong likelihood of runtime failure, regression, unsafe design drift, or missing protection in an important flow
- **[MEDIUM]**: meaningful risk, maintainability debt, or validation/test gap that should be addressed soon
- **[LOW]**: preventive hardening, cleanup, or low-probability but worthwhile improvement

### 4. Always propose solutions

Never stop at commentary. Every finding must include a concrete proposed solution, mitigation, refactor, rollout strategy, or test to add.

### 5. Favor concrete risk statements

Strong review comments sound like: "this change can break this runtime path for this reason" or "future changes here will require touching these 3-4 places because the logic is fragmented."

## Suggested Review Flow

### For PR review

1. Read the diff
2. Identify which of the 4 verticals apply
3. Load the relevant reference files
4. Inspect blast radius outside the touched files
5. Produce prioritized findings with proposed solutions
6. End with the priority table

### For repository or module review

1. Identify the review scope
2. Map risky areas by vertical
3. Read only the necessary reference files
4. Focus on hotspots, repeated patterns, and systemic issues
5. Produce prioritized findings with proposed solutions
6. End with the priority table

## Final Output Contract

Always end the review with this structure:

### Summary
- Overall review risk: low / medium / high
- Main concerns in 1-3 bullets

### Priority table
Always include a final table summarizing all findings.

| Priority | Vertical | Risk / Issue | Impacted surface | Proposed solution |
| --- | --- | --- | --- | --- |
| CRITICAL/HIGH/MEDIUM/LOW | Maintainability / Backward Compatibility / Tests / Performance | ... | ... | ... |

### Detailed findings
For each finding:
- **[CRITICAL/HIGH/MEDIUM/LOW] Title**
- **Vertical:** maintainability / backward compatibility / tests / performance
- **Category:** ...
- **Risk / Issue:** ...
- **Why it matters:** ...
- **Impacted surface:** ...
- **Proposed solution:** ...
- **Trade-offs / notes:** ...

### Additional checks performed
- Brief list of what was inspected beyond the changed lines

### Residual risk
- What still cannot be validated with the available context

If no relevant findings exist, still include the priority table with a single row stating that no material issues were identified.

## Reference Files

Use the reference files below as needed:

- [references/maintainability.md](references/maintainability.md)
- [references/retrocompatibility.md](references/retrocompatibility.md)
- [references/tests.md](references/tests.md)
- [references/performance.md](references/performance.md)
