package me.shiki.livepusher.encodec

import me.shiki.livepusher.BaseMarkRender
import me.shiki.livepusher.camera.CameraFboRender

class EncodecRender(private val textureId: Int) : BaseMarkRender() {

    private val cameraFboRender: CameraFboRender by lazy {
        CameraFboRender()
    }

    override fun onSurfaceCreated() {
        cameraFboRender.onSurfaceCreated()
        super.onSurfaceCreated()
    }

    override fun onDrawFrame() {
        onDraw(textureId)
    }
}