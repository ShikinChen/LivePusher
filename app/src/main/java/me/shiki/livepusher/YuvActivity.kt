package me.shiki.livepusher

import android.Manifest
import android.content.res.Configuration
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.permissionx.guolindev.PermissionX
import kotlinx.android.synthetic.main.activity_img_video.btn_start
import kotlinx.android.synthetic.main.activity_yuv.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class YuvActivity : AppCompatActivity() {

    private var isExit = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_yuv)

        btn_start.setOnClickListener {
            if (!isExit) {
                isExit = true
                btn_start.text = "开始"
                return@setOnClickListener
            }


            PermissionX.init(this)
                .permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                .request { allGranted, grantedList, deniedList ->
                    if (allGranted) {
                        lifecycleScope.launch {
                            btn_start.text = "播放中"
                            play()
                            btn_start.text = "开始"
                        }
                    }
                }
        }
    }

    private suspend fun play() {
        isExit = false
        withContext(Dispatchers.IO) {
            Log.d(this::class.java.name, "thread name:" + Thread.currentThread().name)
            val width = 352
            val height = 288
            val y = ByteArray(width * height)
            val u = ByteArray(width * height / 4)
            val v = ByteArray(width * height / 4)
            try {
                val inputStream =
                    File(Environment.getExternalStorageDirectory().absolutePath + "/test.yuv").inputStream()
                while (!isExit) {
                    val ySize = inputStream.read(y)
                    val uSize = inputStream.read(u)
                    val vSize = inputStream.read(v)
                    if (ySize > 0 && uSize > 0 && vSize > 0) {
                        yv.setFrameData(width, height, y, u, v)
                        Thread.sleep(60)
                    } else {
                        isExit = true
                    }
                }
            } catch (e: Exception) {
                Log.e(this::class.java.name, e.message)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }
}
