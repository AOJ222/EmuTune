# Adapter Contract

Every emulator/runtime integration implements one interface:

```kotlin
interface EmulatorAdapter {
    val identity: EmulatorIdentity
    suspend fun detectInstallation(): EmulatorInstallation?
    suspend fun detectCapabilities(installation: EmulatorInstallation): Set<EmulatorCapability>
    suspend fun readConfiguration(game: GameIdentity?): ConfigReadResult
    suspend fun validateConfiguration(candidate: CandidateConfig): ConfigValidationResult
    suspend fun applyConfiguration(candidate: CandidateConfig): ApplyConfigResult
    suspend fun verifyAppliedConfiguration(expected: CandidateConfig): ConfigVerificationResult
    suspend fun restoreConfiguration(snapshot: ConfigSnapshot): RestoreConfigResult
    suspend fun launch(route: ExecutionRoute): LaunchResult
}
```

## Capabilities

`INSTALLATION_DETECTION`, `GAME_DETECTION`, `GAME_LAUNCH`, `CONFIG_READ`, `CONFIG_WRITE`, `PER_GAME_CONFIG`, `CONFIG_IMPORT`, `CONFIG_EXPORT`, `DRIVER_SELECTION`, `TELEMETRY`, `AUTOMATED_BENCHMARK`, `GUIDED_CONFIG`.

The UI is capability-driven. If `CONFIG_WRITE` is absent, no automatic-modification surface is shown. A capability is declared and then honoured — never implied.

## Principles

- Every result type is explicit (`Success`/`NotSupported`/`Failed`/…); no exceptions are swallowed.
- Unknown schema or config version fails closed (`Unsupported`), never guessed.
- Package identifiers are referenced from `EmulatorRegistry`, not duplicated in adapter code.

## Transactions

`ConfigTransactionManager` drives the state machine over the adapter:

```
READ CURRENT → VALIDATE → SNAPSHOT + HASH → VALIDATE CANDIDATE → APPLY → READ BACK → VERIFY → COMMIT
                                              (any failure) → ROLLBACK → VERIFY ROLLBACK
```

State is persisted via `ConfigTransactionStore` so an interrupted transaction can be detected and recovered after process death.
