package me.shiki.livepusher.encodec

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.lang.ref.WeakReference

class VideoEncodecThread(private var mediaEncoderWeakReference: WeakReference<BaseMediaEncodec>?) : Thread() {

    var isExit = false
        private set

    var videoTrackIndex = -1
        private set
    private var pts: Long = 0

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
                var outputBufferIndex = videoEncodec?.dequeueOutputBuffer(videoBufferInfo, 0) ?: -1

                if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (videoEncodec != null) {
                        videoTrackIndex = mediaMuxer?.addTrack(videoEncodec.outputFormat) ?: -1
                        if (mediaEncoderWeakReference?.get()?.audioEncodecThread?.audioTrackIndex != -1) {
                            mediaMuxer?.start()
                            mediaEncoderWeakReference?.get()?.encodecStart = true
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
                                //写入文件
                                mediaMuxer?.writeSampleData(videoTrackIndex, it, videoBufferInfo)
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