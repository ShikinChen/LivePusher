package me.shiki.livepusher.camera

import android.content.res.Resources
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Log
import me.shiki.livepusher.BaseMarkRender
import me.shiki.livepusher.egl.EGLRender
import me.shiki.livepusher.egl.ShaderUtil
import me.shiki.livepusher.ext.getScreenHeight
import me.shiki.livepusher.ext.getScreenWidth
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

//TODO 矩阵适配view大小
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
        uniform mat4 u_Matrix;
        void main() {
            ft_Position = f_Position;
            gl_Position = v_Position * u_Matrix;
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
    private val matrix = FloatArray(16)

    private var program = 0
    private var vPosition = 0
    private var fPosition = 0
    private var sampler = 0
    private var uMatrix = 0

    private var vboId = 0
    private var fboId = 0

    private var fboTextureId = -1

    private var cameraTextureId = -1

    private var surfaceTexture: SurfaceTexture? = null
    var onSurfaceCreateListener: ((surfaceTexture: SurfaceTexture?, textureId: Int) -> Unit)? = null

    private var screenWidth = 0
    private var screenHeight = 0

    private var width = 0
    private var height = 0

    var cameraFboRender: BaseMarkRender? = null
    var isOnSurfaceCreated = false

    init {
        vertexBuffer.position(0)
        fragmentBuffer.position(0)
        screenWidth = Resources.getSystem().getScreenWidth()
        screenHeight = Resources.getSystem().getScreenHeight()
    }

    override fun onSurfaceCreated() {
        if (cameraFboRender == null) {
            cameraFboRender = CameraFboRender()
        }

        cameraFboRender?.onSurfaceCreated()

        if (isOnSurfaceCreated) {
            return
        }

        program = ShaderUtil.createProgram(vertexSource, fragmentSource)
        vPosition = GLES20.glGetAttribLocation(program, "v_Position")
        fPosition = GLES20.glGetAttribLocation(program, "f_Position")
        sampler = GLES20.glGetUniformLocation(program, "sTexture")
        uMatrix = GLES20.glGetUniformLocation(program, "u_Matrix")

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

        // GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        // GLES20.glUniform1i(sampler, 0)

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D,
            0,
            GLES20.GL_RGBA,
            screenWidth,
            screenHeight,
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
            Log.d(this::class.java.name, "fbo success")
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

        onSurfaceCreateListener?.invoke(surfaceTexture, fboTextureId)

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)

        isOnSurfaceCreated = true
    }

    fun resetMatrix() {
        Matrix.setIdentityM(matrix, 0)
    }

    fun setAngle(angle: Float, x: Float, y: Float, z: Float) {
        Matrix.rotateM(matrix, 0, angle, x, y, z)
    }

    override fun onSurfaceChanged(width: Int, height: Int) {
        // cameraFboRender.onChange(width, height)
        // GLES20.glViewport(0, 0, width, height)
        this.height = height
        this.width = width
    }

    override fun onDrawFrame() {
        surfaceTexture?.updateTexImage()
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glClearColor(1f, 1f, 1f, 1f)

        GLES20.glUseProgram(program)
        GLES20.glViewport(0, 0, screenWidth, screenHeight)
        GLES20.glUniformMatrix4fv(uMatrix, 1, false, matrix, 0)


        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId)

        GLES20.glEnableVertexAttribArray(vPosition)
        GLES20.glVertexAttribPointer(
            vPosition, 2, GLES20.GL_FLOAT, false, 8,
            0
        )

        GLES20.glEnableVertexAttribArray(fPosition)
        GLES20.glVertexAttribPointer(
            fPosition, 2, GLES20.GL_FLOAT, false, 8,
            vertexData.size * 4
        )

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)

        cameraFboRender?.onSurfaceChanged(width, height)
        cameraFboRender?.onDraw(fboTextureId)
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
    }
}