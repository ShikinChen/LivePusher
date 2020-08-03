package me.shiki.livepusher

import android.Manifest
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.permissionx.guolindev.PermissionX
import com.ywl5320.libmusic.WlMusic
import com.ywl5320.listener.OnShowPcmDataListener
import kotlinx.android.synthetic.main.activity_img_video.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.shiki.livepusher.encodec.MediaEncodec
import java.io.File

//TODO 录制后比例
class ImgVideoActivity : AppCompatActivity() {

    val music: WlMusic by lazy {
        WlMusic.getInstance()
    }

    var mediaEncodec: MediaEncodec? = null

    private val framesNum = 253
    private val sleppTime = 40L

    private var width = 320
    private var height = 240
    private var stop = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_img_video)
        ivv.setCurrentImg(R.drawable.img_001)
        ivv.post {
            val r = ivv.width / width.toFloat()
            height = (height * r).toInt()
            width = (width * r).toInt()
        }
        music.setCallBackPcmData(true)
        music.setOnPreparedListener {
            val start = 10
            music.playCutAudio(start, start + (framesNum * sleppTime / 1000).toInt())
        }
        music.setOnCompleteListener {
            stop()
        }


        music.setOnShowPcmDataListener(object : OnShowPcmDataListener {
            override fun onPcmInfo(samplerate: Int, bit: Int, channels: Int) {
                mediaEncodec = MediaEncodec(ivv.fboTextureId)
                mediaEncodec?.encodecRender?.bitmap = null
                mediaEncodec?.initEncodec(
                    ivv.getEglContext(),
                    width,
                    height,
                    samplerate,
                    channels,
                    "${Environment.getExternalStorageDirectory().absolutePath}/test_img.mp4"
                )
                mediaEncodec?.onMediaTime = {
                    // Log.d(this::javaClass.name, "时间:${it}")
                }
                mediaEncodec?.startRecord()
                play()
            }

            override fun onPcmData(pcmdata: ByteArray?, size: Int, clock: Long) {
                mediaEncodec?.putPCMData(pcmdata, size)
            }
        })


        btn_start.setOnClickListener {

            PermissionX.init(this)
                .permissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .request { allGranted, grantedList, deniedList ->
                    if (allGranted) {
                        if (mediaEncodec == null) {
                            val file = File(Environment.getExternalStorageDirectory().absolutePath + "/test.mp3")
                            if (!file.exists()) {
                                Toast.makeText(this, "背景音乐文件不存在", Toast.LENGTH_LONG).show()
                                return@request
                            }
                            music.source = file.absolutePath
                            music.prePared()
                            btn_start.text = "转换中"
                        } else {
                            mediaEncodec?.stopRecord()
                            music.stop()
                            stop = true
                            mediaEncodec = null
                            btn_start.text = "开始"
                        }
                    }
                }
        }
    }

    private fun stop() {
        mediaEncodec?.stopRecord()
        mediaEncodec = null
        lifecycleScope.launch {
            btn_start.text = "开始"
        }
    }

    private fun play() {
        stop = false
        lifecycleScope.launch(Dispatchers.IO) {
            val str = StringBuffer()
            for (i in 1..framesNum) {
                if (stop) {
                    break
                }
                if (i < 10) {
                    str.append("00${i}")
                } else if (i < 100) {
                    str.append("0${i}")
                } else {
                    str.append(i)
                }
                val imgSrc = resources.getIdentifier(
                    "img_$str",
                    "drawable",
                    "me.shiki.livepusher"
                )
                ivv.setCurrentImg(imgSrc)
                str.delete(0, str.length)
                Thread.sleep(sleppTime)
            }
            stop()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
