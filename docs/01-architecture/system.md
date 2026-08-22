# System Architecture

EmuTune is a native Android app that works offline in Milestone 1. It is built on Jetpack Compose with a custom design system, Room for local persistence, Hilt for DI, and Navigation 3 for type-safe navigation.

## Data flow

```
DeviceProfiler / EmulatorDetector ──► Device + Emulator knowledge
                                             │
Game / GameEdition / ExecutionRoute ──► Room (source of truth for games & routes)
                                             │
Observation / BenchmarkSession ───────► Room (immutable evidence)
                                             │
                         RecommendationEngine (deterministic, staged)
                                             │
                                     Recommendation (derived, recomputable)
                                             │
                                ConfigTransactionManager (apply/verify/rollback)
                                             │
                                     EmulatorAdapter (capability-based)
```

## Dependency direction

```
:app ──► :core:data ──► :core:model
:app ──► :core:designsystem
```

- `:core:model` is pure Kotlin and has no Android dependency. The recommendation and confidence engines are deterministic and JVM-testable.
- `:core:data` holds Android persistence, hardware profiling and adapter implementations; it maps Room entities to/from domain types.
- `:core:designsystem` holds tokens and visual primitives and depends on nothing else.
- `:app` wires Hilt, navigation and screens; no business logic lives in Compose.

## Key boundaries

- Domain entities are separate from Room entities; `Mappers` is the single translation point.
- Status/confidence presentation is a single central mapping (`StatusPresentation`).
- Emulator package identifiers live only in `EmulatorRegistry`.
- Debug-only seeded evidence lives in the debug source set; the release graph cannot resolve it.
