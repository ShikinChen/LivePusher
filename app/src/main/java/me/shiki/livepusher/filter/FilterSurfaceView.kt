package me.shiki.livepusher.filter

import android.content.Context
import android.util.AttributeSet
import me.shiki.livepusher.egl.EGLSurfaceView
import javax.microedition.khronos.egl.EGLContext

/**
 * MutiSurfaceView
 *
 * @author shiki
 * @date 2020/7/17
 *
 */
class FilterSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    EGLSurfaceView(context, attrs, defStyleAttr) {
    private val filterRender by lazy {
        FilterRender()
    }

    init {
        render = filterRender
    }

    fun setTextureIdAndEglcontext(textureId: Int, eglContext: EGLContext?) {
        setSurfaceAndEglContext(eglContext = eglContext)
        filterRender.textureId = textureId
    }
}