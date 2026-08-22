# ADR-002 — ExecutionRoute is the central domain object

**Status:** Accepted

## Context

The product is not "recommended settings". A game can have several editions and several fundamentally different ways of running them (PC via GameNative or Winlator, PS3 via ARMSX3, Wii via Dolphin). Reducing this to a mutable `bestSettings` record loses the identity of *what was actually run*.

## Decision

`ExecutionRoute` is the central object. It carries the full identity of a way to run an edition: platform, emulator, build, GPU driver, translation layer and configuration. The hierarchy is:

```
Game → GameEdition → ExecutionRoute → Observation → Evidence → Recommendation
```

Recommendations are derived conclusions over evidence about routes; the route is the thing that persists and that evidence attaches to.

## Alternatives considered

- **Settings-centric model** — a `bestSettings` record per game; collapses editions and routes and cannot express "different edition, different fidelity".
- **Emulator-centric model** — the emulator as the primary entry point; wrong hierarchy for a product whose primary entry point is the game.

## Consequences

- Editions are never assumed equivalent; a Wii build reaching 60 FPS does not defeat a materially richer PC build.
- Typed identifiers (`ExecutionRouteId`, `EmulatorId`, …) prevent identity confusion.
- The recommendation engine can rank routes deterministically because each route is a stable, comparable identity.
