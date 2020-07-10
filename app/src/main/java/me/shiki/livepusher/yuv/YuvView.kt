package me.shiki.livepusher.yuv

import android.content.Context
import android.util.AttributeSet
import me.shiki.livepusher.RenderMode
import me.shiki.livepusher.egl.EGLSurfaceView

/**
 * YuvView
 *
 * @author shiki
 * @date 2020/7/10
 *
 */
class YuvView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : EGLSurfaceView(context, attrs, defStyleAttr) {
    private val yuvRender = YuvRender()

    init {
        render = yuvRender
        renderMode = RenderMode.WHEN_DIRTY
    }

    fun setFrameData(width: Int, height: Int, y: ByteArray, u: ByteArray, v: ByteArray) {
        yuvRender.setFrameData(width, height, y, u, v)
        requestRender()
    }
}