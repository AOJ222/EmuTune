# Privacy

EmuTune is optimisation telemetry, not user tracking.

- No advertising-style identifiers; no user fingerprinting.
- No uploading ROMs, game binaries, or personal content.
- Device profiling uses only legitimate Android APIs and creates no tracking identifiers; unknown values (including SoC, which Android does not reliably expose) are represented explicitly, never inferred into an identifier.
- Package detection uses manifest `<queries>` for known integrations, never `QUERY_ALL_PACKAGES`.
- Future benchmark contribution is opt-in, minimal, and pseudonymous/anonymous where practical.
