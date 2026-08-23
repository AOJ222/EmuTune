# EmuTune Documentation

EmuTune is a premium, evidence-driven emulator optimisation platform. Its purpose is to determine the best verified way to run a game on the user's exact hardware, explain why, apply it when safe, verify it, and roll back if it is not better.

This index is the single entry point to the documentation.

- [Status](STATUS.md) — the canonical living project-status document (current state, what works/partial/unsupported, latest test results, real-device validation status, next priorities).

## 00 — Overview

- [Product](00-overview/product.md) — what the product is (and is not)
- [Principles](00-overview/principles.md) — the non-negotiable principles
- [Terminology](00-overview/terminology.md) — canonical domain vocabulary

## 01 — Architecture

- [System](01-architecture/system.md) — overall architecture and data flow
- [Domain model](01-architecture/domain-model.md) — the core domain hierarchy
- [Module boundaries](01-architecture/module-boundaries.md) — Gradle modules and ownership

## 02 — Android

- [Permissions](02-android/permissions.md) — scoped storage, SAF, all-files access
- [Package visibility](02-android/package-visibility.md) — `<queries>` strategy
- [Device profiling](02-android/device-profiling.md) — legitimate hardware APIs

## 03 — Emulators

- [Adapter contract](03-emulators/adapter-contract.md) — the `EmulatorAdapter` interface
- [Dolphin](03-emulators/dolphin.md) — Dolphin integration (see the [spike](08-research/spikes/dolphin-integration.md))

## 04 — Evidence

- [Observations](04-evidence/observations.md) — the evidence model
- [Confidence](04-evidence/confidence.md) — deterministic confidence
- [Recommendation engine](04-evidence/recommendation-engine.md) — staged deterministic ranking

## 05 — Product

- [Information architecture](05-product/information-architecture.md) — the curated hierarchy
- [Product language](05-product/product-language.md) — canonical vocabulary and wording rules
- [Interaction model](05-product/interaction-model.md) — default vs expert UX

## 06 — Design

- [Design system](06-design/design-system.md) — tokens and primitives
- [Tokens](06-design/tokens.md) — colour, type, spacing, radius
- [Motion](06-design/motion.md) — motion language
- [Controller navigation](06-design/controller-navigation.md) — D-pad and controller focus

## 07 — Operations

- [Backend](07-operations/backend.md) — future low-cost architecture
- [Telemetry](07-operations/telemetry.md) — benchmarking telemetry, not tracking
- [Privacy](07-operations/privacy.md) — what is never uploaded

## 08 — Research

- [Decisions](08-research/decisions/) — architecture decision records (ADRs)
- [Spikes](08-research/spikes/) — research spikes (Dolphin integration)

---

- [Implementation plan](implementation-plan.md) — the milestone 1 plan and its status
