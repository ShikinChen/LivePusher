package me.shiki.livepusher.filter

/**
 *
 * @author shiki
 * @date 2020/7/31
 *
 */

inline class FilterRenderTypeInline(private val value: Int) {
    override fun toString(): String {
        return value.toString()
    }
}

object FilterRenderType {
    val GRAY = FilterRenderTypeInline(0)
    val EXPOSURE = FilterRenderTypeInline(1)
}

class FilterRenderFactory {
    companion object {
        @JvmStatic
        @JvmOverloads
        fun createFilterRender(type: FilterRenderTypeInline?, textureId: Int = -1): BaseFilterRender? {
            return when (type) {
                FilterRenderType.GRAY -> GrayFilterRender(textureId)
                FilterRenderType.EXPOSURE -> ExposureFilterRender(textureId)
                else -> null
            }
        }
    }
}