package com.example.healthtracker

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Range
import android.view.Surface

class CameraHeartRate(private val context: Context) {

    private var cameraId: String? = null
    private var camera: CameraDevice? = null
    private var session: CameraCaptureSession? = null

    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null

    private var running = false
    private var callback: ((Int) -> Unit)? = null

    // PPG buffer
    private val intensityList = ArrayList<Double>()
    private var lastBpmTimestamp = 0L

    fun start(onBpm: (Int) -> Unit) {
        callback = onBpm
        setupCamera()
    }

    fun stop() {
        running = false
        try { session?.close() } catch (_: Exception) {}
        try { camera?.close() } catch (_: Exception) {}
        try { handlerThread?.quitSafely() } catch (_: Exception) {}
    }

    @SuppressLint("MissingPermission")
    private fun setupCamera() {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        try {
            cameraId = manager.cameraIdList.firstOrNull { id ->
                val chars = manager.getCameraCharacteristics(id)
                val flash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                flash && facing == CameraCharacteristics.LENS_FACING_BACK
            }

            if (cameraId == null) {
                Log.e("CameraHR", "No back camera with flash found")
                return
            }

            handlerThread = HandlerThread("hr_camera_thread").apply { start() }
            handler = Handler(handlerThread!!.looper)

            manager.openCamera(
                cameraId!!,
                object : CameraDevice.StateCallback() {
                    override fun onOpened(cam: CameraDevice) {
                        camera = cam
                        startPreview()
                    }
                    override fun onDisconnected(cam: CameraDevice) {}
                    override fun onError(cam: CameraDevice, error: Int) {}
                },
                handler
            )

        } catch (e: Exception) {
            Log.e("CameraHR", "Camera error: $e")
        }
    }

    private fun startPreview() {
        val texture = SurfaceTexture(0)
        texture.setDefaultBufferSize(640, 480)
        val surface = Surface(texture)

        val request = camera?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)?.apply {
            addTarget(surface)
            set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
            set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(30, 30))
        } ?: return

        camera?.createCaptureSession(
            listOf(surface),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(sess: CameraCaptureSession) {
                    session = sess
                    running = true
                    sess.setRepeatingRequest(request.build(), captureCallback, handler)
                }
                override fun onConfigureFailed(sess: CameraCaptureSession) {}
            },
            handler
        )
    }

    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            val gains = result.get(CaptureResult.COLOR_CORRECTION_GAINS)
            if (gains != null) {
                processSample(gains.red.toDouble())
            }
        }
    }

    private fun processSample(value: Double) {
        if (!running) return

        intensityList.add(value)
        if (intensityList.size > 200) intensityList.removeAt(0)

        val now = System.currentTimeMillis()
        if (now - lastBpmTimestamp < 3000) return

        lastBpmTimestamp = now

        val bpm = calculateBpm(intensityList)
        if (bpm != null && bpm in 40..180) {
            callback?.invoke(bpm)
        }
    }

    private fun calculateBpm(signal: List<Double>): Int? {
        if (signal.size < 40) return null

        val peaks = ArrayList<Int>()
        for (i in 1 until signal.size - 1) {
            if (signal[i] > signal[i - 1] && signal[i] > signal[i + 1]) {
                peaks.add(i)
            }
        }

        if (peaks.size < 2) return null

        val intervals = peaks.zipWithNext { a, b -> b - a }
        val avg = intervals.average()
        if (avg <= 0) return null

        val fps = 30.0
        return ((fps / avg) * 60).toInt()
    }
}
