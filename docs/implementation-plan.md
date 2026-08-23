# Implementation Plan — Milestone 1

**Date:** 2026-08-21

This records the toolchain decisions, the module structure, what is built and what is intentionally deferred.

## Toolchain (verified against authoritative sources)

| Concern | Decision | Rationale |
| --- | --- | --- |
| JDK | **21 (LTS)** | Only JDK 26 was installed; AGP 9.2 documents JDK 17+ with 17/21 as the supported zone. JDK 26 is too new for the toolchain. |
| Gradle | **9.4.1** | Minimum for AGP 9.2; also the first line that supports JDK 26 for the daemon. |
| Android Gradle Plugin | **9.2.1** | Cached locally; within the "9.x generation". |
| Kotlin | **2.3.21** | Era-matched to AGP 9.2. AGP 9 uses built-in Kotlin, so `org.jetbrains.kotlin.android` is removed everywhere. |
| KSP | **2.3.11** | Unified `2.3.x` versioning aligned with Kotlin 2.3.x. |
| Compose | **BOM 2026.08.00** | Latest stable; Material 3 1.4.0. |
| Navigation | **Navigation 3 1.1.6** | Stable type-safe navigation. |
| Adaptive | **1.2.0** | Latest stable (1.3 is RC). |
| Room / Hilt / DataStore | **2.8.4 / 2.60.1 / 1.2.1** | Latest stable. |
| compileSdk / targetSdk / minSdk | **37 / 36 / 29** | compileSdk 37 (Android 17) is required by the latest androidx releases; installed as `platforms;android-37.0`. |

## Module structure

```
:app                  — application, Hilt entry, Navigation 3, UI (Home/Library/Game/Device/Settings)
:core:model           — pure Kotlin domain (no Android): ids, enums, sealed interfaces,
                        deterministic recommendation + confidence engines, config schema,
                        transaction state machine, adapter contract
:core:data            — Android data layer: Room, repositories, DeviceProfiler, ThermalMonitor,
                        EmulatorRegistry/Detector, adapters (Dolphin + Fake), ConfigTransactionManager
:core:designsystem    — Compose design system: tokens, theme, primitives (OpticSurface, Metric, …)
```

The domain is Android-free so the engine and confidence model test as fast JVM tests.

## Implemented (Milestone 1)

- Core domain: `Game`, `GameEdition`, `ExecutionRoute`, `Platform`, `Emulator`, `Configuration`, `Observation`, `BenchmarkSession`, `Evidence`, `Recommendation`, typed IDs, `OptimizationGoal`, `OptimizationStatus`, `ConfidenceLevel`, `EvidenceGrade`, `HardwareMatchQuality`, `EditionRelationship`.
- Deterministic `RecommendationEngine` (staged: hard compatibility → target → stability → sustained → quality → confidence) with per-goal weights.
- Deterministic `ConfidenceModel` (grade × hardware-match × recency × build-currency, quantity boost, contradiction + failure penalties).
- Config schema framework (`ConfigField` sealed hierarchy, typed `ConfigValue`).
- `ConfigTransactionManager` with the full read → snapshot → validate → apply → verify → commit/rollback state machine, persisted via `ConfigTransactionStore`.
- `FakeEmulatorAdapter` (full lifecycle) + honest `DolphinAdapter` (only research-confirmed capabilities).
- `DeviceProfiler` + `ThermalMonitor` (legitimate Android APIs, unknown values represented explicitly).
- `EmulatorRegistry` + `EmulatorDetector` (single source of package identifiers).
- Room persistence (games, editions, routes, observations, device profile, installations) + repositories.
- Design system (tokens, `EmuTuneTheme`, `OpticSurface`, `Metric`, `StatusBadge`, `ConfidenceIndicator`) + central `StatusPresentation` mapping.
- Adaptive/controller-focus tab navigation; Home, Library, Game, Device, Activity, Settings screens.
- Debug-only `DebugDemoDataSeeder` (Spider-Man: Web of Shadows demo) isolated in the debug source set; the release graph cannot resolve it.
- Debug-only optimisation simulation: Settings renders an `OptimizationDemoSection` that drives the real `ConfigTransactionManager` through a `FakeEmulatorAdapter`, showing each transaction step (read → snapshot → validate → apply → verify → commit/rollback) live. The release build substitutes a no-op section, and the fake adapter lives in the debug source set (verified: 0 `FakeEmulatorAdapter` references in the release APK).

## v0.1 Alpha loop (added later)

- **Real Add Game** — title → platform → edition → optional platform id → optional emulator route, with duplicate detection and auto-generated Room ids linking game/edition/route.
- **Now Playing** — launch-through (Dolphin deep link) with manual "mark as playing" fallback.
- **Measurement → Observation** — screen-capture frame-delta FPS via MediaProjection, persisted as a `C_PARTIAL_MEASUREMENT` observation; cancelled/failed measurements never persist.
- **Observation → Evidence → Recommendation** — a new observation re-runs the deterministic engine and updates the game UI without refresh.
- **Activity** — a curated feed (`GAME_ADDED` / `MEASUREMENT_COMPLETED` / `RECOMMENDATION_CHANGED`).

See [STATUS.md](docs/STATUS.md) for what is partial and unsupported.

## Verified

- 42 unit tests pass (recommendation engine, confidence model, config transactions incl. snapshot persistence + step ordering, frame-delta analyzer, session launch/fallback, game add/link, measurement recorder), plus 1 instrumented UI test.
- `:app:assembleDebug` and `:app:assembleRelease` both build clean; `lintVitalRelease` passes.

## Intentionally not built in Milestone 1

- Cloud backend, accounts, subscriptions, LLM integration, Lab Runner, Bayesian optimiser, universal FPS capture, more emulator adapters, marketing site.
- The OPTIMISE button on the game page is still a placeholder; automatic apply is gated on `CONFIG_WRITE`, which Dolphin does not expose (see the Dolphin spike). The full apply/verify/rollback lifecycle is demonstrated live in the debug-only Settings simulation through the `FakeEmulatorAdapter`.

## What works / what is honest

- Device profiling, emulator detection, game/edition/route modelling, deterministic recommendation with confidence and structured reasoning.
- Automatic config mutation is fully demonstrated through `FakeEmulatorAdapter` and unit-tested, but real emulator config mutation is correctly reported as unsupported without root.
