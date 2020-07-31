package me.shiki.livepusher

import android.content.res.Configuration
import android.media.MediaFormat
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.yanzhenjie.permission.AndPermission
import com.yanzhenjie.permission.runtime.Permission
import com.ywl5320.libmusic.WlMusic
import com.ywl5320.listener.OnShowPcmDataListener
import kotlinx.android.synthetic.main.activity_video.*
import kotlinx.coroutines.launch
import me.shiki.livepusher.encodec.MediaEncodec
import java.io.File

class VideoActivity : AppCompatActivity() {

    val music: WlMusic by lazy {
        WlMusic.getInstance()
    }

    var mediaEncodec: MediaEncodec? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)
        AndPermission.with(this)
            .runtime()
            .permission(Permission.Group.CAMERA)
            .onGranted {

            }
            .onDenied {

            }
            .start()

        music.setCallBackPcmData(true)
        music.setOnPreparedListener {
            music.playCutAudio(10, 20)
        }
        music.setOnCompleteListener {
            mediaEncodec?.stopRecord()
            mediaEncodec = null
            lifecycleScope.launch {
                btn_record.text = "开始录制"
            }
        }

        music.setOnShowPcmDataListener(object : OnShowPcmDataListener {
            override fun onPcmInfo(samplerate: Int, bit: Int, channels: Int) {
                mediaEncodec = MediaEncodec(this@VideoActivity, cv.textureId)
                mediaEncodec?.initEncodec(
                    cv.getEglContext(),

                    720,
                    1280,
                    samplerate,
                    channels,
                    "${Environment.getExternalStorageDirectory().absolutePath}/test.mp4"
                )
                mediaEncodec?.onMediaTime = {
                    // Log.d(this::javaClass.name, "时间:${it}")
                }
                mediaEncodec?.startRecord()
            }

            override fun onPcmData(pcmdata: ByteArray?, size: Int, clock: Long) {
                mediaEncodec?.putPCMData(pcmdata, size)
            }
        })

        btn_record.setOnClickListener {
            AndPermission.with(this)
                .runtime()
                .permission(Permission.Group.STORAGE)
                .onGranted {
                    if (mediaEncodec == null) {
                        val file = File(Environment.getExternalStorageDirectory().absolutePath + "/test.mp3")
                        if (!file.exists()) {
                            Toast.makeText(this, "背景音乐文件不存在", Toast.LENGTH_LONG).show()
                            return@onGranted
                        }
                        music.source = file.absolutePath
                        music.prePared()
                        btn_record.text = "录制中"
                    } else {
                        mediaEncodec?.stopRecord()
                        music.stop()
                        mediaEncodec = null
                        btn_record.text = "开始录制"
                    }
                }
                .onDenied {

                }
                .start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cv.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        cv.prewviewAngle()
    }
}
