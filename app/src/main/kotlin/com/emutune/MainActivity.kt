package com.emutune

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.emutune.benchmark.FpsCaptureResult
import com.emutune.benchmark.FpsCaptureService
import com.emutune.model.benchmark.BenchmarkResult
import com.emutune.ui.EmuTuneApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val captureConsent = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val intent = Intent(this, FpsCaptureService::class.java).apply {
                putExtra(FpsCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(FpsCaptureService.EXTRA_RESULT_DATA, result.data)
            }
            ContextCompat.startForegroundService(this, intent)
        } else {
            // Denied or cancelled — report it so the waiting ViewModel fails fast rather
            // than timing out after the full capture window.
            FpsCaptureResult.flow.value =
                BenchmarkResult.Failure("Screen capture permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EmuTuneApp(
                onMeasureFps = ::requestFpsConsent,
            )
        }
    }

    private fun requestFpsConsent() {
        val manager = getSystemService(MediaProjectionManager::class.java)
        captureConsent.launch(manager.createScreenCaptureIntent())
    }
}
