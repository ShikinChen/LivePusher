package me.shiki.livepusher

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.yanzhenjie.permission.AndPermission
import com.yanzhenjie.permission.runtime.Permission
import kotlinx.android.synthetic.main.activity_live_push.*
import me.shiki.livepusher.encodec.MediaEncodec
import me.shiki.livepusher.ext.getScreenHeight
import me.shiki.livepusher.ext.getScreenWidth
import me.shiki.livepusher.push.PushVideo

class LivePushActivity : AppCompatActivity() {

    private val pushVideo: PushVideo by lazy {
        PushVideo()
    }
    private var start = false

    var pushEncodec: MediaEncodec? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndPermission.with(this)
            .runtime()
            .permission(Permission.CAMERA)
            .onGranted {

            }
            .onDenied {

            }
            .start()
        setContentView(R.layout.activity_live_push)
        pushVideo.onConnecting = {
            Log.d(this::javaClass.name, "连接中")
        }
        pushVideo.onConnectSuccess = {
            Log.d(this::javaClass.name, "连接成功")
            pushEncodec = MediaEncodec(this, cv.textureId)
            pushEncodec?.initEncodec(
                cv.getEglContext(),
                resources.getScreenWidth() / 2,
                resources.getScreenHeight() / 2
            )
            pushEncodec?.onSpsAndPpsInfo = { sps, pps ->
                pushVideo.pushSpsAndPps(sps, pps)
            }
            pushEncodec?.onVideoInfo = { data, isKeyFrame ->
                pushVideo.pushVideoData(data, isKeyFrame)
            }
            pushEncodec?.startRecord()
        }
        pushVideo.onConnectFail = {
            Log.d(this::javaClass.name, "连接失败:${it}")
        }
        btn_start.setOnClickListener {
            if (!start) {
                start = true
                btn_start.text = "停止"
                pushVideo.initLivePush("rtmp://192.168.121.102:1935/myapp/live")
            } else {
                btn_start.text = "开始"
                pushEncodec?.stopRecord()
                pushEncodec = null
            }

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cv.onDestroy()
        pushEncodec?.stopRecord()
        pushEncodec = null
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        cv.prewviewAngle()
    }
}
