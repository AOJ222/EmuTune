# Information Architecture

One highly curated hierarchy. Six primary conceptual areas, not fifteen overlapping destinations.

- **Home** — what matters now: device summary, installed emulators, games requiring attention.
- **Library** — the canonical game collection; the game is the primary entry point, not the emulator.
- **Optimise** — active/recent optimisation operations (future).
- **Activity** — curated history of optimisations, benchmark changes, route changes, updates, regressions, rollbacks (not raw logs).
- **Device** — detected hardware, Android version, installed emulators, builds, capabilities.
- **Settings** — app preferences only. Advanced emulator configuration belongs to the game/route context, not a global settings dump.

Milestone 1 implements Home, Library, Device and Settings as top-level tabs, with Game as a pushed detail screen. Optimise and Activity are future areas.
