package me.shiki.livepusher.encodec

import me.shiki.livepusher.BaseMarkRender
import me.shiki.livepusher.camera.CameraFboRender

class EncodecRender(private val textureId: Int) : BaseMarkRender() {
    override fun onDrawFrame() {
        onDraw(textureId)
    }
}