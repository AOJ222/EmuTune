# Terminology

The canonical domain vocabulary. See [Product language](../05-product/product-language.md) for wording rules and banned synonyms.

## Domain entities

| Term | Package | Notes |
| --- | --- | --- |
| Game | `model.game` | owns editions |
| GameEdition | `model.game` | a platform release |
| EditionRelationship | `model.game` | `SAME_CORE_EDITION`, `CLOSE_EQUIVALENT`, `PORT`, `MATERIALLY_DIFFERENT`, `DEMAKE` |
| Platform | `model.game` | + `PlatformFamily` |
| ExecutionRoute | `model.route` | the central object |
| Emulator / EmulatorBuild / GpuDriver / TranslationLayer | `model.route` | registry-owned identity |
| Configuration / ConfigField / ConfigValue / ConfigKey | `model.config` | typed config schema |
| Observation / BenchmarkMetrics / BenchmarkSession / Evidence | `model.evidence` | the evidence model |
| Recommendation / RouteEvaluation | `model.recommendation` | derived conclusions |

## Status taxonomy (`OptimizationStatus`)

`OPTIMAL`, `IMPROVEMENT_AVAILABLE`, `BETTER_ROUTE_AVAILABLE`, `UPDATE_RECOMMENDED`, `REGRESSION_DETECTED`, `HARDWARE_LIMITED`, `UNVERIFIED`, `INSUFFICIENT_EVIDENCE`, `UNSUPPORTED`.

One central presentation mapping (`StatusPresentation`) controls label, icon, colour and explanation.

## Confidence levels

`LOW`, `MEDIUM`, `HIGH`, `VERY_HIGH` — thresholds are centralised. Confidence answers "how strongly does the evidence support this recommendation for this exact environment", and is kept separate from the recommendation score.

## Evidence grades

`A_DETERMINISTIC_BENCHMARK`, `B_MACHINE_GAMEPLAY`, `C_PARTIAL_MEASUREMENT`, `D_USER_SUBMITTED`, `E_EXTERNAL_RESEARCH`. Grade A is not averaged with Grade E anecdotes.

## Hardware match quality

`EXACT_DEVICE`, `EXACT_SOC_GPU`, `RELATED_HARDWARE`, `UNKNOWN_HARDWARE`.
