# ADR-006 — Capability-based emulator adapters

**Status:** Accepted

## Context

Every emulator integrates differently, and Android deliberately prevents an ordinary app from reading another app's private config. If emulator-specific behaviour leaked into the UI or recommendation code, the app would make false assumptions about what it can do.

## Decision

Every emulator/runtime integration lives behind a single `EmulatorAdapter` interface with capability detection. The adapter declares a set of `EmulatorCapability`s (e.g. `CONFIG_WRITE`, `GAME_LAUNCH`, `GUIDED_CONFIG`), and the UI is capability-driven: if `CONFIG_WRITE` is absent, no automatic-modification surface is shown. Package identifiers live only in the `EmulatorRegistry`, which adapters reference.

## Alternatives considered

- **Emulator-specific conditionals scattered through the app** — rejected; impossible to keep consistent.
- **One monolithic "manager"** — rejected as a god-class; adapters keep each integration bounded.

## Consequences

- Unknown capabilities fail closed (a capability is declared, then honoured — never implied).
- The `FakeEmulatorAdapter` exercises the full transaction lifecycle safely; the real `DolphinAdapter` claims only what upstream research confirmed.
- Adding an emulator means adding a registry entry and an adapter, never touching UI or ranking code.
