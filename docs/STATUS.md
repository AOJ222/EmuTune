# EmuTune — Project Status

> Canonical living status. Update this file, not random notes, when state changes.

## Current state

**v0.1 Alpha** — a real end-to-end loop that learns from a real game on a real device.

## What works

- **Device profiling** — manufacturer/model/Android/ABIs/RAM/display/refresh rate via legitimate Android APIs; SoC is best-effort (`Build.SOC_MODEL`, labelled "inferred", generic strings like `ranchu`/`goldfish`/`arm64` collapse to unknown).
- **Emulator detection** — package-visibility-driven via the `EmulatorRegistry` (Dolphin is the first real integration).
- **Add Game** — title → platform → edition → optional platform id → optional emulator route, with duplicate detection and persistence.
- **Now Playing** — launch-through (Dolphin deep link) with manual "mark as playing" fallback.
- **Measurement** — screen-capture frame-delta FPS via MediaProjection (foreground `mediaProjection` service, Android 14+ compliant), graded `C_PARTIAL_MEASUREMENT`.
- **Observation pipeline** — a completed measurement becomes a persisted `Observation`; a cancelled/failed measurement never does.
- **Recommendation recomputation** — a new observation re-runs the deterministic engine and updates the game UI without manual refresh.
- **Activity** — a curated feed of `GAME_ADDED` / `MEASUREMENT_COMPLETED` / `RECOMMENDATION_CHANGED` events (not raw logs).
- **Config transaction framework** — snapshot → validate → apply → verify → commit/rollback, with recovery (unit-tested via the debug `FakeEmulatorAdapter`).

## What is partial

- **Optimise / guided configuration** — the transaction engine is wired and tested, but the game page does not yet trigger it; Dolphin exposes no `CONFIG_WRITE`, so a real emulator is guided (route-level) rather than auto-configured. Specific per-setting guidance requires verified emulator schema knowledge that does not exist yet.
- **MediaProjection** — the capture path compiles and the analyzer is unit-tested, but needs a real-device runtime check (emulator screen capture is unreliable).
- **Hardware matching** — the engine matches exact-device evidence correctly, but observations store only a `deviceFingerprintId`; observation-device fingerprints are not persisted, so cross-device matching is dormant until the evidence network (M2).

## What is unsupported (by design)

- Reading another app's private config / internal FPS / game list (no root, no Shizuku).
- Reading "what game is running" in another app — EmuTune learns via launch-through or explicit declaration.
- Live emulator-internal render FPS — screen capture observes *displayed updates*, not render telemetry.
- `QUERY_ALL_PACKAGES` / `MANAGE_EXTERNAL_STORAGE` — not used, not qualified under Play policy.

## Current Android constraints

- `Android/data` of other apps is inaccessible (scoped storage, SAF, even all-files access).
- Package visibility is filtered on Android 11+; EmuTune declares `<queries>` for known integrations.
- No reliable SoC-name API; `Build.SOC_MODEL` is best-effort.
- Thermal APIs expose coarse device-level status/headroom only.
- MediaProjection requires per-session user consent + a visible indicator, and a `mediaProjection` foreground service on Android 14+.

## Latest test results

- `:core:model:test` and `:core:data:testDebugUnitTest` pass (recommendation, confidence, config transactions, frame-delta analyzer, session launch/fallback, game add/link, measurement recorder).
- Debug and release APKs build clean, zero warnings.
- Release APK verified to contain 0 debug-only class references (`FakeEmulatorAdapter`, `DebugDemoDataSeeder`, `OptimizationDemoViewModel`, `DebugDataModule`).

## Real-device validation status

**Not yet performed.** Prepared target: AYN Thor Max (Snapdragon 8 Gen 2 / Adreno 740). The acceptance journey is: fresh install → profile → detect emulator → add real game → launch/mark → measure → observation persists → recommendation recomputes → kill/reopen → everything survives → optimise/guided behaves per adapter capability.

## Next priorities

1. Runtime-verify MediaProjection on a real device.
2. Wire Optimise / guided configuration to the game page (capability-aware).
3. Persist observation-device fingerprints so cross-device hardware matching can fire (M2).
4. Add GitHub Actions CI.
5. Run Macrobenchmark / Baseline Profile for real numbers.
