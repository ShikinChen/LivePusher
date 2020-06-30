package me.shiki.livepusher.camera

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.util.AttributeSet
import android.util.Log
import android.view.Surface
import me.shiki.livepusher.egl.EGLSurfaceView
import me.shiki.livepusher.ext.getRotation

//TODO 优化切换摄像头画面滞留问题
class CameraView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : EGLSurfaceView(context, attrs, defStyleAttr) {

    private val cameraRender: CameraRender by lazy {
        CameraRender(context)
    }
    private val camera: LivePusherCamera by lazy {
        LivePusherCamera(context.applicationContext)
    }

    var cameraId: Int = CameraCharacteristics.LENS_FACING_FRONT
        private set

    init {
        render = cameraRender
        cameraRender.onSurfaceCreateListener = {
            camera.initCamera(it, cameraId)
        }
        camera.onStartPreviewListener = {
            prewviewAngle()
        }
    }

    fun changeCamera() {
        cameraId = if (cameraId == CameraCharacteristics.LENS_FACING_FRONT) {
            CameraCharacteristics.LENS_FACING_BACK
        } else {
            CameraCharacteristics.LENS_FACING_FRONT
        }
        camera.changeCamera(cameraId)
    }

    fun onDestroy() {
        camera.releaseCamera()
    }

    fun prewviewAngle() {
        val angle = context.getRotation()
        cameraRender.resetMatrix()
        when (angle) {
            Surface.ROTATION_0 -> {
                if (cameraId == CameraCharacteristics.LENS_FACING_FRONT) {
                    cameraRender.setAngle(90f, 0f, 0f, 1f)
                    cameraRender.setAngle(180f, 1f, 0f, 0f)
                } else {
                    cameraRender.setAngle(90f, 0f, 0f, 1f)
                }
            }
            Surface.ROTATION_90 -> {
                if (cameraId == CameraCharacteristics.LENS_FACING_FRONT) {
                    cameraRender.setAngle(180f, 0f, 0f, 1f)
                    cameraRender.setAngle(180f, 0f, 1f, 0f)
                } else {
                    cameraRender.setAngle(90f, 0f, 0f, 1f)
                }
            }
            Surface.ROTATION_180 -> {
                if (cameraId == CameraCharacteristics.LENS_FACING_FRONT) {
                    cameraRender.setAngle(90f, 0f, 0f, 1f)
                    cameraRender.setAngle(180f, 0f, 1f, 0f)
                } else {
                    cameraRender.setAngle(-90f, 0f, 0f, 1f)
                }
            }
            Surface.ROTATION_270 -> {
                if (cameraId == CameraCharacteristics.LENS_FACING_FRONT) {
                    cameraRender.setAngle(180f, 0f, 1f, 0f)
                } else {
                    cameraRender.setAngle(0f, 0f, 0f, 1f)
                }
            }
        }
    }
}