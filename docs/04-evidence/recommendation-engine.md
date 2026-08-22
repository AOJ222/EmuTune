# Recommendation Engine

`RecommendationEngine` produces a deterministic recommendation from current evidence. No LLM is in the ranking path (ADR-005).

## Stages

1. **Hard compatibility** — reject routes that fail to boot, crash materially, are architecturally incompatible, use unsupported drivers, or have severe corruption.
2. **Optimisation target** — for `STABLE_30`/`STABLE_60`, configurations that cannot genuinely sustain the target are rejected even if they spike higher; for `MAX_QUALITY_AT_*`, the performance floor is met first, then fidelity preferred.
3. **Stability** — 1% lows (frame pacing dominates), stutter, frame-time variance, crashes.
4. **Sustained behaviour** — thermal degradation and sustained FPS.
5. **Quality** — resolution, rendering correctness, known defects.
6. **Confidence** — how certain we are the result generalises to this environment.

## Optimisation goals

`BALANCED`, `MAX_PERFORMANCE`, `STABLE_30`, `STABLE_60`, `MAX_QUALITY_AT_30`, `MAX_QUALITY_AT_60`, `BATTERY_EFFICIENT` — each with explicit per-stage weights summing to 1.0.

## Guaranteed behaviours (test-covered)

- Higher average FPS with terrible lows **loses** under `BALANCED`.
- Maximum valid performance **wins** under `MAX_PERFORMANCE`.
- `STABLE_30` **rejects** candidates that cannot sustain 30.
- Exact-device evidence outranks weak hardware matches.
- Stale emulator-build evidence is discounted; crash results are heavily penalised.
- A materially different `GameEdition` is never naively compared.

## Hardware-limited detection

The model supports the "your result ≈ best verified result → no meaningful retuning" conclusion; it is not fabricated before sufficient evidence exists.
