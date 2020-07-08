package me.shiki.livepusher.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.core.app.ActivityCompat
import me.shiki.livepusher.ext.getScreenHeight
import me.shiki.livepusher.ext.getScreenWidth
import java.lang.ref.WeakReference

class LivePusherCamera(private val context: Context) {

    private var surfaceTexture: SurfaceTexture? = null
    private var currCameraId: Int = -1

    private var cameraDevice: CameraDevice? = null
    private var cameraManager: CameraManager? = null

    private var previewSurface: Surface? = null

    //相继属性
    private var cameraCharacteristics: CameraCharacteristics? = null

    //预览尺寸
    var previewSize: Size? = null
        private set

    //输出图片尺寸
    var pictureSize: Size? = null
        private set

    private var previewRequestBuilder: CaptureRequest.Builder? = null

    private var backgroundHandler: Handler? = null
    private var backgroundThread: HandlerThread? = null

    private var imageReader: ImageReader? = null
    private var captureSession: CameraCaptureSession? = null
    private var previewRequest: CaptureRequest? = null

    private val stateCallback: StateCallback by lazy {
        StateCallback(WeakReference(this))
    }

    var onStartPreviewListener: (() -> Unit)? = null

    private var width = 0
    private var height = 0

    init {
        width = context.getScreenWidth()
        height = context.getScreenHeight()
    }

    fun initCamera(surfaceTexture: SurfaceTexture?, cameraId: Int) {
        try {
            this.surfaceTexture = surfaceTexture
            cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            cameraCharacteristics = cameraManager?.getCameraCharacteristics(cameraId.toString())
            val map: StreamConfigurationMap? =
                cameraCharacteristics?.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            if (map != null) {
                previewSize = getFitSize(map.getOutputSizes(SurfaceTexture::class.java))
                pictureSize = getFitSize(map.getOutputSizes(ImageFormat.JPEG))
            }
            openCamera(cameraId)
        } catch (e: Exception) {
            // Log.e(this::javaClass.name, e.message)
            e.printStackTrace()
        }
    }

    /**
     * 打开摄像头
     */
    private fun openCamera(cameraId: Int) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        currCameraId = cameraId
        startBackgroundThread()
        cameraManager?.openCamera(cameraId.toString(), stateCallback, backgroundHandler)
    }

    private fun initPreview() {
        try {
            previewRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            if (surfaceTexture != null && previewSurface == null) { // use texture view
                surfaceTexture?.setDefaultBufferSize(previewSize?.width ?: 0, previewSize?.height ?: 0)
                previewSurface = Surface(surfaceTexture)
            }
            previewRequestBuilder?.addTarget(previewSurface!!)
            cameraDevice?.createCaptureSession(
                arrayListOf(previewSurface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(this::javaClass.name, "ConfigureFailed. session: captureSession")
                    }

                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        previewRequest = previewRequestBuilder?.build()
                        startPreview()
                    }
                }, backgroundHandler
            )
        } catch (e: Exception) {
            e.printStackTrace()
            // Log.e(this::javaClass.name, e.message)
        }
    }

    fun startPreview() {
        if (captureSession != null && previewRequest != null) {
            try {
                captureSession?.setRepeatingRequest(previewRequest!!, null, backgroundHandler)
                onStartPreviewListener?.invoke()
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
        }
    }

    fun releaseCamera() {
        captureSession?.close()
        captureSession = null
        cameraDevice?.close()
        cameraDevice = null
        imageReader?.close()
        imageReader = null
        previewSurface?.release()
        previewSurface = null
        stopBackgroundThread()
    }

    fun changeCamera(cameraId: Int) {
        if (currCameraId != cameraId) {
            releaseCamera()
            openCamera(cameraId)
        }
    }

    private fun startBackgroundThread() {
        if (backgroundThread == null || backgroundHandler == null) {
            backgroundThread = HandlerThread("CameraBackground")
            backgroundThread?.start()
            backgroundHandler = Handler(backgroundThread!!.looper)
        }
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.quit()
            backgroundHandler?.removeCallbacksAndMessages(null)
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private fun getFitSize(sizes: Array<Size>): Size? {
        if (width < height) {
            val t = height
            height = width
            width = t
        }
        for (size in sizes) {
            if (1.0f * size.width / size.height == 1.0f * width / height) {
                return size
            }
        }
        return sizes[0]
    }

    class StateCallback(private val cameraWeakReference: WeakReference<LivePusherCamera>) :
        CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraWeakReference.get()?.cameraDevice = camera
            cameraWeakReference.get()?.initPreview()
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraWeakReference.get()?.releaseCamera()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraWeakReference.get()?.releaseCamera()
        }
    }
}