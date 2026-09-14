# Contributing

EmuTune is still under active development, but contributions are welcome.

If you're changing core behaviour, please keep the existing architecture and evidence model intact. In particular, don't fabricate benchmark data, claim unsupported emulator capabilities, or add emulator-specific behaviour outside the adapter layer.

Run the relevant tests before submitting a change:

```bash
./gradlew :core:model:test :core:data:testDebugUnitTest
./gradlew :app:assembleDebug
```

For larger changes, open an issue first so we can agree on the approach.
