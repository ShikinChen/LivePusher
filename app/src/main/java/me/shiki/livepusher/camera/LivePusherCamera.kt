package me.shiki.livepusher.camera

import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.Camera

class LivePusherCamera {

    private var surfaceTexture: SurfaceTexture? = null
    private var camera: Camera? = null
    private var currCameraId: Int = -1

    fun initCamera(surfaceTexture: SurfaceTexture?, cameraId: Int) {
        this.surfaceTexture = surfaceTexture
        setCameraParameters(cameraId)
    }

    private fun setCameraParameters(cameraId: Int) {
        currCameraId = cameraId
        camera = Camera.open(cameraId)
        camera?.setPreviewTexture(surfaceTexture)
        val parameters = camera?.parameters
        parameters?.flashMode = "off"
        parameters?.previewFormat = ImageFormat.NV21
        val pictureSize = parameters?.supportedPictureSizes?.get(0)
        parameters?.setPictureSize(
            pictureSize?.width ?: 0,
            pictureSize?.height ?: 0
        )
        val previewSize = parameters?.supportedPreviewSizes?.get(0)
        parameters?.setPreviewSize(
            previewSize?.width ?: 0,
            previewSize?.height ?: 0
        )
        camera?.parameters = parameters
        camera?.startPreview()
    }

    fun stopPreview() {
        camera?.stopPreview()
        camera?.release()
        camera = null
    }

    fun changeCamera(cameraId: Int) {
        if (currCameraId != cameraId) {
            stopPreview()
            setCameraParameters(cameraId)
        }
    }
}