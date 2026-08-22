# Product Language

One canonical term everywhere: domain code, persistence, APIs, and user-facing wording. The domain model never uses synonyms.

## Canonical terms

| Term | Meaning |
| --- | --- |
| **Game** | The top-level curated entity owning editions. |
| **GameEdition** | A distinct release of a game on one platform. |
| **ExecutionRoute** | Exactly how one edition is executed: platform, emulator, build, driver, translation layer, configuration. |
| **Emulator** | A curated emulator/runtime identity in the registry. |
| **EmulatorBuild** | A specific released build of an emulator. |
| **GpuDriver** | A selectable GPU driver package. |
| **TranslationLayer** | A translation layer (e.g. Winlator's Box64). |
| **Configuration** | A typed set of setting values applied to a route. |
| **Observation** | An immutable measured fact under a specific environment. |
| **Evidence** | The assessed evidentiary contribution of an observation. |
| **BenchmarkSession** | The container that produced observations. |
| **Recommendation** | A derived conclusion over current evidence. |
| **OptimizationGoal** | The goal the system optimises for (e.g. `STABLE_60`). |

## Banned synonyms

These must not appear in domain code, persistence or APIs. If the UX needs friendlier display language, map it centrally in `StatusPresentation`:

- Preset
- Profile
- Setup
- Settings Profile
- Best Setup
- Suggested Setup
- Performance Profile

They mean `Configuration` or `ExecutionRoute`; use the precise term.

## Wording rules

- The default UX **makes a decision** ("Best verified route", "47.8 FPS", "High confidence"). It does not present a research assignment.
- The expert UX **explains the decision** (candidates considered, why one won).
- Statuses come from `OptimizationStatus` via the central `StatusPresentation` mapping; screens never invent status strings.
- Confidence is presented as a controlled level (Low/Medium/High/Very High); the numeric score is internal.
- Unavailable metrics are shown as unavailable ("—" / "Unknown"), never synthesised.
