# Maintainability Review Reference

Use this reference when the review asks whether the code is readable, cohesive, aligned with project patterns, or too complex to evolve safely.

## What to look for

- methods that are too long or branch-heavy
- nested conditionals, many temporary variables, and tangled orchestration
- vague names, spelling mistakes, inconsistent domain vocabulary
- services or classes doing more than their name suggests
- controllers, repositories, mappers, or helpers owning the wrong responsibilities
- code and tests diverging from local project patterns without a good reason
- weak DDD boundaries, scattered domain rules, and accidental coupling
- changes that require touching 3-4 places together for one business rule

## Review questions

1. Is the method still easy to understand in one pass?
2. Does the class name match what it actually does?
3. Are names precise enough to explain domain intent?
4. Is business logic living in the right layer?
5. Does this create a second unofficial pattern in the codebase?
6. If someone changes this rule again, how many files will likely move together?

## Strong finding patterns

### Complex method
- **Risk:** new changes become error-prone because branching, persistence, mapping, and validation are interleaved
- **Proposed solution:** extract focused private methods or move business rules into a domain/service object with one clear responsibility

### Responsibility drift
- **Risk:** a service now orchestrates, validates, maps, and persists, making ownership unclear
- **Proposed solution:** separate orchestration from business policy and mapping; keep each class aligned with its stated responsibility

### Poor naming
- **Risk:** future reviewers cannot safely infer intent from names like `data`, `obj`, `item2`, or misspelled business terms
- **Proposed solution:** rename to domain-specific terms and align vocabulary across service, DTO, tests, and repository methods

### Pattern inconsistency
- **Risk:** future contributors must special-case this file because it ignores the dominant local style for tests, error handling, or layering
- **Proposed solution:** refactor toward the nearby module pattern unless there is a documented reason to diverge

### High coupling
- **Risk:** one rule change forces coordinated edits across controller, service, mapper, repository, and tests
- **Proposed solution:** centralize the rule, introduce a single owner for the concept, and reduce duplication of conditionals/constants

## Category suggestions

Use categories such as:
- complexity
- naming
- spelling
- responsibility
- standards
- clean-code
- DDD
- coupling
- tests

## Priority hints

- **CRITICAL**: dangerous coupling or design drift likely to cause repeated breakages across multiple modules
- **HIGH**: strong complexity or responsibility problem that makes future edits risky
- **MEDIUM**: meaningful maintainability debt or pattern deviation
- **LOW**: cleanup or clarity improvement worth addressing

## Solution style

Always propose a practical fix, such as:
- extract method/class
- move rule to domain/service with clear ownership
- rename variables/methods/classes to domain terms
- align with existing module pattern
- reduce coupling by centralizing duplicated logic
- simplify tests to assert behavior instead of implementation details
