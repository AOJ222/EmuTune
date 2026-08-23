package com.emutune.benchmark

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.emutune.model.benchmark.BenchmarkResult
import com.emutune.model.benchmark.FrameDeltaAnalyzer
import com.emutune.model.benchmark.FrameSample
import com.emutune.model.evidence.EvidenceGrade
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Foreground service that captures the screen through MediaProjection and computes a
 * frame-rate sample by hashing each captured frame and counting when the image changes.
 *
 * It runs only while measuring (Android 14+ requires the `mediaProjection` foreground
 * service type and a visible notification). The result is delivered through
 * [FpsCaptureResult.result] once a fixed capture window elapses. The evidence grade is
 * [EvidenceGrade.C_PARTIAL_MEASUREMENT], mirroring ScreenCaptureFpsProvider's capability.
 */
class FpsCaptureService : Service() {

    private val binder = LocalBinder()
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private val running = AtomicBoolean(false)
    private val previousHash = AtomicReference<Int>(null)

    private val lock = Any()
    private val samples = mutableListOf<FrameSample>()

    inner class LocalBinder : Binder() {
        val service: FpsCaptureService get() = this@FpsCaptureService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(EXTRA_RESULT_DATA)
        }
        val durationMs = intent?.getLongExtra(EXTRA_DURATION_MS, DEFAULT_DURATION_MS) ?: DEFAULT_DURATION_MS

        if (resultCode == 0 || data == null) {
            FpsCaptureResult.flow.value = BenchmarkResult.Failure("Missing screen capture consent")
            stopSelf()
            return START_NOT_STICKY
        }

        startAsMediaProjectionForeground()
        beginCapture(resultCode, data, durationMs)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopCapture()
        super.onDestroy()
    }

    private fun startAsMediaProjectionForeground() {
        createNotificationChannel()
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("Measuring performance")
            .setContentText("EmuTune is reading the frame rate")
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun beginCapture(resultCode: Int, data: Intent, durationMs: Long) {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val projection = projectionManager.getMediaProjection(resultCode, data) ?: run {
            FpsCaptureResult.flow.value = BenchmarkResult.Failure("Could not obtain MediaProjection")
            stopSelf()
            return
        }
        mediaProjection = projection

        val metrics = DisplayMetrics()
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)
        val width = metrics.widthPixels
        val height = metrics.heightPixels

        val reader = ImageReader.newInstance(width, height, android.graphics.PixelFormat.RGBA_8888, 2)
        imageReader = reader
        running.set(true)

        virtualDisplay = projection.createVirtualDisplay(
            "emutune-fps",
            width,
            height,
            metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface,
            null,
            null,
        )

        reader.setOnImageAvailableListener({ r ->
            if (!running.get()) return@setOnImageAvailableListener
            var image: Image? = null
            try {
                image = r.acquireLatestImage() ?: return@setOnImageAvailableListener
                val hash = hashBuffer(image.planes[0].buffer)
                val previous = previousHash.getAndSet(hash)
                addSample(
                    FrameSample(
                        timestampNanos = System.nanoTime(),
                        changed = previous == null || previous != hash,
                    ),
                )
            } finally {
                image?.close()
            }
        }, null)

        Thread {
            try {
                Thread.sleep(durationMs)
            } catch (_: InterruptedException) {
                // Fall through to finish the window.
            }
            val snapshot = snapshotSamples()
            val measured = FrameDeltaAnalyzer.analyze(snapshot)
            FpsCaptureResult.flow.value = if (measured.hasAnyMeasurement) {
                BenchmarkResult.Success(
                    metrics = measured,
                    grade = EvidenceGrade.C_PARTIAL_MEASUREMENT,
                    durationSeconds = (durationMs / 1000).toInt().coerceAtLeast(1),
                )
            } else {
                BenchmarkResult.Failure("No frame changes detected during the capture window")
            }
            stopSelf()
        }.start()
    }

    private fun addSample(sample: FrameSample) = synchronized(lock) {
        samples += sample
    }

    private fun snapshotSamples(): List<FrameSample> = synchronized(lock) {
        samples.toList()
    }

    private fun stopCapture() {
        running.set(false)
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        mediaProjection?.stop()
        mediaProjection = null
    }

    /** A fast non-cryptographic hash over sampled pixels — any change flips the hash. */
    private fun hashBuffer(buffer: java.nio.ByteBuffer): Int {
        var hash = 0x811C9DC5.toInt()
        val limit = buffer.limit()
        val stride = 64
        buffer.rewind()
        var i = 0
        while (i < limit) {
            hash = (hash * 0x01000193) xor (buffer.get(i).toInt() and 0xFF)
            i += stride
        }
        buffer.rewind()
        return hash
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Performance measurement", NotificationManager.IMPORTANCE_LOW),
            )
        }
    }

    companion object {
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        const val EXTRA_DURATION_MS = "duration_ms"
        const val DEFAULT_DURATION_MS = 10_000L

        private const val CHANNEL_ID = "fps_capture"
        private const val NOTIFICATION_ID = 1001
    }
}

/** Holds the result of the most recent capture, observed by the UI after the service stops. */
object FpsCaptureResult {
    val flow = kotlinx.coroutines.flow.MutableStateFlow<BenchmarkResult?>(null)
}
