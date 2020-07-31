package me.shiki.livepusher.filter.model

import me.shiki.livepusher.filter.BaseFilterRender
import me.shiki.livepusher.filter.FilterRenderFactory
import me.shiki.livepusher.filter.FilterRenderTypeInline
import javax.microedition.khronos.egl.EGLContext

/**
 * BaseFilter
 *
 * @author shiki
 * @date 2020/7/21
 *
 */
class Filter @JvmOverloads constructor(private var filterRenderType: FilterRenderTypeInline? = null) {
    var eglContext: EGLContext? = null
    var render: BaseFilterRender? = null
    var textureId: Int = -1
    val previewRender: BaseFilterRender? by lazy {
        FilterRenderFactory.createFilterRender(filterRenderType, textureId)
    }

    fun initRender(textureId: Int, eglContext: EGLContext?) {
        this.textureId = textureId
        render = FilterRenderFactory.createFilterRender(filterRenderType, textureId)
        this.eglContext = eglContext
    }
}