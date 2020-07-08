package me.shiki.livepusher.encodec

import me.shiki.commlib.constant.Consts
import me.shiki.livepusher.RenderMode
import me.shiki.livepusher.egl.EGLHelper
import java.lang.ref.WeakReference
import java.util.concurrent.locks.ReentrantLock
import javax.microedition.khronos.egl.EGLContext
import kotlin.concurrent.withLock

class EGLMediaThread(private var mediaEncoderWeakReference: WeakReference<BaseMediaEncodec>?) : Thread() {
    private var eglHelper: EGLHelper? = null

    private val lock by lazy {
        ReentrantLock()
    }
    private val condition by lazy {
        lock.newCondition()
    }

    private var isExit = false
    var isCreate = false
    var isChange = false
    private var isStart = false

    override fun run() {
        super.run()
        isExit = false
        isStart = false

        eglHelper = EGLHelper()
        eglHelper?.initEgl(
            mediaEncoderWeakReference?.get()?.surface,
            mediaEncoderWeakReference?.get()?.eglContext
        )

        while (!isExit) {
            if (isStart) {
                with(mediaEncoderWeakReference?.get()?.renderMode) {
                    if (this == RenderMode.WHEN_DIRTY) {
                        lock.withLock {
                            condition.await()
                        }
                    } else {
                        sleep(Consts.FPS_TIME)
                    }
                }
            }
            onCreate()
            onChange(
                mediaEncoderWeakReference?.get()?.width ?: 0,
                mediaEncoderWeakReference?.get()?.height ?: 0
            )
            onDraw()
            isStart = true
        }
        release()
    }

    private fun onCreate() {
        mediaEncoderWeakReference?.get()?.render?.let {
            if (isCreate) {
                isCreate = false
                it.onSurfaceCreated()
            }
        }
    }

    private fun onDraw() {
        with(mediaEncoderWeakReference?.get()?.render) {
            if (this != null && eglHelper != null) {
                onDrawFrame()
                if (!isStart) {
                    onDrawFrame()
                }
                eglHelper?.swapBuffers()
            }
        }
    }

    private fun onChange(width: Int, height: Int) {
        mediaEncoderWeakReference?.get()?.render?.let {
            if (isChange) {
                isChange = false
                it.onSurfaceChanged(width, height)
            }
        }
    }

    private fun release() {
        eglHelper?.let {
            it.destroyEgl()
            eglHelper = null
            mediaEncoderWeakReference = null
        }
    }

    fun requestRender() {
        lock.withLock {
            condition.signal()
        }
    }

    fun onDestroy() {
        isExit = true
        requestRender()
    }

    fun getEglContext(): EGLContext? {
        return eglHelper?.eglContext
    }
}