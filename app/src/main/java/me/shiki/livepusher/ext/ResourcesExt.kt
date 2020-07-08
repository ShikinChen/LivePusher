package me.shiki.livepusher.ext

import android.content.res.Resources

fun Resources.getScreenWidth(): Int {
    return displayMetrics.widthPixels
}

fun Resources.getScreenHeight(): Int {
    return displayMetrics.heightPixels
}
