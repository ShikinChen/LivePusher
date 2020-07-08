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

    var surface: Surface? = null
        private set
    var eglContext: EGLContext? = null
        private set

    var width = 0
    var height = 0

    var renderMode = RenderMode.CONTINUOUSLY

    var videoEncodec: MediaCodec? = null
        private set
    var videoFromat: MediaFormat? = null
        private set
    var videoBufferInfo: MediaCodec.BufferInfo? = null
        private set

    var audioEncodec: MediaCodec? = null
        private set
    var audioFromat: MediaFormat? = null
        private set
    var audioBufferInfo: MediaCodec.BufferInfo? = null
        private set

    private var eglMediaThread: EGLMediaThread? = null
    var videoEncodecThread: VideoEncodecThread? = null
        private set
    var audioEncodecThread: AudioEncodecThread? = null
        private set

    var render: EGLRender? = null

    var mediaMuxer: MediaMuxer? = null
        private set

    var encodecStart = false

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

    private fun initAudioEncodec(mimeType: String) {
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
}