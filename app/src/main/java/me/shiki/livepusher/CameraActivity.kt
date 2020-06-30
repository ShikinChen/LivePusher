package me.shiki.livepusher

import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.yanzhenjie.permission.Action
import com.yanzhenjie.permission.AndPermission
import com.yanzhenjie.permission.runtime.Permission
import kotlinx.android.synthetic.main.activity_camera.*
import kotlinx.android.synthetic.main.activity_main.*

class CameraActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        AndPermission.with(this)
            .runtime()
            .permission(Permission.CAMERA)
            .onGranted {

            }
            .onDenied {

            }
            .start()

        btn_change.setOnClickListener {
            cv.changeCamera()
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
