# EmuTune

A premium, evidence-driven emulator optimisation platform for Android gaming
handhelds (AYN, Odin, Retroid and similar landscape devices) — and phones and
tablets second.

EmuTune determines the best *verified* way to run a particular game on your exact
hardware, explains why, applies it when technically safe, verifies the result is
genuinely better, and rolls back safely if it is not.

> **Recommendations are hypotheses until measurements verify them.**

This is not a compatibility wiki, an AI chat app, an emulator launcher, or a
database of community settings. The app makes a decision; the evidence remains
inspectable underneath.

[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.21-7F52FF?logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-2026.08.00-4285F4?logo=jetpackcompose)](https://developer.android.com/develop/ui/compose)
[![Android](https://img.shields.io/badge/Android-29..36-3DDC84?logo=android)](https://developer.android.com)
[![AGP](https://img.shields.io/badge/AGP-9.2.1-3DDC84)](https://developer.android.com/build/releases/gradle-plugin)

## Status — Milestone 1

A working, tested vertical slice:

- **Deterministic recommendation engine** — staged evaluation (hard compatibility →
  target → stability → sustained → quality → confidence), no LLM in the ranking path.
- **Confidence model** — evidence grade × hardware match × recency × build currency,
  kept separate from the recommendation score.
- **Transactional config framework** — read → snapshot → validate → apply → verify →
  commit, with rollback and interruption recovery.
- **Device profiling + emulator detection** — legitimate Android APIs only, with
  unknown values represented explicitly.
- **Adaptive, controller-first Compose UI** — Home, Library, Game, Device, Settings,
  with a custom design system.
- **Debug-only optimisation simulation** — the full lifecycle through a fake adapter;
  provably absent from release (verified via `dexdump`).

See [`docs/README.md`](docs/README.md) for the documentation index and
[`docs/implementation-plan.md`](docs/implementation-plan.md) for the full
Milestone 1 plan and its status.

## Build

Requires JDK 17+ (JDK 21 recommended), Gradle 9.4.1, and Android SDK platform 37.

```bash
./gradlew :app:assembleDebug   # debug APK
./gradlew :app:assembleRelease # release APK (unsigned)
./gradlew :core:model:test :core:data:testDebugUnitTest   # unit tests
```

The Gradle wrapper is committed; no global Gradle install is required.

## Layout

```
:app                  — application, Hilt, Navigation 3, UI
:core:model           — pure-Kotlin domain (no Android): recommendation + confidence engines
:core:data            — Room, repositories, profiler, registry, adapters, transaction manager
:core:designsystem    — Compose design system: tokens + primitives
```
