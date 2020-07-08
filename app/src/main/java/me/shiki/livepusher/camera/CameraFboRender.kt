package me.shiki.livepusher.camera

import android.opengl.GLES20
import me.shiki.livepusher.BaseCommRender

class CameraFboRender : BaseCommRender() {
    private var sampler = 0

    override fun onSurfaceCreated() {
        super.onSurfaceCreated()
        sampler = GLES20.glGetUniformLocation(program, "sTexture")
    }

    override fun onDrawFrame() {
    }
}