# EmuTune

A local-first, evidence-driven emulator optimisation platform for Android gaming handhelds.

EmuTune tries to answer one question:

> **What is the best verified way to run this game on this hardware?**

It combines device information, emulator/build data, configuration and measurements to make deterministic recommendations, then verifies them where possible.

> **Recommendations are hypotheses until measurements verify them.**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.21-7F52FF?logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-2026.08.00-4285F4?logo=jetpackcompose)](https://developer.android.com/develop/ui/compose)
[![Android](https://img.shields.io/badge/Android-29..36-3DDC84?logo=android)](https://developer.android.com)

## Why?

Emulator settings are usually pieced together from Reddit posts, Discord messages, videos, spreadsheets and trial and error.

That advice may have been tested on different hardware, a different emulator build, or not measured at all.

The aim is to start with the best available hypothesis, test it on the user's actual device, and replace assumptions with measurements over time.

## Current status

Milestone 1 is substantially complete. Milestone 2 emulator-management work is in progress.

Current functionality includes:

* deterministic recommendation and confidence models;
* graded evidence with provenance;
* immutable measured observations;
* device profiling using normal Android APIs;
* execution-route modelling;
* no-root performance measurement using MediaProjection;
* transactional configuration changes with read-back verification;
* a real Dolphin integration using Android's Storage Access Framework.

The current measurement path observes displayed frame updates rather than emulator-internal render telemetry, so it is deliberately treated as partial evidence.

## Dolphin

Dolphin is the first real emulator integration.

With user-granted storage access, EmuTune can read a curated set of Dolphin settings, validate changes, modify the relevant INI files, read the result back and verify that the intended state was reached.

Current automated settings include graphics backend, internal resolution, MSAA and anisotropic filtering.

Configuration changes follow:

`read → snapshot → validate → apply → read back → verify → commit`

Unsupported or unsafe capabilities fail closed.

Transaction state is currently memory-backed, so recovery after Android kills the EmuTune process is not yet a production guarantee.

## Emulator management

Milestone 2 is adding support for managing emulator builds as well as their settings.

The repository already contains infrastructure for:

* emulator catalogue and release modelling;
* upstream GitHub release discovery;
* artifact selection and caching;
* emulator build/version comparison;
* installation provenance;
* package and signer verification;
* package installation;
* desired-state modelling;
* setup plans and reconciliation.

Some of this is implemented and tested but not yet fully connected to the release app.

Release discovery is restricted to explicitly curated public upstream repositories. EmuTune does not source modified APKs or unverified third-party binaries.

## Local-first

Core optimisation does not depend on a cloud recommendation service.

Measurements, observations, configuration state and recommendation evidence are kept locally.

EmuTune is designed for stock Android:

* no root;
* no Shizuku;
* no embedded emulator cores;
* no remote AI ranking.

## App

The Android app currently includes:

* Home
* Library
* Game details
* Device information
* Activity/history
* Settings
* emulator-management work in progress

The UI is designed primarily for landscape Android handhelds and controller navigation.

## Next

The main goal now is a completely real end-to-end optimisation run:

`device → game → emulator/build → baseline → recommendation → config change → verification → new measurement → keep or rollback`

Current priorities are:

* persistent transaction state;
* stronger rollback and multi-file config safety;
* real-device Dolphin testing;
* completing emulator-management integration;
* improving measurement quality;
* collecting real evidence across games and hardware.

Longer term, EmuTune is intended to compare emulator builds, detect regressions, test different execution routes and support more emulator integrations.

## Architecture

```text
:app
    Android application, Compose UI and ViewModels

:core:model
    Pure Kotlin domain logic

:core:data
    Persistence, device profiling, emulator adapters,
    measurements, transactions and management infrastructure

:core:designsystem
    Shared Compose design system

:benchmark:macrobenchmark
    Android performance benchmarks

:benchmark:baselineprofile
    Baseline Profile generation
```

## Build

Requires JDK 17+ (JDK 21 recommended), Gradle 9.4.1 and Android SDK platform 37.

```bash
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
./gradlew :core:model:test
./gradlew :core:data:testDebugUnitTest
```

The Gradle wrapper is committed.

## Legal

EmuTune is an independent emulator optimisation, configuration and build-management utility.

It does not provide, host or bundle game ROMs, proprietary firmware, BIOS files or encryption keys.

EmuTune is not affiliated with or endorsed by emulator development teams or device manufacturers.

## Documentation

See [`docs/README.md`](docs/README.md) for the deeper technical documentation, architecture notes, implementation status and ADRs.
