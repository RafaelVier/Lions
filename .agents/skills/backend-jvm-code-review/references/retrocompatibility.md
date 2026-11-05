# Backward Compatibility Review Reference

Use this reference when the change touches migrations, entities, JPA/Hibernate, JsonB/JSON columns, shared DTOs, validations, nullability, rollout strategy, or critical endpoints.

## What to look for

- migrations that rename, drop, or constrain columns without compatibility steps
- JPA mappings that no longer match schema or legacy data
- projections, JPQL, native queries, and selects that were not updated
- JsonB/JSON payload shape changes that break stored or historical data
- new non-null assumptions over nullable database or JSON fields
- validations that clean up the code but reject legacy records
- changes to shared DTOs or entities that affect unreviewed endpoints
- risky changes to critical flows without feature toggle or staged rollout

## Review questions

1. Can the new code run safely against old data?
2. Can the old app and new schema coexist during deployment?
3. Did all queries, projections, serializers, and mappers adapt to the change?
4. Will legacy rows with `null`, blank, malformed, or partial data still work?
5. Does this affect endpoints or consumers not touched in the PR?
6. Should this be guarded by feature toggle or gradual rollout?

## Strong finding patterns

### Unsafe migration
- **Risk:** new `NOT NULL`, type change, rename, or dropped column breaks running code or legacy rows
- **Proposed solution:** add backfill, compatibility phase, dual-read/write, or staged migration before enforcing the new constraint

### JPA/query mismatch
- **Risk:** entity, projection, constructor, or native query still assumes the old schema and fails only at runtime
- **Proposed solution:** update mappings and queries together, add integration coverage for the affected query path, and verify select/projection alignment

### JsonB/legacy data breakage
- **Risk:** migration succeeds but deserialization or validation fails when historical rows are read
- **Proposed solution:** make readers tolerant to old payload shapes, backfill the data, or version the JSON contract before enforcing stricter parsing

### Nullability regression
- **Risk:** Kotlin/Java code now dereferences fields that remain nullable in production data
- **Proposed solution:** keep compatibility guards, add null-safe handling, and only tighten nullability after production data is cleaned

### Missing rollout protection
- **Risk:** a critical endpoint changes behavior with no toggle, making rollback operationally expensive
- **Proposed solution:** introduce feature toggle, dark launch, staged rollout, or write-compatible transition phase

## Category suggestions

Use categories such as:
- migration
- JPA
- JsonB
- query
- nullability
- breaking-change
- rollout
- test-gap

## Priority hints

- **CRITICAL**: likely production breakage, mixed-version deploy failure, critical endpoint regression, or high-blast-radius compatibility gap
- **HIGH**: strong runtime breakage risk through queries, mappings, validations, or legacy data
- **MEDIUM**: important compatibility hardening or rollout gap
- **LOW**: lower-probability but worthwhile compatibility guard

## Solution style

Always propose a practical fix, such as:
- phased migration
- backfill or cleanup script
- dual-read/dual-write strategy
- query/projection alignment
- null-safe compatibility layer
- tolerant serializer/converter
- feature toggle and staged rollout
- integration test against realistic legacy data
