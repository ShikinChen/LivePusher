package me.shiki.livepusher.camera

import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.util.Log
import me.shiki.livepusher.egl.EGLRender
import me.shiki.livepusher.egl.ShaderUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class CameraRender : EGLRender, SurfaceTexture.OnFrameAvailableListener {

    private val vertexData = floatArrayOf(
        -1f, -1f,
        1f, -1f,
        -1f, 1f,
        1f, 1f
    )
    private val vertexBuffer: FloatBuffer by lazy {
        ByteBuffer.allocateDirect(vertexData.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertexData)
    }

    private val fragmentData = floatArrayOf(
        0f, 1f,
        1f, 1f,
        0f, 0f,
        1f, 0f
    )

    private val fragmentBuffer: FloatBuffer by lazy {
        ByteBuffer.allocateDirect(fragmentData.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(fragmentData)
    }

    private val vertexSource: String = """
        attribute vec4 v_Position;
        attribute vec2 f_Position;
        varying vec2 ft_Position;
        void main() {
            ft_Position = f_Position;
            gl_Position = v_Position;
        }
  """

    private val fragmentSource: String = """
        #extension GL_OES_EGL_image_external : require
        precision mediump float;
        varying vec2 ft_Position;
        uniform samplerExternalOES sTexture;
        void main() {
            gl_FragColor=texture2D(sTexture, ft_Position);
        }
  """

    init {
        vertexBuffer.position(0)
        fragmentBuffer.position(0)
    }

    private var program = 0
    private var vPosition = 0
    private var fPosition = 0

    private var vboId = 0
    private var fboId = 0

    private var fboTextureId = 0
    private var cameraTextureId = 0

    private var surfaceTexture: SurfaceTexture? = null
    private val onSurfaceCreateListener: ((surfaceTexture: SurfaceTexture?) -> Unit)? = null

    override fun onSurfaceCreated() {
        program = ShaderUtil.createProgram(vertexSource, fragmentSource)
        vPosition = GLES20.glGetAttribLocation(program, "v_Position")
        fPosition = GLES20.glGetAttribLocation(program, "f_Position")

        //vbo
        val vbos = IntArray(1)
        GLES20.glGenBuffers(1, vbos, 0)
        vboId = vbos[0]

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId)
        GLES20.glBufferData(
            GLES20.GL_ARRAY_BUFFER,
            vertexData.size * 4 + fragmentData.size * 4,
            null,
            GLES20.GL_STATIC_DRAW
        )
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, vertexData.size * 4, vertexBuffer)
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, vertexData.size * 4, fragmentData.size * 4, fragmentBuffer)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)

        //fbo
        val fbos = IntArray(1)
        GLES20.glGenBuffers(1, fbos, 0)
        fboId = fbos[0]
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId)

        val textureIds = IntArray(1)
        GLES20.glGenTextures(1, textureIds, 0)
        fboTextureId = textureIds[0]

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTextureId)

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D,
            0,
            GLES20.GL_RGBA,
            720,
            1280,
            0,
            GLES20.GL_RGBA,
            GLES20.GL_UNSIGNED_BYTE,
            null
        )
        GLES20.glFramebufferTexture2D(
            GLES20.GL_FRAMEBUFFER,
            GLES20.GL_COLOR_ATTACHMENT0,
            GLES20.GL_TEXTURE_2D,
            fboTextureId,
            0
        )
        if (GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            Log.e(this::class.java.name, "fbo wrong")
        } else {
            Log.e(this::class.java.name, "fbo success")
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)

        val textureIdseos = IntArray(1)
        GLES20.glGenTextures(1, textureIdseos, 0)
        cameraTextureId = textureIdseos[0]

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

        surfaceTexture = SurfaceTexture(cameraTextureId)
        surfaceTexture?.setOnFrameAvailableListener(this)

        onSurfaceCreateListener?.invoke(surfaceTexture)

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
    }

    override fun onSurfaceChanged(width: Int, height: Int) {
    }

    override fun onDrawFrame() {
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
    }
}