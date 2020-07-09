package me.shiki.livepusher.encodec

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.view.Surface
import me.shiki.livepusher.RenderMode
import me.shiki.livepusher.egl.EGLRender
import java.io.IOException
import java.lang.ref.WeakReference
import javax.microedition.khronos.egl.EGLContext

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

    private var audioPts: Long = 0
    private var sampleRate = 0

    var audioExit = false
    var videoExit = false

    @JvmOverloads
    fun initEncodec(
        eglContext: EGLContext?, savePath: String, width: Int, height: Int, sampleRate: Int,
        channelCount: Int
    ) {
        this.width = width
        this.height = height
        this.eglContext = eglContext
        this.sampleRate = sampleRate
        initMediaEncodec(savePath, width, height, sampleRate, channelCount)
    }

    @JvmOverloads
    private fun initMediaEncodec(
        savePath: String,
        width: Int,
        height: Int,
        sampleRate: Int,
        channelCount: Int
    ) {
        try {
            mediaMuxer = MediaMuxer(savePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            initVideoEncodec(MediaFormat.MIMETYPE_VIDEO_AVC, width, height)
            initAudioEncodec(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount)
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
            videoBufferInfo = null
            videoFromat = null
            videoEncodec = null
        }
    }

    private fun initAudioEncodec(mimeType: String, sampleRate: Int, channelCount: Int) {
        try {
            audioBufferInfo = MediaCodec.BufferInfo()
            //音频格式参数
            audioFromat = MediaFormat.createAudioFormat(mimeType, sampleRate, channelCount)
            audioFromat?.setInteger(MediaFormat.KEY_BIT_RATE, 96000)
            audioFromat?.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            audioFromat?.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 4096)
            //创建编码器
            audioEncodec = MediaCodec.createEncoderByType(mimeType)
            audioEncodec?.configure(audioFromat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        } catch (e: IOException) {
            e.printStackTrace()
            audioBufferInfo = null
            audioFromat = null
            audioEncodec = null
        }
    }

    fun startRecord() {
        if (surface != null && eglContext != null) {

            encodecStart = false
            audioExit = false
            videoExit = false
            audioPts = 0

            eglMediaThread = EGLMediaThread(WeakReference(this))
            videoEncodecThread = VideoEncodecThread(WeakReference(this))
            audioEncodecThread = AudioEncodecThread(WeakReference(this))
            eglMediaThread?.isCreate = true
            eglMediaThread?.isChange = true
            eglMediaThread?.start()
            videoEncodecThread?.start()
            audioEncodecThread?.start()
        }
    }

    fun putPCMData(buffer: ByteArray?, size: Int) {
        if (audioEncodecThread != null && audioEncodecThread?.isExit == false && buffer != null && size > 0) {
            val iputBufferIndex = audioEncodec?.dequeueInputBuffer(0) ?: -1
            if (iputBufferIndex >= 0) {
                val byteBuffer = audioEncodec?.getInputBuffer(iputBufferIndex)
                byteBuffer?.clear()
                byteBuffer?.put(buffer)
                val pts = getAudioPts(size, sampleRate)
                audioEncodec?.queueInputBuffer(iputBufferIndex, 0, size, pts, 0)
            }
        }
    }

    private fun getAudioPts(size: Int, sampleRate: Int): Long {
        audioPts += (size.toFloat() / (sampleRate * 2 * 2) * 1000000).toLong()
        return audioPts
    }

    fun stopRecord() {
        if (eglMediaThread != null && videoEncodecThread != null) {
            videoEncodecThread?.exit()
            audioEncodecThread?.exit()
            eglMediaThread?.onDestroy()
            videoEncodecThread = null
            audioEncodecThread = null
            videoEncodec = null
            audioEncodec = null
            eglMediaThread = null
        }
    }
}