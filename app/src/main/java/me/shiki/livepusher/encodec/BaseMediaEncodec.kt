package me.shiki.livepusher.encodec

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.view.Surface
import me.shiki.commlib.constant.Consts
import me.shiki.livepusher.RenderMode
import me.shiki.livepusher.egl.EGLHelper
import me.shiki.livepusher.egl.EGLRender
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.concurrent.locks.ReentrantLock
import javax.microedition.khronos.egl.EGLContext
import kotlin.concurrent.withLock

open class BaseMediaEncodec(context: Context) {

    private var surface: Surface? = null
    private var eglContext: EGLContext? = null

    var width = 0
    var height = 0

    var renderMode = RenderMode.CONTINUOUSLY

    private var videoEncodec: MediaCodec? = null
    private var videoFromat: MediaFormat? = null
    private var videoBufferInfo: MediaCodec.BufferInfo? = null

    private var eglMediaThread: EGLMediaThread? = null
    private var videoEncodecThread: VideoEncodecThread? = null

    var render: EGLRender? = null

    private var mediaMuxer: MediaMuxer? = null

    var onMediaTime: ((Long) -> Unit)? = null

    fun initEncodec(eglContext: EGLContext?, savePath: String, mimeType: String, width: Int, height: Int) {
        this.width = width
        this.height = height
        this.eglContext = eglContext
        initMediaEncodec(savePath, mimeType, width, height)
    }

    private fun initMediaEncodec(savePath: String, mimeType: String, width: Int, height: Int) {
        try {
            mediaMuxer = MediaMuxer(savePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            initVideoEncodec(mimeType, width, height)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun initVideoEncodec(mimeType: String, width: Int, height: Int) {
        try {
            videoBufferInfo = MediaCodec.BufferInfo()
            //视频格式参数
            videoFromat = MediaFormat.createVideoFormat(mimeType, width, height)
            videoFromat?.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            videoFromat?.setInteger(MediaFormat.KEY_BIT_RATE, width * height * 4)
            videoFromat?.setInteger(MediaFormat.KEY_FRAME_RATE, 30)
            videoFromat?.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            //创建编码器
            videoEncodec = MediaCodec.createEncoderByType(mimeType)
            videoEncodec?.configure(videoFromat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

            surface = videoEncodec?.createInputSurface()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun startRecord() {
        if (surface != null && eglContext != null) {
            eglMediaThread = EGLMediaThread(WeakReference(this))
            videoEncodecThread = VideoEncodecThread(WeakReference(this))
            eglMediaThread?.isCreate = true
            eglMediaThread?.isChange = true
            eglMediaThread?.start()
            videoEncodecThread?.start()
        }
    }

    fun stopRecord() {
        if (eglMediaThread != null && videoEncodecThread != null) {
            videoEncodecThread?.exit()
            eglMediaThread?.onDestroy()
            videoEncodecThread = null
            eglMediaThread = null
        }
    }

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

    class VideoEncodecThread(private var mediaEncoderWeakReference: WeakReference<BaseMediaEncodec>?) : Thread() {

        private var isExit = false

        private var videoTrackIndex = -1
        private var pts: Long = 0

        override fun run() {
            super.run()
            videoTrackIndex = -1
            pts = 0
            isExit = false
            val videoEncodec: MediaCodec? = mediaEncoderWeakReference?.get()?.videoEncodec
            val videoFromat: MediaFormat? = mediaEncoderWeakReference?.get()?.videoFromat
            val videoBufferInfo: MediaCodec.BufferInfo? = mediaEncoderWeakReference?.get()?.videoBufferInfo
            val mediaMuxer: MediaMuxer? = mediaEncoderWeakReference?.get()?.mediaMuxer
            videoEncodec?.start()
            while (!isExit) {
                if (videoBufferInfo != null) {
                    var outputBufferIndex = videoEncodec?.dequeueOutputBuffer(videoBufferInfo, 0) ?: -1

                    if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (videoEncodec != null) {
                            videoTrackIndex = mediaMuxer?.addTrack(videoEncodec.outputFormat) ?: -1
                            mediaMuxer?.start()
                        }
                    } else {
                        while (outputBufferIndex >= 0) {
                            //修正时间戳
                            if (pts == 0L) {
                                pts = videoBufferInfo.presentationTimeUs
                            }
                            videoBufferInfo.presentationTimeUs = videoBufferInfo.presentationTimeUs - pts

                            val outputBuffer = videoEncodec?.getOutputBuffer(outputBufferIndex)
                            outputBuffer?.let {
                                it.position(videoBufferInfo.offset)
                                it.limit(videoBufferInfo.offset + videoBufferInfo.size)
                                //写入文件
                                mediaMuxer?.writeSampleData(videoTrackIndex, it, videoBufferInfo)
                            }
                            mediaEncoderWeakReference?.get()?.onMediaTime?.invoke(
                                videoBufferInfo.presentationTimeUs / 1000000
                            )

                            videoEncodec?.releaseOutputBuffer(outputBufferIndex, false)
                            outputBufferIndex = videoEncodec?.dequeueOutputBuffer(videoBufferInfo, 0) ?: -1
                        }
                    }
                }
            }
            videoEncodec?.stop()
            videoEncodec?.release()
            mediaMuxer?.stop()
            mediaMuxer?.release()
        }

        fun exit() {
            isExit = true
        }
    }
}