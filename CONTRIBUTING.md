# Contributing to EmuTune

Thanks for helping build EmuTune. This is a curated, evidence-driven product —
quality and coherence matter more than the size of a diff.

## Getting started

```bash
# JDK 17+ (21 recommended). Gradle wrapper is committed; no global Gradle needed.
./gradlew :app:assembleDebug          # debug APK
./gradlew :core:model:test            # pure-domain unit tests (fast JVM)
./gradlew :core:data:testDebugUnitTest # data-layer unit tests
./gradlew :app:assembleRelease        # release APK (unsigned)
```

Full test suites run in CI; locally, run only the tests for the files you touched.

## Preflight

Run this before pushing. It is the same gate CI enforces.

```bash
./gradlew :core:model:test :core:data:testDebugUnitTest   # unit tests
./gradlew :app:assembleDebug :app:assembleRelease          # both APKs
```

CI (`.github/workflows/ci.yml`) runs these on every push and pull request.

## Module layout

| Module | Owns |
| --- | --- |
| `:core:model` | Pure-Kotlin domain — no Android imports. Recommendation + confidence engines, config schema, transaction model, adapter contract. |
| `:core:data` | Android persistence (Room), repositories, `DeviceProfiler`, `ThermalMonitor`, `EmulatorRegistry`/`Detector`, adapters, `ConfigTransactionManager`. |
| `:core:designsystem` | Compose tokens + primitives (`OpticSurface`, `Metric`, `StatusBadge`, `ConfidenceIndicator`). |
| `:app` | Hilt, Navigation 3, screens and ViewModels, central status presentation. |

## Commit conventions

- Group related changes into logical commits, not one big squashed diff.
- Short, human, past-tense messages in the imperative style of the existing log:
  - `Add data layer and emulator adapters`
  - `Fix config preview rendering`
  - `Update custom button styles`
- One concern per commit where practical (build files, domain, UI, docs separately).

## Code standards

Run the conformance pass over files you touch before finishing — see
`.cursor/skills/conformance-check/SKILL.md`.

- **No business logic in Compose** — domain logic lives in `:core:model`, data
  access in `:core:data`; screens only render and dispatch.
- **One canonical owner per concept** — no duplicate domain models, no raw status
  strings in UI (map via `StatusPresentation`), no duplicated package identifiers.
- **Typed IDs, not raw strings/longs**, where a value class improves safety.
- **No generic `Utils.kt` / `Helpers.kt` / god-class `Manager`.**
- **Debug-only code lives in `src/debug`** with a no-op `src/release` stub — it must
  never enter the release dependency graph (verified via `dexdump`).
- **Emulator-specific behaviour stays behind adapters**; package IDs live only in
  `EmulatorRegistry`.
- **No emojis** in code, comments, copy or UI — use Compose material icons.
- **Never fabricate benchmark data or Android capabilities**; unknown values are
  represented explicitly.

## Docs

When you change a core system, API, or data structure, update the matching file in
`docs/`. ADRs live in `docs/08-research/decisions/`.
