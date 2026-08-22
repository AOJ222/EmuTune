# Benchmarking

A `BenchmarkProvider` abstraction describes where measurements come from, because Android does not expose reliable arbitrary FPS telemetry from every external emulator.

## Providers (future)

- Adapter-native telemetry
- Emulator logs
- Platform tracing / Perfetto
- Enhanced-device integration (Tier 2)
- Lab Runner telemetry (Tier 3)
- Deterministic replay
- Structured manual benchmark import

Each provider declares the metrics it can produce, its environment requirements, its measurement quality, and its limitations. Missing metrics are never manufactured.

## `BenchmarkSession`

Captures identity, route/config hash, start/end, device/environment, emulator build, thermal state, provider, raw/aggregate metrics, evidence grade, and success/failure — so an observation can be traced back to the run that produced it.

## Automatic search (future, not Milestone 1)

A future optimiser will start from a known-strong configuration, remove invalid combinations, identify high-impact dimensions, generate nearby candidates, benchmark, retain the winner, and stop when improvement becomes insignificant. Bayesian optimisation may follow. The current model supports this without implementing it now.
