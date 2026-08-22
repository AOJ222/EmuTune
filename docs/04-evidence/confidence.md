# Confidence

`ConfidenceModel` answers: *how strongly does the evidence support this recommendation for this exact environment?*

It is deterministic. It combines:

1. **Signal** — the strongest evidence: evidence grade × hardware match × recency × build currency.
2. **Quantity boost** — more observations raise confidence with diminishing returns.
3. **Contradiction penalty** — widely divergent measurements lower confidence.
4. **Failure penalty** — failed runs lower confidence.

## Levels

`LOW`, `MEDIUM`, `HIGH`, `VERY_HIGH`, with centralised thresholds (0.45 / 0.70 / 0.85). The numeric score is internal; presentation shows the controlled level.

## Separation from score

Confidence is *not* the recommendation score. A high-confidence but modest recommendation is distinguishable from a high-scoring but poorly-evidenced one. `RecommendationScore` (how good) and `EvidenceConfidence` (how sure) are kept conceptually separate.

## Hardware match

`HardwareMatcher` compares the observation's device fingerprint to the user's device and fails closed — an absent SoC identity degrades to `UNKNOWN_HARDWARE`, never upgrades a comparison.
