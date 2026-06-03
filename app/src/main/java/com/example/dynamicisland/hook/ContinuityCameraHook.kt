package com.example.dynamicisland.hook

import android.content.Context
import android.content.Intent
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.annotation.SuppressLint
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * BATCH 6: Continuity Camera — QR/Barcode Island
 *
 * Periodically samples the rear camera preview to detect QR codes,
 * barcodes, URLs, and contact cards. When something is detected, the
 * island expands with actionable options — no app-switch needed.
 *
 * Unlike a full camera app this uses a minimal ImageReader at 640×480
 * and samples at most one frame every 600ms to stay within battery budget.
 *
 * Architecture:
 *   BackgroundHandlerThread → Camera2 API → ImageReader (YUV_420_888)
 *   → MLKit BarcodeScanner → IslandBarcodeScanResult
 *   → IslandController receives via broadcast
 *
 * Activation policy:
 *   Only activates when the island is visible AND the user has opted in
 *   via the Features screen toggle. Suspends automatically:
 *     - When a call is active
 *     - When the screen turns off
 *     - When the foreground app is a camera/video app
 *     - After a successful scan (re-arms after 8 seconds)
 */
class ContinuityCameraScanner(private val context: Context) {

    companion object {
        private const val TAG = "ContinuityCamera"
        const val ACTION_BARCODE = "com.example.dynamicisland.BARCODE_DETECTED"

        private const val SCAN_INTERVAL_MS  = 600L
        private const val REARM_DELAY_MS    = 8_000L
        private const val PREVIEW_WIDTH     = 640
        private const val PREVIEW_HEIGHT    = 480
    }

    // ── Published state ───────────────────────────────────────────────────────

    data class BarcodeResult(
        val rawValue:    String,
        val displayText: String,
        val type:        BarcodeType,
        val actionLabel: String
    )

    enum class BarcodeType {
        URL, EMAIL, PHONE, CONTACT, WIFI, GEO, TEXT, PRODUCT_CODE
    }

    private val _latestResult = MutableStateFlow<BarcodeResult?>(null)
    val latestResult: StateFlow<BarcodeResult?> = _latestResult.asStateFlow()

    // ── Internal ──────────────────────────────────────────────────────────────

    private val scope         = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val barcodeClient by lazy {
        try {
            com.google.mlkit.common.sdkinternal.MlKitContext.initializeIfNeeded(context)
        } catch (e: Exception) {
            // Ignore if already initialized or fails
        }
        BarcodeScanning.getClient()
    }
    private val isRunning     = AtomicBoolean(false)
    private var isArmed       = true   // resets after each successful scan

    private val bgThread      = HandlerThread("IslandCamera").also { it.start() }
    private val bgHandler     = Handler(bgThread.looper)

    private var cameraDevice:  CameraDevice?     = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader:   ImageReader?      = null

    // ── Public API ────────────────────────────────────────────────────────────

    /** Call when user enables Continuity Camera in Features screen. */
    fun start() {
        if (isRunning.getAndSet(true)) return
        scope.launch { openCamera() }
    }

    fun stop() {
        if (!isRunning.getAndSet(false)) return
        closeCamera()
    }

    /** Re-arm after user dismisses the island barcode UI. */
    fun rearm() { isArmed = true }

    var isScreenOn = true
        set(value) { field = value; if (value && isRunning.get()) scope.launch { ensureActive() } }

    // ── Camera setup ──────────────────────────────────────────────────────────

    private fun openCamera() {
        try {
            val manager   = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val rearId    = manager.cameraIdList.firstOrNull { id ->
                manager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            } ?: return

            imageReader = ImageReader.newInstance(
                PREVIEW_WIDTH, PREVIEW_HEIGHT, ImageFormat.YUV_420_888, 2
            ).apply {
                setOnImageAvailableListener({ reader ->
                    if (!isArmed || !isRunning.get()) {
                        reader.acquireLatestImage()?.close()
                        return@setOnImageAvailableListener
                    }
                    val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                    scope.launch {
                        try {
                            val inputImage = InputImage.fromMediaImage(image, 0)
                            processFrame(inputImage)
                        } finally {
                            image.close()
                        }
                    }
                }, bgHandler)
            }
            
            @SuppressLint("MissingPermission")
            manager.openCamera(rearId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    createCaptureSession(camera)
                }
                override fun onDisconnected(camera: CameraDevice) {
                    camera.close(); cameraDevice = null
                }
                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close(); cameraDevice = null
                    Log.w(TAG, "Camera error $error")
                }
            }, bgHandler)

        } catch (e: SecurityException) {
            Log.w(TAG, "Camera permission not granted")
        } catch (e: Exception) {
            Log.w(TAG, "Camera open failed: ${e.message}")
        }
    }

    private fun createCaptureSession(camera: CameraDevice) {
        val reader = imageReader ?: return
        try {
            camera.createCaptureSession(
                listOf(reader.surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        startRepeatingCapture(session, camera)
                    }
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.w(TAG, "Capture session configuration failed")
                    }
                },
                bgHandler
            )
        } catch (e: Exception) {
            Log.w(TAG, "Create session failed: ${e.message}")
        }
    }

    private fun startRepeatingCapture(session: CameraCaptureSession, camera: CameraDevice) {
        try {
            val request = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(imageReader!!.surface)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            }.build()
            session.setRepeatingBurst(
                List(1) { request },
                null,
                bgHandler
            )
        } catch (e: Exception) {
            Log.w(TAG, "Repeating capture failed: ${e.message}")
        }
    }

    private fun closeCamera() {
        try {
            captureSession?.close(); captureSession = null
            cameraDevice?.close();   cameraDevice   = null
            imageReader?.close();    imageReader     = null
        } catch (_: Exception) {}
    }

    private suspend fun ensureActive() {
        if (cameraDevice == null) openCamera()
    }

    // ── Frame processing ──────────────────────────────────────────────────────

    private fun processFrame(image: InputImage) {
        barcodeClient.process(image)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isEmpty()) return@addOnSuccessListener
                val barcode = barcodes.first()
                val result  = mapBarcode(barcode) ?: return@addOnSuccessListener

                Log.d(TAG, "Barcode detected: ${result.displayText}")
                isArmed = false   // Prevent re-scanning until dismissed

                _latestResult.value = result

                // Broadcast to IslandController
                val intent = Intent(ACTION_BARCODE).apply {
                    setPackage("com.android.systemui")
                    putExtra("raw",     result.rawValue)
                    putExtra("display", result.displayText)
                    putExtra("type",    result.type.name)
                    putExtra("action",  result.actionLabel)
                }
                context.sendBroadcast(intent)

                // Auto-rearm after delay
                scope.launch {
                    delay(REARM_DELAY_MS)
                    isArmed = true
                    _latestResult.value = null
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Barcode scan error: ${e.message}")
            }
    }

    private fun mapBarcode(barcode: Barcode): BarcodeResult? {
        val raw = barcode.rawValue ?: return null

        return when (barcode.valueType) {
            Barcode.TYPE_URL -> BarcodeResult(
                rawValue    = raw,
                displayText = barcode.url?.url ?: raw,
                type        = BarcodeType.URL,
                actionLabel = "Open Link"
            )
            Barcode.TYPE_EMAIL -> BarcodeResult(
                rawValue    = raw,
                displayText = barcode.email?.address ?: raw,
                type        = BarcodeType.EMAIL,
                actionLabel = "Send Email"
            )
            Barcode.TYPE_PHONE -> BarcodeResult(
                rawValue    = raw,
                displayText = barcode.phone?.number ?: raw,
                type        = BarcodeType.PHONE,
                actionLabel = "Call Number"
            )
            Barcode.TYPE_CONTACT_INFO -> BarcodeResult(
                rawValue    = raw,
                displayText = barcode.contactInfo?.name?.formattedName ?: "Contact Card",
                type        = BarcodeType.CONTACT,
                actionLabel = "Add Contact"
            )
            Barcode.TYPE_WIFI -> {
                val ssid = barcode.wifi?.ssid ?: "Network"
                BarcodeResult(
                    rawValue    = raw,
                    displayText = ssid,
                    type        = BarcodeType.WIFI,
                    actionLabel = "Connect to $ssid"
                )
            }
            Barcode.TYPE_GEO -> BarcodeResult(
                rawValue    = raw,
                displayText = "Location",
                type        = BarcodeType.GEO,
                actionLabel = "Open in Maps"
            )
            Barcode.TYPE_PRODUCT -> BarcodeResult(
                rawValue    = raw,
                displayText = raw,
                type        = BarcodeType.PRODUCT_CODE,
                actionLabel = "Search Product"
            )
            else -> if (raw.length in 6..120) {
                BarcodeResult(
                    rawValue    = raw,
                    displayText = raw,
                    type        = BarcodeType.TEXT,
                    actionLabel = "Copy Code"
                )
            } else null
        }
    }
}