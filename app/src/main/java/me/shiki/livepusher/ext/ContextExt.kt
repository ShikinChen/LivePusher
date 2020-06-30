package me.shiki.livepusher.ext

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager

fun Context.getDisplayMetrics(): DisplayMetrics {
    return resources.displayMetrics
}

fun Context.getScreenWidth(): Int {
    return getDisplayMetrics().widthPixels
}

fun Context.getScreenHeight(): Int {
    return getDisplayMetrics().heightPixels
}

fun Context.getWindowManager(): WindowManager {
    return getSystemService(Context.WINDOW_SERVICE) as WindowManager
}

fun Context.getRotation(): Int {
    return getWindowManager().defaultDisplay.rotation
}