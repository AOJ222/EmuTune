# Observations

An `Observation` records something that was actually measured or reliably established under a specific environment. It is an immutable historical fact.

```
Device: Snapdragon 8 Gen 2 / Adreno 740
Game edition: Web of Shadows PC
Execution route: GameNative + build + driver + config
Average FPS: 48.2      1% low: 39.4
Duration: 15 min       Thermal: recorded
Benchmark method: machine-measured gameplay
Timestamp: specific
```

When a new emulator build ships, the observation is not modified — a new observation is recorded.

## Metrics

`BenchmarkMetrics` carries average FPS, 1% low, 0.1% low, frame-time variance, stutter, sustained FPS, thermal degradation, power, visual quality, rendering correctness and compatibility defects. Every numeric field is nullable: an unavailable metric stays explicitly unavailable, never synthesised.

## Grades and match

- **Evidence grade** (`EvidenceGrade`) A–E distinguishes deterministic benchmarks from community anecdotes; they are never averaged as equivalent.
- **Hardware match** (`HardwareMatchQuality`) is computed independently (`EXACT_DEVICE` … `UNKNOWN_HARDWARE`) and never inferred from weak evidence.

## Thermal context

`ThermalState` distinguishes a cold 30-second 54 FPS run from a sustained 15-minute 47 FPS run. Where Android exposes thermal headroom it is captured; where it does not, it is absent.
