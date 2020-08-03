package me.shiki.livepusher.camera

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.util.AttributeSet
import android.util.Log
import android.util.Size
import android.view.Surface
import me.shiki.livepusher.BaseMarkRender
import me.shiki.livepusher.egl.EGLSurfaceView
import me.shiki.livepusher.ext.getRotation
import javax.microedition.khronos.egl.EGLContext

//TODO 优化切换摄像头画面滞留问题
class CameraView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : EGLSurfaceView(context, attrs, defStyleAttr) {

    private val cameraRender: CameraRender by lazy {
        CameraRender()
    }
    private val camera: LivePusherCamera by lazy {
        LivePusherCamera(context.applicationContext)
    }

    var cameraId: Int = CameraCharacteristics.LENS_FACING_BACK
        private set

    var textureId = -1
        private set

    var onSurfaceCreateListener: ((eglContext: EGLContext?, surfaceTexture: SurfaceTexture?, textureId: Int) -> Unit)? =
        null

    init {
        render = cameraRender
        cameraRender.onSurfaceCreateListener = { surfaceTexture, textureId ->
            camera.initCamera(surfaceTexture)
            this.textureId = textureId
            onSurfaceCreateListener?.invoke(getEglContext(), surfaceTexture, textureId)
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

    fun getPreviewSize(): Size? {
        return camera.previewSize
    }

    fun getPictureSize(): Size? {
        return camera.pictureSize
    }

    fun prewviewAngle() {
        val angle = context.getRotation()
        cameraRender.resetMatrix()
        when (angle) {
            Surface.ROTATION_0 -> {
                if (cameraId == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraRender.setAngle(90f, 0f, 0f, 1f)
                    cameraRender.setAngle(180f, 1f, 0f, 0f)
                } else {
                    cameraRender.setAngle(90f, 0f, 0f, 1f)
                }
            }
            Surface.ROTATION_90 -> {
                if (cameraId == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraRender.setAngle(180f, 0f, 0f, 1f)
                    cameraRender.setAngle(180f, 0f, 1f, 0f)
                } else {
                    cameraRender.setAngle(90f, 0f, 0f, 1f)
                }
            }
            Surface.ROTATION_180 -> {
                if (cameraId == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraRender.setAngle(90f, 0f, 0f, 1f)
                    cameraRender.setAngle(180f, 0f, 1f, 0f)
                } else {
                    cameraRender.setAngle(-90f, 0f, 0f, 1f)
                }
            }
            Surface.ROTATION_270 -> {
                if (cameraId == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraRender.setAngle(180f, 0f, 1f, 0f)
                } else {
                    cameraRender.setAngle(0f, 0f, 0f, 1f)
                }
            }
        }
    }

    fun setCameraFboRender(cameraFboRender: BaseMarkRender?) {
        cameraRender.cameraFboRender = cameraFboRender
        //重置SurfaceCreated状态从而编译和链接新的program
        resetSurfaceCreated()
    }
}