package me.shiki.livepusher.push

/**
 * PushVideo
 *
 * @author shiki
 * @date 2020/7/13
 *
 */
class PushVideo {
    companion object {
        init {
            System.loadLibrary("push-lib")
        }
    }

    var onConnecting: (() -> Unit)? = null

    var onConnectSuccess: (() -> Unit)? = null

    var onConnectFail: ((msg: String) -> Unit)? = null

    fun initLivePush(pushUrl: String) {
        if (pushUrl.isNotEmpty()) {
            initPush(pushUrl)
        }
    }

    fun pushSpsAndPps(sps: ByteArray, pps: ByteArray) {
        pushSpsAndPps(sps, sps.size, pps, pps.size)
    }

    fun pushVideoData(data: ByteArray, isKeyFrame: Boolean) {
        pushVideoData(data, data.size, isKeyFrame)
    }

    fun pushAudioData(data: ByteArray) {
        pushAudioData(data, data.size)
    }

    fun stop() {
        pushStop()
    }

    private fun onConnecting() {
        onConnecting?.invoke()
    }

    private fun onConnectSuccess() {
        onConnectSuccess?.invoke()
    }

    private fun onConnectFail(msg: String) {
        onConnectFail?.invoke(msg)
    }

    private external fun initPush(pushUrl: String)

    private external fun pushSpsAndPps(sps: ByteArray, spsLen: Int, pps: ByteArray, ppsLen: Int)

    private external fun pushVideoData(data: ByteArray, dataLen: Int, isKeyFrame: Boolean)

    private external fun pushAudioData(data: ByteArray, dataLen: Int)

    private external fun pushStop()
}