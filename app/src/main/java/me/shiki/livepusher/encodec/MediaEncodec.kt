package me.shiki.livepusher.encodec

import me.shiki.livepusher.BaseMarkRender
import me.shiki.livepusher.RenderMode

class MediaEncodec(textureId: Int) : BaseMediaEncodec() {
    //TODO 水印无法显示
    var encodecRender: BaseMarkRender = EncodecRender(textureId)
        set(value) {
            field = value
            render = value
        }

    init {
        render = encodecRender
        renderMode = RenderMode.CONTINUOUSLY
    }
}