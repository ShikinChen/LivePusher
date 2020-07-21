package me.shiki.livepusher.filter.model

import kotlinx.android.parcel.Parcelize
import javax.microedition.khronos.egl.EGLContext

/**
 * GrayFilter
 *
 * @author shiki
 * @date 2020/7/21
 *
 */
class GrayFilter @JvmOverloads constructor(eglContext: EGLContext? = null, textureId: Int = -1) : BaseFilter(eglContext,textureId)