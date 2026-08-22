# Module Boundaries

Four Gradle modules, each with clear ownership. Boundaries are package-level within `:core:*`; substantial future emulator integrations may become `:integration/<name>` modules.

## `:core:model` (pure Kotlin)

- `ids/` — typed identifiers
- `game/` — Game, GameEdition, Platform, EditionRelationship
- `route/` — ExecutionRoute, Emulator, EmulatorBuild, GpuDriver, TranslationLayer, EmulatorCapability
- `device/` — DeviceFingerprint, SoCModel, HardwareMatchQuality, DeviceEnvironment, ThermalState, HardwareMatcher
- `config/` — ConfigKey, ConfigValue, ConfigField, CandidateConfig, ConfigSnapshot, ConfigTransaction
- `evidence/` — Observation, BenchmarkMetrics, BenchmarkSession, Evidence
- `recommendation/` — Recommendation, OptimizationGoal, OptimizationStatus, ConfidenceModel, RecommendationEngine
- `emulator/` — EmulatorAdapter contract, GameIdentity

No Android imports. No UI formatting. No business logic leaking from here.

## `:core:data`

- `hardware/` — DeviceProfiler, ThermalMonitor
- `emulator/` — EmulatorRegistry, EmulatorDetector, DolphinAdapter, FakeEmulatorAdapter
- `config/` — ConfigHasher, ConfigTransactionManager, ConfigTransactionStore
- `db/` — Room entities, DAOs, database, Mappers
- `repo/` — DeviceRepository, GameRepository, RecommendationRepository
- `di/` — DataModule (Hilt), optional demo seeder binding

## `:core:designsystem`

- `theme/` — Color, Type, Dimens, Motion, EmuTuneTheme
- `component/` — OpticSurface, Metric, StatusBadge, ConfidenceIndicator

## `:app`

- Hilt entry (`EmuTuneApplication`, `MainActivity`)
- `ui/` — navigation, MainScreen (tab scaffold), Home/Library/Game/Device/Settings screens + ViewModels
- `ui/presentation/` — central status/confidence presentation mapping

## Rules

- One canonical owner per concept; no duplicate domain models between features.
- No business logic in Compose; no emulator-specific conditionals outside adapters/registry.
- No generic `Utils.kt` / `Helpers.kt` / god-class `Manager`.
