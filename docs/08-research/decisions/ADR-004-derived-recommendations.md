# ADR-004 — Recommendations are derived, not persisted as truth

**Status:** Accepted

## Context

"Best" is a deterministic interpretation of current evidence, not a fact. If recommendations were persisted as the authoritative record, they would drift from the evidence that produced them and become another community-settings database.

## Decision

`Recommendation` is a disposable, recomputable conclusion. The database stores device profiles, games/editions/routes, observations and transaction state; recommendations are computed on demand by `RecommendationEngine` and never persisted as a mutable `bestSettings` record.

## Alternatives considered

- **Persisted recommendation table** — convenient caching, but risks a second source of truth that disagrees with evidence.
- **Hybrid** — cache recommendations with invalidation; rejected as premature for Milestone 1.

## Consequences

- The evidence is the single authoritative store.
- Re-running the engine with new evidence always yields the correct answer.
- `RecommendationScore` and `EvidenceConfidence` stay separate; confidence answers "how sure are we" while score answers "how good".
