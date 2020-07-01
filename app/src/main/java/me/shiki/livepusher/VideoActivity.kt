package me.shiki.livepusher

import android.content.res.Configuration
import android.media.MediaFormat
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.yanzhenjie.permission.AndPermission
import com.yanzhenjie.permission.runtime.Permission
import kotlinx.android.synthetic.main.activity_video.*
import me.shiki.livepusher.encodec.MediaEncodec

class VideoActivity : AppCompatActivity() {

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

        btn_record.setOnClickListener {
            AndPermission.with(this)
                .runtime()
                .permission(Permission.Group.STORAGE)
                .onGranted {
                    if (mediaEncodec == null) {
                        mediaEncodec = MediaEncodec(this, cv.textureId)
                        mediaEncodec?.initEncodec(
                            cv.getEglContext(),
                            "${Environment.getExternalStorageDirectory().absolutePath}/test.mp4",
                            MediaFormat.MIMETYPE_VIDEO_AVC,
                            720,
                            1280
                        )
                        mediaEncodec?.onMediaTime = {
                            Log.d(this::javaClass.name, "时间:${it}")
                        }
                        mediaEncodec?.startRecord()
                        btn_record.text = "录制中"
                    } else {
                        mediaEncodec?.stopRecord()
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
