package me.shiki.livepusher.camera

import android.content.Context
import android.hardware.Camera
import android.util.AttributeSet
import me.shiki.livepusher.egl.EGLSurfaceView

class CameraView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : EGLSurfaceView(context, attrs, defStyleAttr) {

    private val cameraRender: CameraRender by lazy {
        CameraRender()
    }
    private val camera: LivePusherCamera by lazy {
        LivePusherCamera()
    }

    var cameraId: Int = Camera.CameraInfo.CAMERA_FACING_BACK

    init {
        render = cameraRender
        cameraRender.onSurfaceCreateListener = {
            camera.initCamera(it, cameraId)
        }
    }

    fun onDestroy() {
        camera.stopPreview()
    }
}