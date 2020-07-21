package me.shiki.livepusher.encodec

import android.media.MediaCodec
import android.media.MediaMuxer
import android.util.Log
import java.lang.ref.WeakReference

class AudioEncodecThread(private var mediaEncoderWeakReference: WeakReference<BaseMediaEncodec>?) : Thread() {

    var isExit = false
        private set

    var audioTrackIndex = -1
        private set
    private var pts: Long = 0

    override fun run() {
        super.run()
        audioTrackIndex = -1
        pts = 0
        isExit = false
        val audioEncodec: MediaCodec? = mediaEncoderWeakReference?.get()?.audioEncodec
        val audioBufferInfo: MediaCodec.BufferInfo? = mediaEncoderWeakReference?.get()?.audioBufferInfo
        val mediaMuxer: MediaMuxer? = mediaEncoderWeakReference?.get()?.mediaMuxer
        audioEncodec?.start()
        while (!isExit) {
            if (audioBufferInfo != null) {
                var outputBufferIndex = audioEncodec?.dequeueOutputBuffer(audioBufferInfo, 0) ?: -1

                if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (audioEncodec != null) {
                        audioTrackIndex = mediaMuxer?.addTrack(audioEncodec.outputFormat) ?: -1
                        if (mediaEncoderWeakReference?.get()?.videoEncodecThread?.videoTrackIndex != -1) {
                            mediaMuxer?.start()
                            mediaEncoderWeakReference?.get()?.encodecStart = true
                        }
                    }
                } else {
                    while (outputBufferIndex >= 0) {
                        if (mediaEncoderWeakReference?.get()?.encodecStart == true) {
                            //修正时间戳
                            if (pts == 0L) {
                                pts = audioBufferInfo.presentationTimeUs
                            }
                            audioBufferInfo.presentationTimeUs = audioBufferInfo.presentationTimeUs - pts

                            // Log.d(this::javaClass.name, "时间:${audioBufferInfo.presentationTimeUs / 1000000}")

                            val outputBuffer = audioEncodec?.getOutputBuffer(outputBufferIndex)
                            outputBuffer?.let {
                                it.position(audioBufferInfo.offset)
                                it.limit(audioBufferInfo.offset + audioBufferInfo.size)
                                //写入文件
                                mediaMuxer?.writeSampleData(audioTrackIndex, it, audioBufferInfo)

                                //直播推流
                                //TODO 去除杂音
                                if (mediaMuxer == null) {
                                    val data = ByteArray(it.remaining())
                                    outputBuffer.get(data, 0, data.size)
                                    mediaEncoderWeakReference?.get()?.onAudioInfo?.invoke(
                                        data
                                    )
                                }
                            }
                        }
                        audioEncodec?.releaseOutputBuffer(outputBufferIndex, false)
                        outputBufferIndex = audioEncodec?.dequeueOutputBuffer(audioBufferInfo, 0) ?: -1
                    }
                }
            }
        }
        audioEncodec?.stop()
        audioEncodec?.release()
        mediaEncoderWeakReference?.get()?.audioExit = true
        if (mediaEncoderWeakReference?.get()?.videoExit != false) {
            mediaMuxer?.stop()
            mediaMuxer?.release()
            Log.d(this::javaClass.name, "audioExit")
        }
    }

    fun exit() {
        isExit = true
    }
}