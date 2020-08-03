package me.shiki.livepusher

import android.Manifest
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.permissionx.guolindev.PermissionX
import kotlinx.android.synthetic.main.activity_live_push.*
import kotlinx.coroutines.launch
import me.shiki.livepusher.encodec.MediaEncodec
import me.shiki.livepusher.filter.FilterRenderType
import me.shiki.livepusher.filter.model.Filter
import me.shiki.livepusher.push.PushVideo

class LivePushActivity : AppCompatActivity() {

    private val filterList: Array<Filter> by lazy {
        arrayOf(
            Filter(FilterRenderType.GRAY),
            Filter(FilterRenderType.EXPOSURE)
        )
    }

    private var curFilter: Filter? = null

    private val pushVideo: PushVideo by lazy {
        PushVideo()
    }
    private var start = false

    var pushEncodec: MediaEncodec? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_push)

        initRecyclerView()

        PermissionX.init(this)
            .permissions(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            .request { allGranted, grantedList, deniedList ->
            }

        pushVideo.onConnecting = {
            Log.d(this::javaClass.name, "连接中")
        }
        pushVideo.onConnectSuccess = {
            Log.d(this::javaClass.name, "连接成功")
            pushEncodec = MediaEncodec(cv.textureId)
            if (curFilter != null && curFilter?.previewRender != null) {
                pushEncodec?.encodecRender = curFilter?.previewRender!!
            }
            pushEncodec?.initEncodec(
                cv.getEglContext(),
                720,
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
        rv_filter.adapter = FilterAdapter(this, filterList).apply {
            onItemClickListener = { _, filter ->
                cv.setCameraFboRender(filter.previewRender)
                curFilter = filter
                pushEncodec?.encodecRender = filter.previewRender!!
            }
        }
        cv.onSurfaceCreateListener = { eglContext, _, textureId ->
            filterList.forEach {
                it.initRender(textureId, eglContext)
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
        curFilter = null
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        cv.prewviewAngle()
    }
}
