package me.shiki.livepusher.encodec

import android.content.Context
import me.shiki.livepusher.RenderMode

class MediaEncodec(context: Context, textureId: Int) : BaseMediaEncodec(context) {
    val encodecRender: EncodecRender by lazy {
        EncodecRender(textureId)
    }

    init {
        render = encodecRender
        renderMode = RenderMode.CONTINUOUSLY
    }
}