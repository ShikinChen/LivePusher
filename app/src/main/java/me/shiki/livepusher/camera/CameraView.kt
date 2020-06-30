package me.shiki.livepusher.camera

import android.content.Context
import android.hardware.Camera
import android.hardware.camera2.CameraCharacteristics
import android.util.AttributeSet
import me.shiki.livepusher.egl.EGLSurfaceView

class CameraView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : EGLSurfaceView(context, attrs, defStyleAttr) {

    private val cameraRender: CameraRender by lazy {
        CameraRender()
    }
    private val camera: LivePusherCamera by lazy {
        LivePusherCamera(context.applicationContext)
    }

    private var cameraId: Int = CameraCharacteristics.LENS_FACING_FRONT

    init {
        render = cameraRender
        cameraRender.onSurfaceCreateListener = {
            camera.initCamera(it, cameraId)
        }
    }

    fun onDestroy() {
        camera.releaseCamera()
    }
}