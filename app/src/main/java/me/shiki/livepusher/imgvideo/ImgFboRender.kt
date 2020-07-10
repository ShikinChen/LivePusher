package me.shiki.livepusher.imgvideo

import android.opengl.GLES20
import me.shiki.livepusher.BaseCommRender

class ImgFboRender : BaseCommRender() {
    private var sampler = 0

    init {
        bitmap = null
    }

    override fun onSurfaceCreated() {
        super.onSurfaceCreated()
        sampler = GLES20.glGetUniformLocation(program, "sTexture")
    }

    override fun onDrawFrame() {
    }
}