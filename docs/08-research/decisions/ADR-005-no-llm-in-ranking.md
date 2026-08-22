# ADR-005 — No LLM in the ranking path

**Status:** Accepted

## Context

The product principle is that measurements establish truth; recommendations are hypotheses until verified. An LLM in the ranking path would produce opaque, non-deterministic results and invite fabrication.

## Decision

The recommendation engine is deterministic. `RecommendationEngine` runs a staged evaluation (hard compatibility, optimisation target, stability, sustained behaviour, quality, confidence) with no machine-learning and no external API calls. `ConfidenceModel` is likewise deterministic. AI is reserved for future research-assistant roles (parsing changelogs, classifying reports, proposing candidates) — never for declaring a winner or mutating config.

## Alternatives considered

- **LLM ranking** — rejected: non-deterministic, unexplainable, capable of hallucinating settings.
- **Giant weighted equation** — rejected in favour of staged evaluation so each rejection is inspectable.

## Consequences

- Every recommendation is reproducible and explainable via structured `RecommendationReason`s.
- Tests can assert exact behaviour ("higher FPS with terrible lows loses under BALANCED").
- A future `ResearchAssistant` interface can feed candidates in without contaminating the ranking.
