# EmuTune

A local-first, evidence-driven emulator optimisation platform for Android gaming handhelds such as AYN Odin/Thor, Retroid Pocket and similar devices.

EmuTune tries to answer one question:

> **What is the best verified way to run this game on this hardware?**

It does that by combining device information, emulator/build data, configuration, measurements and prior evidence.

> **Recommendations are hypotheses until measurements verify them.**

EmuTune is not a compatibility wiki, AI chatbot or settings database. Recommendations are deterministic, confidence is tracked separately, and weak evidence stays weak evidence.

[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.21-7F52FF?logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-2026.08.00-4285F4?logo=jetpackcompose)](https://developer.android.com/develop/ui/compose)
[![Android](https://img.shields.io/badge/Android-29..36-3DDC84?logo=android)](https://developer.android.com)
[![AGP](https://img.shields.io/badge/AGP-9.2.1-3DDC84)](https://developer.android.com/build/releases/gradle-plugin)

## Why?

Emulator settings are usually pieced together from Reddit posts, Discord messages, videos, spreadsheets and trial and error.

That advice may have been tested on different hardware, a different emulator build, a different driver, or not measured at all.

EmuTune treats that information as evidence, not truth.

The aim is to start with the best available hypothesis, test it where possible, and replace assumptions with measurements from the user's actual device.

## Current status

Milestone 1 is substantially complete. Milestone 2 emulator-management work is in progress.

### Optimisation

EmuTune currently includes:

* a deterministic recommendation engine;
* a separate confidence model;
* graded evidence with provenance;
* immutable measured observations;
* device profiling using normal Android APIs;
* execution-route modelling rather than simple `game + emulator` matching;
* no-root performance measurement using MediaProjection;
* transactional configuration changes with validation and read-back verification.

The current screen-capture measurement path observes displayed frame updates rather than the emulator's internal render telemetry, so it is deliberately treated as partial evidence.

## Dolphin

Dolphin is the first real emulator integration.

With user-granted Storage Access Framework access, EmuTune can:

* detect Dolphin;
* read a curated set of configuration values;
* validate supported changes;
* modify real Dolphin INI files;
* read the values back;
* verify that the intended change was applied;
* restore the previous configuration when a transaction fails.

Current automated settings are deliberately limited to values EmuTune understands, including graphics backend, internal resolution, MSAA and anisotropic filtering.

The configuration flow is:

`read → snapshot → validate → apply → read back → verify → commit`

Automatic configuration fails closed when a capability is unavailable or unsafe.

### Current safety limitation

Transaction state is still memory-backed, so recovery after Android kills the EmuTune process is not yet a production guarantee.

Persistent transaction storage and stronger exact-file rollback are current priorities.

## Emulator management

Milestone 2 is building the foundation for EmuTune to manage emulator state as well as optimise settings.

Implemented infrastructure includes:

* emulator catalogue modelling;
* release and artifact discovery;
* GitHub-backed upstream release sources;
* local release caching;
* emulator version/build ordering;
* installation provenance;
* APK/package verification;
* signer validation;
* artifact selection;
* package-install infrastructure;
* desired-state modelling;
* typed setup plans and steps;
* setup execution and reconciliation.

Some of this is implemented and tested but not yet fully connected to the release app.

Release discovery is limited to explicitly curated public upstream repositories. EmuTune does not source modified APKs or unverified third-party binaries.

## Local-first

Core optimisation does not rely on a cloud recommendation service.

Device profiles, measurements, observations, configuration state and recommendation evidence are kept locally.

EmuTune is designed to work on stock Android:

* no root;
* no Shizuku;
* no embedded emulator cores;
* no remote AI ranking.

Network access is only used where the feature requires it, such as checking curated upstream emulator releases.

## App

The current Android app includes:

* Home
* Library
* Game details
* Device information
* Activity/history
* Settings
* emulator-management work in progress

The UI is built primarily for landscape Android handhelds and controller navigation.

## Next priorities

The main goal now is a completely real end-to-end optimisation run:

`device → game → emulator/build → baseline measurement → recommendation → config change → verification → new measurement → keep or rollback`

Immediate work includes:

* persistent transaction state;
* exact-file rollback;
* safer multi-file config writes;
* real-device Dolphin acceptance testing;
* finishing production emulator-management bindings;
* improving measurement quality;
* collecting real evidence across games, devices and emulator builds.

Longer term, EmuTune is intended to compare emulator builds, detect regressions, test configuration alternatives, evaluate sustained/thermal performance and support more emulator adapters.

## Architecture

```text
:app
    Android application, Hilt, Navigation 3, Compose UI and ViewModels

:core:model
    Pure Kotlin domain logic
    Recommendations, confidence, evidence, routes, configuration and benchmarking

:core:data
    Room, repositories, device profiling, emulator adapters,
    SAF, measurements, transactions and emulator-management infrastructure

:core:designsystem
    Compose design system and reusable UI primitives

:benchmark:macrobenchmark
    Android application performance benchmarks

:benchmark:baselineprofile
    Baseline Profile generation
```

A few rules matter throughout the project:

* no business logic in Compose;
* no LLM in recommendation ranking;
* emulator-specific behaviour stays behind adapters;
* unknown data stays unknown;
* measurements retain provenance;
* automatic changes must be validated and verified;
* unsupported capabilities fail closed.

## Build

Requires JDK 17+ (JDK 21 recommended), Gradle 9.4.1 and Android SDK platform 37.

```bash
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease

./gradlew :core:model:test
./gradlew :core:data:testDebugUnitTest
```

The Gradle wrapper is committed.

The repository also includes GitHub Actions CI, Android instrumentation tests, Macrobenchmark infrastructure and Baseline Profile generation.

## Legal

EmuTune is an independent emulator optimisation, configuration and build-management utility.

EmuTune does not provide, host or bundle game ROMs, proprietary firmware, BIOS files or encryption keys.

It is not affiliated with or endorsed by emulator development teams or device manufacturers. Product names and trademarks remain the property of their respective owners.

## Documentation

See [`docs/README.md`](docs/README.md) for the documentation index.

The repository contains deeper documentation for architecture, evidence, benchmarking, emulator integration, Android constraints, design, privacy, implementation status and ADRs.
