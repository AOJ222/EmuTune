# EmuTune

A premium, evidence-driven emulator optimisation platform for Android gaming handhelds such as the AYN Odin/Thor family, Retroid Pocket devices, Anbernic Android handhelds and similar landscape hardware — with phones and tablets as secondary targets.

EmuTune determines the best *verified* way to run a particular game on your hardware, explains why, applies configuration changes when technically safe, measures the result, and retains the evidence behind its decision.

> **Recommendations are hypotheses until measurements verify them.**

EmuTune is not a compatibility wiki, an AI chat app, an emulator launcher, or a database of community settings. It makes deterministic decisions from inspectable evidence, keeps confidence separate from recommendation quality, and represents uncertainty explicitly.

[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.21-7F52FF?logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-2026.08.00-4285F4?logo=jetpackcompose)](https://developer.android.com/develop/ui/compose)
[![Android](https://img.shields.io/badge/Android-29..36-3DDC84?logo=android)](https://developer.android.com)
[![AGP](https://img.shields.io/badge/AGP-9.2.1-3DDC84)](https://developer.android.com/build/releases/gradle-plugin)

## Why EmuTune?

Emulator configuration is usually pieced together from community spreadsheets, Reddit posts, Discord messages, videos, compatibility lists and trial and error.

Those recommendations may:

* have been measured on different hardware;
* target a different emulator build;
* depend on a different graphics driver or Android version;
* be outdated;
* optimise only for peak FPS rather than sustained performance;
* or never have been measured at all.

EmuTune treats this information as potential evidence rather than truth.

Its goal is to determine what actually works for a particular game, emulator build and device, measure the result where possible, and progressively replace assumptions with verified observations.

## Core principles

### Evidence over folklore

Machine measurements, partial measurements, community reports and external research remain distinguishable.

Research can produce a hypothesis. It does not silently become measured evidence.

### Deterministic recommendations

The recommendation engine does not use an LLM for ranking.

Given the same routes, evidence and environment, EmuTune should reach the same decision.

AI may assist development or explanation, but it is not part of the application's optimisation decision path.

### Confidence is not score

The route EmuTune currently believes is best and the strength of the evidence supporting that route are separate concepts.

A recommendation can rank first while still carrying low confidence.

### Unknown means unknown

EmuTune does not fabricate hardware information, emulator capabilities or benchmark data to make its model appear more complete.

If Android cannot expose a value reliably, or EmuTune cannot measure something honestly, it remains unknown.

### Verification over assumption

A successful configuration write is not considered success until the resulting state has been read back and verified.

A theoretical performance improvement is not considered verified until evidence supports it.

### Safety over automation

Automatic configuration is only used where EmuTune has a sufficiently understood and reversible integration.

Unsupported capabilities fail closed.

## Status

**Milestone 1 is substantially complete**, with Milestone 2 emulator-management infrastructure under active development.

### Optimisation core

Current capabilities include:

* **Deterministic recommendation engine** — staged evaluation across hard compatibility, target performance, stability, sustained performance and visual quality.
* **Independent confidence model** — evidence quality, hardware match, recency and emulator-build currency are evaluated separately from recommendation score.
* **Graded evidence and provenance** — measurements, community reports and external research retain their source and evidence quality.
* **Immutable observations** — successful measurements become persistent evidence and can influence later recommendations.
* **Execution-route modelling** — EmuTune evaluates complete ways of running a game rather than treating `game + emulator` as the entire decision.
* **Device profiling** — reliable Android and hardware characteristics are gathered through legitimate platform APIs, with unavailable data represented explicitly as unknown.
* **On-device performance measurement** — a no-root MediaProjection provider measures displayed frame-update behaviour including average update rate, 1% lows, frame-time variance and stutter.
* **Transactional configuration framework** — configuration changes are validated, applied, read back and verified before they are considered committed.

Execution routes are designed to distinguish factors such as emulator, emulator build, rendering path, configuration and other execution-specific variables as support expands.

## Evidence model

Evidence quality is a first-class part of EmuTune.

Current evidence grades distinguish between concepts including:

* deterministic benchmarks;
* machine-measured gameplay;
* partial measurements;
* user-submitted evidence;
* external research.

A benchmark provider declares which metrics it can actually produce, its evidence quality, environmental requirements and known limitations.

Missing metrics remain missing rather than being synthesised.

The current MediaProjection measurement path intentionally measures **displayed screen updates**, not the emulator's internal render pipeline. It is therefore treated as partial measurement rather than presented as perfect internal frame telemetry.

Stronger measurement providers can be added without changing the evidence model.

## Dolphin integration

Dolphin is the first real emulator integration and the proving ground for EmuTune's optimisation model.

With user-granted Android Storage Access Framework access, EmuTune can:

* detect the real Dolphin installation;
* inspect a curated subset of Dolphin configuration;
* parse supported INI configuration;
* validate candidate settings before mutation;
* apply real configuration changes;
* read the resulting values back;
* verify that the intended state was reached;
* and restore previous configuration when a transaction fails.

Automatic configuration is deliberately restricted to settings EmuTune understands rather than blindly modifying arbitrary emulator configuration.

The currently curated surface includes settings such as:

* graphics backend;
* internal resolution;
* MSAA;
* anisotropic filtering.

The architecture is capability-based, allowing future emulator adapters to expose only the operations they genuinely support.

## Safe configuration transactions

Configuration changes follow a transactional lifecycle:

`read → snapshot → validate → apply → read back → verify → commit`

Failures enter a rollback path rather than being treated as successful writes.

The transaction architecture also supports interrupted-operation recovery.

Durable recovery across Android process death is **not yet a production guarantee** because the current transaction-state implementation is memory-backed. Persistent transaction storage is a current priority.

Rollback fidelity is also being hardened so that configuration recovery can preserve the exact original contents of every physical emulator configuration file affected by a transaction.

Safety takes priority over increasing the number of settings EmuTune can automate.

## Emulator management — Milestone 2

EmuTune is also evolving from an optimisation utility into a managed emulation platform.

The goal is for EmuTune to understand not only that an emulator is installed, but also:

* which emulator it is;
* which build or release is installed;
* where it came from;
* whether a more appropriate build exists;
* whether the installed package matches the expected signer;
* and which emulator state is desired for a particular execution route.

Milestone 2 currently includes substantial infrastructure for:

* canonical emulator catalogue modelling;
* release and artifact discovery;
* GitHub-backed release sources;
* local release caching and freshness handling;
* emulator version and build ordering;
* installation provenance;
* management levels;
* desired execution state;
* artifact selection;
* APK and package verification;
* signer validation;
* package installation infrastructure;
* typed setup plans;
* typed setup steps;
* setup execution;
* installation reconciliation.

Ambiguous emulator versions are allowed to remain incomparable rather than being forced into an unsafe upgrade/downgrade decision.

Signer mismatches fail closed.

> **Release sourcing:** release discovery is restricted to explicitly curated, public upstream repositories. EmuTune does not source, host or scrape modified APKs or unverified third-party binaries.

Parts of the Milestone 2 infrastructure are implemented and tested but are not yet fully connected to the release application.

## Local-first by design

Core optimisation does not depend on a remote recommendation service.

Device profiles, configuration state, measurements, observations and recommendation evidence are processed and retained locally.

Network access is used only where a feature inherently requires it, such as discovering or downloading curated upstream emulator releases.

EmuTune is designed for stock Android:

* **no root requirement;**
* **no Shizuku requirement;**
* **no bypassing Android's application sandbox;**
* **no embedded emulator cores;**
* **no remote AI ranking service.**

Emulator integrations use platform-supported mechanisms such as package discovery, intents and user-granted Storage Access Framework permissions.

## App

The native Android application currently includes:

* Home
* Library
* Game details
* Device information
* Activity/history
* Settings
* emulator-management work in progress

The Compose interface is designed primarily for landscape gaming handhelds.

Controller and directional navigation are treated as first-class interaction methods rather than desktop/mobile afterthoughts.

A dedicated design system provides shared:

* surfaces;
* metrics;
* status indicators;
* confidence indicators;
* typography;
* spacing;
* focus behaviour;
* motion;
* and visual tokens.

## Current priorities

The next major objective is a completely genuine end-to-end optimisation journey on target hardware:

`device → game → emulator/build → baseline measurement → recommendation → safe configuration change → verification → new measurement → evidence → keep or rollback`

Key remaining work includes:

* replacing memory-backed configuration transaction state with durable persistence;
* preserving exact original emulator configuration during rollback;
* hardening multi-file configuration mutation against partial writes;
* completing real-device Dolphin acceptance testing;
* completing production bindings for the managed-emulator lifecycle;
* hardening multi-plan setup-state persistence;
* connecting release discovery, installed-state reconciliation and setup planning;
* expanding performance measurement beyond screen-update observation;
* collecting real observations across games, devices and emulator builds.

Future optimisation work includes:

* comparing emulator builds and detecting performance regressions;
* controlled A/B testing of configurations and execution routes;
* evaluating graphics drivers where safe integrations exist;
* sustained-performance and thermal testing;
* battery and efficiency measurements;
* performance, quality and efficiency profiles;
* automatic per-device optimisation;
* broader emulator adapter support.

The priority is to prove one integration deeply before expanding emulator coverage broadly.

## Architecture

```text
:app
    Application wiring, Hilt, Navigation 3, Compose UI and ViewModels

:core:model
    Pure-Kotlin domain model
    Recommendations, confidence, evidence, routes, configuration,
    benchmarking and emulator lifecycle concepts

:core:data
    Room, repositories, device profiling, emulator adapters,
    measurement persistence, SAF integration, configuration transactions
    and emulator-management infrastructure

:core:designsystem
    Compose design system, tokens and reusable primitives

:benchmark:macrobenchmark
    Android application performance benchmarks

:benchmark:baselineprofile
    Baseline Profile generation
```

Important architectural rules include:

* no business logic in Compose;
* one canonical owner per domain concept;
* typed identifiers where they improve safety;
* no LLM in the recommendation-ranking path;
* emulator-specific behaviour stays behind capability-based adapters;
* unavailable information remains unknown rather than being fabricated;
* measurements retain their provenance and evidence grade;
* fake/demo evidence must not enter the release decision path;
* automatic configuration must be validated and verified;
* unsupported capabilities fail closed.

## Testing

EmuTune is developed as a tested system rather than a UI prototype.

The project contains automated coverage across areas including:

* recommendation ranking;
* confidence calculation;
* hardware matching;
* evidence handling;
* configuration models;
* INI parsing and lossless editing;
* configuration transactions and rollback;
* repositories and persistence;
* measurement recording;
* game/session behaviour;
* emulator lifecycle modelling;
* version comparison;
* setup planning and execution;
* Compose/layout behaviour.

The project also includes:

* Android instrumentation tests;
* Macrobenchmark infrastructure;
* Baseline Profile generation;
* GitHub Actions CI.

CI builds and tests the application on pushes and pull requests.

## Build

Requires JDK 17+ (JDK 21 recommended), Gradle 9.4.1, and Android SDK platform 37.

```bash
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease

./gradlew :core:model:test
./gradlew :core:data:testDebugUnitTest
```

The Gradle wrapper is committed; no global Gradle installation is required.

## Legal & Attribution

EmuTune is an independent emulator optimisation, configuration and build-management utility.

* **No copyrighted game assets:** EmuTune does not provide, host, bundle or link to copyrighted game ROMs, proprietary firmware, BIOS files or encryption keys. Users are responsible for supplying any required software or files in accordance with applicable law.
* **No affiliation:** EmuTune is not affiliated with, authorised by, sponsored by or endorsed by emulator development teams or device manufacturers including AYN, Retroid or Anbernic.
* **Trademarks:** Product names, trademarks, logos and brands remain the property of their respective owners.

## Documentation

See [`docs/README.md`](docs/README.md) for the documentation index.

The repository contains dedicated documentation covering:

* product principles and terminology;
* system architecture and module boundaries;
* Android platform restrictions and permissions;
* emulator adapter contracts;
* benchmarking and evidence;
* confidence and recommendation behaviour;
* interaction and information architecture;
* controller navigation and design system;
* privacy and telemetry;
* implementation status;
* architectural decision records;
* technical research and integration spikes.

Major architectural decisions are recorded as ADRs so that the reasoning behind the system remains inspectable alongside the implementation.

Detailed implementation status and milestone work are maintained in the project documentation.
