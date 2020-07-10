package me.shiki.livepusher.imgvideo

import android.content.Context
import android.util.AttributeSet
import me.shiki.livepusher.RenderMode
import me.shiki.livepusher.egl.EGLSurfaceView

class ImgVideoView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : EGLSurfaceView(context, attrs, defStyleAttr) {
    private var imgVideoRender: ImgVideoRender = ImgVideoRender(context)
    var fboTextureId = -1
        private set

    init {
        render = imgVideoRender
        renderMode = RenderMode.WHEN_DIRTY
        imgVideoRender.onRenderCreateListener = {
            fboTextureId = it
        }
        requestRender()
    }

    fun setCurrentImg(imgSrc: Int) {
        imgVideoRender.imgSrc = imgSrc
        requestRender()
    }
}