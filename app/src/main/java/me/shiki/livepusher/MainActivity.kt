package me.shiki.livepusher

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.yanzhenjie.permission.Action
import com.yanzhenjie.permission.AndPermission
import com.yanzhenjie.permission.runtime.Permission
import kotlinx.android.synthetic.main.activity_camera.*
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btn_preview.setOnClickListener {
            startActivity(Intent(this, CameraActivity::class.java))
        }
        btn_record.setOnClickListener {
            startActivity(Intent(this, VideoActivity::class.java))
        }

        btn_img_video.setOnClickListener {
            startActivity(Intent(this, ImgVideoActivity::class.java))
        }

        btn_yuv.setOnClickListener {
            startActivity(Intent(this, YuvActivity::class.java))
        }
    }
}
