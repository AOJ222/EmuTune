package com.emutune

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.emutune.benchmark.FpsCaptureResult
import com.emutune.benchmark.FpsCaptureService
import com.emutune.data.emulator.SafGrantStore
import com.emutune.model.benchmark.BenchmarkResult
import com.emutune.ui.EmuTuneApp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var safGrantStore: SafGrantStore

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

    // SAF grant for Dolphin config access: persist the granted tree URI so the reader
    // can find it after restart. Only the URI is stored — never the config contents.
    private val dolphinAccess = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri == null) {
            Log.i(TAG, "SAF spike: user cancelled")
            Toast.makeText(this, "SAF spike: cancelled", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }
        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching {
            contentResolver.takePersistableUriPermission(uri, takeFlags)
            lifecycleScope.launch { safGrantStore.setDolphinUri(uri.toString()) }
            Log.i(TAG, "SAF spike: granted + persisted $uri")
            Toast.makeText(this, "SAF spike: granted", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Log.w(TAG, "SAF spike: takePersistableUriPermission failed", it)
            Toast.makeText(this, "SAF spike: grant failed: ${it.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EmuTuneApp(
                onMeasureFps = ::requestFpsConsent,
                onGrantDolphinAccess = ::requestDolphinAccess,
            )
        }
    }

    private fun requestFpsConsent() {
        val manager = getSystemService(MediaProjectionManager::class.java)
        captureConsent.launch(manager.createScreenCaptureIntent())
    }

    private fun requestDolphinAccess() {
        val root = DocumentsContract.buildTreeDocumentUri(
            "org.dolphinemu.dolphinemu.user",
            "root",
        )
        // Prefill the picker at Dolphin's root on API 26+; fall back to the plain picker.
        val initialUri: Uri? = root
        runCatching { dolphinAccess.launch(initialUri) }
            .onFailure { dolphinAccess.launch(null) }
    }

    private companion object {
        const val TAG = "EmuTuneSafSpike"
    }
}
