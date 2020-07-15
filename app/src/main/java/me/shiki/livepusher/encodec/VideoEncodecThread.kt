package me.shiki.livepusher.encodec

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import me.shiki.livepusher.ext.byteToHex
import java.lang.ref.WeakReference
import java.nio.ByteBuffer

class VideoEncodecThread(private var mediaEncoderWeakReference: WeakReference<BaseMediaEncodec>?) : Thread() {

    var isExit = false
        private set

    var videoTrackIndex = -1
        private set
    private var pts: Long = 0
    private var sps: ByteArray? = null
    private var pps: ByteArray? = null
    private var isKeyFrame: Boolean = false

    override fun run() {
        super.run()
        videoTrackIndex = -1
        pts = 0
        isExit = false
        val videoEncodec: MediaCodec? = mediaEncoderWeakReference?.get()?.videoEncodec
        val videoBufferInfo: MediaCodec.BufferInfo? = mediaEncoderWeakReference?.get()?.videoBufferInfo
        val mediaMuxer: MediaMuxer? = mediaEncoderWeakReference?.get()?.mediaMuxer
        videoEncodec?.start()
        while (!isExit) {
            if (videoBufferInfo != null) {
                isKeyFrame = false
                var outputBufferIndex = videoEncodec?.dequeueOutputBuffer(videoBufferInfo, 0) ?: -1

                if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (videoEncodec != null) {
                        videoTrackIndex = mediaMuxer?.addTrack(videoEncodec.outputFormat) ?: -1
                        if (mediaEncoderWeakReference?.get()?.audioEncodecThread?.audioTrackIndex != -1) {
                            mediaMuxer?.start()
                            mediaEncoderWeakReference?.get()?.encodecStart = true
                        }
                        if (mediaMuxer == null) {
                            mediaEncoderWeakReference?.get()?.encodecStart = true
                            val spsb: ByteBuffer = videoEncodec.outputFormat.getByteBuffer("csd-0")
                            sps = ByteArray(spsb.remaining())
                            spsb.get(sps, 0, sps!!.size)

                            val ppsb: ByteBuffer = videoEncodec.outputFormat.getByteBuffer("csd-1")
                            pps = ByteArray(ppsb.remaining())
                            ppsb.get(pps, 0, pps!!.size)
                            // Log.d(this::javaClass.name, "sps:${sps?.byteToHex()}")
                            // Log.d(this::javaClass.name, "pps:${pps?.byteToHex()}")
                        }
                    }
                } else {
                    while (outputBufferIndex >= 0) {
                        if (mediaEncoderWeakReference?.get()?.encodecStart == true) {
                            //修正时间戳
                            if (pts == 0L) {
                                pts = videoBufferInfo.presentationTimeUs
                            }
                            videoBufferInfo.presentationTimeUs = videoBufferInfo.presentationTimeUs - pts

                            val outputBuffer = videoEncodec?.getOutputBuffer(outputBufferIndex)
                            outputBuffer?.let {
                                it.position(videoBufferInfo.offset)
                                it.limit(videoBufferInfo.offset + videoBufferInfo.size)
                                //保存写入文件
                                mediaMuxer?.writeSampleData(videoTrackIndex, it, videoBufferInfo)

                                //直播推流
                                if (mediaMuxer == null) {
                                    val data = ByteArray(it.remaining())
                                    outputBuffer.get(data, 0, data.size)
                                    if (videoBufferInfo.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME) {
                                        isKeyFrame = true
                                        if (sps != null && pps != null) {
                                            mediaEncoderWeakReference?.get()?.onSpsAndPpsInfo?.invoke(
                                                sps!!, pps!!
                                            )
                                        }
                                    }
                                    mediaEncoderWeakReference?.get()?.onVideoInfo?.invoke(
                                        data, isKeyFrame
                                    )
                                }
                            }
                            mediaEncoderWeakReference?.get()?.onMediaTime?.invoke(
                                videoBufferInfo.presentationTimeUs / 1000000
                            )
                        }
                        videoEncodec?.releaseOutputBuffer(outputBufferIndex, false)
                        outputBufferIndex = videoEncodec?.dequeueOutputBuffer(videoBufferInfo, 0) ?: -1
                    }
                }
            }
        }
        videoEncodec?.stop()
        videoEncodec?.release()
        mediaEncoderWeakReference?.get()?.videoExit = true
        if (mediaEncoderWeakReference?.get()?.audioExit != false) {
            mediaMuxer?.stop()
            mediaMuxer?.release()
            Log.d(this::javaClass.name, "videoExit")
        }
    }

    fun exit() {
        isExit = true
    }
}