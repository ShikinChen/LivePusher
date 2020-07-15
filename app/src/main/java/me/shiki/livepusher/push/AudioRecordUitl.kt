package me.shiki.livepusher.push

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder

/**
 * AudioRecordUitl
 *
 * @author shiki
 * @date 2020/7/15
 *
 */
class AudioRecordUitl @JvmOverloads constructor(private val sampleRateInHz: Int = 44100) {

    private val bufferSizeInBytes by lazy {
        AudioRecord.getMinBufferSize(
            sampleRateInHz,
            AudioFormat.CHANNEL_IN_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
    }
    private val audioRecord: AudioRecord by lazy {
        AudioRecord(
            MediaRecorder.AudioSource.MIC, sampleRateInHz, AudioFormat.CHANNEL_IN_STEREO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSizeInBytes
        )
    }
    var isStart: Boolean = false
        private set
    private var readSize = 0

    var onRecordLisener: ((audioData: ByteArray, readSize: Int) -> Unit)? = null


    fun startRecord() {
        Thread {
            isStart = true
            audioRecord.startRecording()
            val audioData = ByteArray(bufferSizeInBytes)
            while (isStart) {
                readSize = audioRecord.read(audioData, 0, bufferSizeInBytes)
                onRecordLisener?.invoke(audioData, readSize)
            }
            audioRecord.release()
        }.start()
    }

    fun stopRecord() {
        isStart = false
    }
}