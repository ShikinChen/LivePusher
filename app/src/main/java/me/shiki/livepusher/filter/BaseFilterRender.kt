package me.shiki.livepusher.filter

import me.shiki.livepusher.BaseMarkRender

/**
 *
 * @author shiki
 * @date 2020/7/9
 *
 */
abstract class BaseFilterRender(var textureId: Int = -1) : BaseMarkRender() {
    init {
        this.fragmentSource = createFragmentSource()
    }

    abstract fun createFragmentSource(): String

    override fun onDrawFrame() {
        onDraw(textureId)
    }
}