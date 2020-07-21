package me.shiki.livepusher.filter.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import javax.microedition.khronos.egl.EGLContext

/**
 * BaseFilter
 *
 * @author shiki
 * @date 2020/7/21
 *
 */
abstract class BaseFilter(var eglContext: EGLContext?, var textureId: Int)