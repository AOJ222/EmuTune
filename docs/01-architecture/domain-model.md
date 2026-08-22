# Domain Model

The core hierarchy:

```
Game
└── GameEdition
    └── ExecutionRoute
        ├── Platform
        ├── Emulator / Runtime
        ├── EmulatorBuild
        ├── GpuDriver
        ├── TranslationLayer
        ├── Configuration
        └── Environment (Device, Android version, performance state, thermal state)

ExecutionRoute → Observation → Evidence

Device + GameEdition + ExecutionRoutes + Observations/Evidence + OptimizationGoal → Recommendation
```

## Key decisions

- **`ExecutionRoute` is the central object** — not "recommended settings" (see ADR-002).
- **Observations are immutable** historical facts (see ADR-003).
- **Recommendations are derived** and recomputable, never persisted as truth (see ADR-004).
- **Edition relationships** are explicit (`EditionRelationship`); platform title matching is never treated as proof of equivalence.
- **Typed identifiers** (`GameId`, `EmulatorId`, …) prevent identity confusion; stringly-typed IDs are banned where a value class materially improves safety.
- **Config is typed** — a `Map<ConfigKey, ConfigValue>` of a sealed value type, never `Map<String, String>`.

## The engine is staged and deterministic

`RecommendationEngine` runs six stages in order: hard compatibility → optimisation target → stability → sustained behaviour → quality → confidence. Each stage rejects or scores explicitly; there is no opaque single weighted equation and no LLM (see ADR-005).
