package me.shiki.livepusher

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yanzhenjie.permission.AndPermission
import com.yanzhenjie.permission.runtime.Permission
import kotlinx.android.synthetic.main.activity_live_push.*
import kotlinx.coroutines.launch
import me.shiki.livepusher.encodec.MediaEncodec
import me.shiki.livepusher.filter.model.BaseFilter
import me.shiki.livepusher.filter.model.GrayFilter
import me.shiki.livepusher.push.PushVideo

class LivePushActivity : AppCompatActivity() {

    private val filterList: Array<BaseFilter> by lazy {
        arrayOf<BaseFilter>(GrayFilter())
    }

    private val pushVideo: PushVideo by lazy {
        PushVideo()
    }
    private var start = false

    var pushEncodec: MediaEncodec? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_push)

        initRecyclerView()

        AndPermission.with(this)
            .runtime()
            .permission(Permission.CAMERA, Permission.RECORD_AUDIO)
            .onGranted {

            }
            .onDenied {

            }
            .start()

        pushVideo.onConnecting = {
            Log.d(this::javaClass.name, "连接中")
        }
        pushVideo.onConnectSuccess = {
            Log.d(this::javaClass.name, "连接成功")
            pushEncodec = MediaEncodec(this, cv.textureId)
            pushEncodec?.initEncodec(
                cv.getEglContext(),
                720 ,
                1280
            )
            pushEncodec?.onSpsAndPpsInfo = { sps, pps ->
                pushVideo.pushSpsAndPps(sps, pps)
            }
            pushEncodec?.onVideoInfo = { data, isKeyFrame ->
                pushVideo.pushVideoData(data, isKeyFrame)
            }
            pushEncodec?.onAudioInfo = { data ->
                pushVideo.pushAudioData(data)
            }
            pushEncodec?.startRecord()
        }
        pushVideo.onConnectFail = {
            Log.d(this::javaClass.name, "连接失败:${it}")
            stop()
        }
        btn_start.setOnClickListener {
            if (!start) {
                start = true
                btn_start.text = "停止"
                pushVideo.initLivePush("rtmp://192.168.121.102:1935/myapp/live")
            } else {
                stop()
            }

        }
    }

    private fun initRecyclerView() {
        rv_filter.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        rv_filter.adapter = FilterAdapter(this, filterList)
        cv.onSurfaceCreateListener = { eglContext, _, textureId ->
            filterList.forEach {
                it.textureId = textureId
                it.eglContext = eglContext
            }
            lifecycleScope.launch {
                rv_filter.adapter?.notifyDataSetChanged()
            }
        }
    }

    private fun stop() {
        start = false
        lifecycleScope.launch {
            btn_start.text = "开始"
        }
        pushVideo.stop()
        pushEncodec?.stopRecord()
        pushEncodec = null
    }

    override fun onDestroy() {
        super.onDestroy()
        cv.onDestroy()
        pushEncodec?.stopRecord()
        pushVideo.stop()
        pushEncodec = null
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        cv.prewviewAngle()
    }
}
