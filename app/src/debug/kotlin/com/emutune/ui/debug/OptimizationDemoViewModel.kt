package com.emutune.ui.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emutune.data.config.ConfigTransactionManager
import com.emutune.data.config.ConfigTransactionResult
import com.emutune.data.emulator.FakeEmulatorAdapter
import com.emutune.model.config.BooleanField
import com.emutune.model.config.CandidateConfig
import com.emutune.model.config.CandidateSource
import com.emutune.model.config.ConfigCategory
import com.emutune.model.config.ConfigField
import com.emutune.model.config.ConfigKey
import com.emutune.model.config.ConfigTransactionStep
import com.emutune.model.config.ConfigValue
import com.emutune.model.config.EnumField
import com.emutune.model.config.EnumOption
import com.emutune.model.config.IntegerRangeField
import com.emutune.model.config.Relevance
import com.emutune.model.ids.EmulatorId
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A single rendered step in the simulation timeline. */
data class DemoStep(val label: String, val detail: String? = null)

/** The terminal result of a simulation run. */
sealed interface OptimizationDemoOutcome {
    data object Committed : OptimizationDemoOutcome
    data class RolledBack(val verified: Boolean) : OptimizationDemoOutcome
    data class Rejected(val reason: String) : OptimizationDemoOutcome
}

data class OptimizationDemoState(
    val running: Boolean = false,
    val before: String = "",
    val steps: List<DemoStep> = emptyList(),
    val outcome: OptimizationDemoOutcome? = null,
)

/**
 * Drives the debug optimisation simulation. Each run constructs a fresh
 * [FakeEmulatorAdapter], seeds a small typed config, and asks the real
 * [ConfigTransactionManager] to mutate it — collecting every emitted step.
 */
@HiltViewModel
class OptimizationDemoViewModel @Inject constructor(
    private val transactionManager: ConfigTransactionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(OptimizationDemoState())
    val state: StateFlow<OptimizationDemoState> = _state

    fun run(forceFailure: Boolean) {
        if (_state.value.running) return
        viewModelScope.launch {
            val adapter = FakeEmulatorAdapter(schema = schema(), initialConfig = initialConfig())
            if (forceFailure) adapter.failNextWrite = true

            _state.value = OptimizationDemoState(
                running = true,
                before = format(adapter.currentFields()),
            )

            val steps = mutableListOf<DemoStep>()
            val result = transactionManager.apply(
                adapter = adapter,
                candidate = CandidateConfig(
                    emulatorId = EmulatorId("fake"),
                    fields = candidateFields(),
                    schemaVersion = 1,
                    source = CandidateSource.VERIFIED_EVIDENCE,
                ),
                onStep = { step ->
                    steps += step.toDemoStep()
                    _state.update { it.copy(steps = steps.toList()) }
                },
            )

            _state.update {
                it.copy(
                    running = false,
                    outcome = result.toOutcome(),
                )
            }
        }
    }

    private fun schema(): Map<ConfigKey, ConfigField> = mapOf(
        CPU_CLOCK to IntegerRangeField(
            key = CPU_CLOCK,
            displayName = "CPU clock override",
            description = "Emulated CPU clock percentage",
            category = ConfigCategory.PERFORMANCE,
            defaultValue = ConfigValue.IntegerValue(100),
            min = 50,
            max = 400,
            performanceRelevance = Relevance.HIGH,
        ),
        RESOLUTION to EnumField(
            key = RESOLUTION,
            displayName = "Internal resolution",
            description = "Renderer internal resolution",
            category = ConfigCategory.QUALITY,
            defaultValue = ConfigValue.EnumValue("1x"),
            options = listOf(
                EnumOption("1x", "1x"),
                EnumOption("2x", "2x"),
                EnumOption("3x", "3x"),
            ),
            visualQualityRelevance = Relevance.HIGH,
        ),
        VSYNC to BooleanField(
            key = VSYNC,
            displayName = "Vsync",
            description = "Synchronise to the display refresh rate",
            category = ConfigCategory.PERFORMANCE,
            defaultValue = ConfigValue.BooleanValue(true),
            performanceRelevance = Relevance.MEDIUM,
        ),
    )

    private fun initialConfig(): Map<ConfigKey, ConfigValue> = mapOf(
        CPU_CLOCK to ConfigValue.IntegerValue(100),
        RESOLUTION to ConfigValue.EnumValue("1x"),
        VSYNC to ConfigValue.BooleanValue(false),
    )

    private fun candidateFields(): Map<ConfigKey, ConfigValue> = mapOf(
        CPU_CLOCK to ConfigValue.IntegerValue(300),
        RESOLUTION to ConfigValue.EnumValue("2x"),
        VSYNC to ConfigValue.BooleanValue(true),
    )

    private fun ConfigTransactionStep.toDemoStep(): DemoStep = when (this) {
        is ConfigTransactionStep.ReadingCurrent -> DemoStep("Reading current configuration")
        is ConfigTransactionStep.SnapshotCreated -> DemoStep("Snapshot created", hash.take(8))
        is ConfigTransactionStep.ValidatingCandidate -> DemoStep("Validating candidate")
        is ConfigTransactionStep.Applying -> DemoStep("Applying configuration")
        is ConfigTransactionStep.Verifying -> DemoStep("Verifying applied values")
        is ConfigTransactionStep.Committed -> DemoStep("Committed", hash.take(8))
        is ConfigTransactionStep.RollingBack -> DemoStep("Rolling back", reason)
        is ConfigTransactionStep.RolledBack -> DemoStep(
            "Rollback " + if (rollbackVerified) "verified" else "verification failed",
        )
        is ConfigTransactionStep.Rejected -> DemoStep("Rejected", reason)
    }

    private fun ConfigTransactionResult.toOutcome(): OptimizationDemoOutcome = when (this) {
        is ConfigTransactionResult.Committed -> OptimizationDemoOutcome.Committed
        is ConfigTransactionResult.RolledBack -> OptimizationDemoOutcome.RolledBack(rollbackVerified)
        is ConfigTransactionResult.Rejected -> OptimizationDemoOutcome.Rejected(reason)
    }

    private fun format(fields: Map<ConfigKey, ConfigValue>): String =
        fields.entries.joinToString(", ") { (key, value) ->
            "${key.value}=${value.rendered()}"
        }

    private fun ConfigValue.rendered(): String = when (this) {
        is ConfigValue.BooleanValue -> value.toString()
        is ConfigValue.IntegerValue -> value.toString()
        is ConfigValue.DecimalValue -> value.toString()
        is ConfigValue.EnumValue -> option
        is ConfigValue.TextValue -> value
    }

    private companion object {
        val CPU_CLOCK = ConfigKey("cpu_clock")
        val RESOLUTION = ConfigKey("resolution")
        val VSYNC = ConfigKey("vsync")
    }
}
