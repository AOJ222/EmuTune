# ADR-001 — Native Android (Kotlin + Jetpack Compose)

**Status:** Accepted

## Context

EmuTune is a gaming-handheld-first optimisation platform. It must profile the device through legitimate Android APIs, respond to controller/D-pad input as a first-class input method, and eventually integrate with emulator apps via package visibility, intents and adapters. These are Android-platform capabilities with no meaningful web or React Native equivalent.

A separate product (Ourtor) has an established React Native design language, but its implementation is not reusable here.

## Decision

Build EmuTune as a native Android application in Kotlin, using Jetpack Compose for UI. Material 3 is the behavioural/accessibility substrate, but the visual identity is a custom Compose design system (`:core:designsystem`), not a stock Material look.

## Alternatives considered

- **React Native / Expo** — reuse of Ourtor DNA, but cannot reach device profiling, controller focus navigation, Perfetto, Macrobenchmark or Baseline Profiles cleanly.
- **Flutter** — good rendering, weaker Android-native integration story than Compose.
- **Web app** — no access to any of the required Android surfaces.

## Consequences

- Full access to `DeviceProfiler`, thermal APIs, package visibility and controller focus.
- Macrobenchmark and Baseline Profile infrastructure are possible.
- No code reuse with Ourtor; the design DNA is translated, not copied.
