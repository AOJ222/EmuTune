# ADR-003 — Observations are immutable historical facts

**Status:** Accepted

## Context

A measurement ("48.2 FPS on Snapdragon 8 Gen 2 via GameNative build X") is a fact about a specific environment at a specific time. When a new emulator build ships, that fact does not change — a new observation is recorded.

## Decision

`Observation` is effectively immutable. It records what was measured under which environment, with its evidence grade, hardware match quality, timestamp and metrics. Nothing mutates an observation in place. `Recommendation` is the derived, disposable interpretation of current observations; it is never the authoritative store.

## Alternatives considered

- **Mutable benchmark rows** — updating a row when a new build arrives destroys history and makes rollback/audit impossible.
- **Persisted recommendations** — making "best" a stored field encourages drift from evidence; recommendations are recomputed, not stored.

## Consequences

- Auditability: every claim traces to a specific observation.
- New builds naturally create new observations rather than editing old ones.
- The engine stays deterministic because its inputs are a stable, append-only evidence set.
