# Performance Review Reference

Use this reference when the review asks whether a code change can hurt latency, throughput, memory, connection usage, scalability, or production resilience.
This reference is for **reviewing changes**. It should help identify likely bottlenecks, operational risks, and unsafe scaling patterns in PRs, repository reviews, and partial code reviews.

## What to look for
- N+1 queries, inefficient fetch strategy, or eager loading that over-fetches
- query shape likely to force scans, sorts, or heavy joins on large tables
- external calls inside long transactions
- OSIV side effects and lazy loading during serialization
- `findAll()` or large in-memory processing that can exhaust heap
- poor Hikari pool sizing, blocked connections, or too many DB connections across pods
- missing cache strategy for static data or cache without invalidation
- Kafka/SQS risks: visibility timeout, rebalance storm, missing idempotency, missing DLQ
- excessive object creation, unnecessary intermediate collections, wrong coroutine dispatcher, noisy logging in hot paths
- hot-path changes with no realistic validation beyond local correctness tests

## Quick diagnosis before judging severity
When possible, ground the review in objective evidence such as:

- p95/p99 latency and error rate
- Hikari active/idle/pending metrics
- slow query evidence or `EXPLAIN ANALYZE`
- heap pressure, GC pauses, blocked threads, or thread dumps
- Kafka lag, SQS message age, redrive/DLQ activity

Without runtime evidence, keep the review hypothesis-driven and explicit about why the code shape is risky.
## Review questions

1. Does this change increase database work per request or per batch item?
2. Can this hold a connection, worker thread, or consumer slot longer than before?
3. Does this materialize large collections or create many temporary objects?
4. Does this scale safely with real production volume and concurrent traffic?
5. Could retries, reprocessing, or load spikes duplicate work or collapse throughput?
6. Is there any realistic validation for the hot path besides correctness-only tests?
## Strong finding patterns

### N+1 or fetch misuse
- **Risk:** data volume growth causes query amplification and sharp latency regression
- **Proposed solution:** keep relationships lazy by default and fetch only what the use case needs via `JOIN FETCH`, projections, or dedicated read queries
### Query/index problem
- **Risk:** a common lookup degrades into full scans or expensive sorts under load
- **Proposed solution:** adjust query shape, add the right index, validate with `EXPLAIN ANALYZE`, and avoid loading unnecessary columns/entities
### Long transaction with I/O
- **Risk:** external API, messaging, or heavy processing keeps DB connections open and degrades pool health
- **Proposed solution:** shorten the transaction, persist state transitions explicitly, and use outbox/idempotency/compensation where side effects are involved
### In-memory batch risk
- **Risk:** `findAll()` or full materialization spikes heap and GC, and may crash workers or pods
- **Proposed solution:** paginate, chunk, stream, or push filtering/aggregation closer to the database
### Pool sizing or DB concurrency problem
- **Risk:** local configuration looks fine per instance but collapses at deployment scale due to pod multiplication
- **Proposed solution:** size Hikari with pod-aware math, check database `max_connections`, and consider PgBouncer when concurrency is high
### Messaging reliability/performance risk
- **Risk:** duplicated processing, consumer rebalance, blocked progress, or infinite retries
- **Proposed solution:** set proper visibility timeout or poll sizing, enforce idempotency, and configure DLQ plus bounded retry strategy
### Coroutine/logging/object allocation issue
- **Risk:** blocked thread pools, unnecessary allocations, GC pressure, or logging bottlenecks in hot paths
- **Proposed solution:** use `Dispatchers.IO` for blocking calls, reduce copies/intermediate collections, sample logs, and avoid eager message construction
### Cache misuse
- **Risk:** repeated static reads waste throughput, or stale cache causes correctness issues
- **Proposed solution:** cache read-mostly data with explicit TTL or eviction strategy and avoid unbounded in-memory caches
## Category suggestions

Use categories such as:
- database
- query
- Hibernate
- transactions
- memory
- cache
- pool
- messaging
- coroutines
- logging
- scalability
- test-gap
## Priority hints

- **CRITICAL**: outage risk, resource exhaustion, duplicate processing, pool collapse, or severe latency blow-up
- **HIGH**: major degradation or intermittent failure under common load
- **MEDIUM**: important inefficiency or scaling gap that should be addressed soon
- **LOW**: preventive hardening or micro-optimization worth doing
## Solution style

Always propose a practical fix, such as:
- query redesign or DTO projection
- index creation and plan validation
- transaction split, outbox, idempotency, or compensation
- pagination, chunking, streaming, or batching
- cache with explicit invalidation
- pool sizing with pod-aware math
- Kafka/SQS retry, timeout, idempotency, and DLQ configuration
- dispatcher correction or allocation reduction
- integration/performance validation for hot paths
## Output expectations inside the unified review

When using this vertical inside `backend-jvm-review-expert`:

- label the finding vertical as `performance`
- keep the same priority system: `CRITICAL`, `HIGH`, `MEDIUM`, `LOW`
- describe the operational symptom or likely runtime evidence
- always propose a concrete remediation, not only an observation
