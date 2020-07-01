package me.shiki.livepusher.encodec

import android.opengl.GLES20
import me.shiki.livepusher.camera.CameraFboRender
import me.shiki.livepusher.egl.EGLRender
import me.shiki.livepusher.egl.ShaderUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class EncodecRender(private val textureId: Int) : EGLRender {
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
        precision mediump float;
        varying vec2 ft_Position;
        uniform sampler2D sTexture;
        void main() {
            gl_FragColor=texture2D(sTexture, ft_Position);
        }
  """

    private var program = 0
    private var vPosition = 0
    private var fPosition = 0

    private var vboId = 0

    private val cameraFboRender: CameraFboRender by lazy {
        CameraFboRender()
    }

    init {
        vertexBuffer.position(0)
        fragmentBuffer.position(0)
    }

    override fun onSurfaceCreated() {
        cameraFboRender.onCreate()
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
    }

    override fun onSurfaceChanged(width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame() {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glClearColor(1f, 1f, 1f, 1f)

        GLES20.glUseProgram(program)


        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

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
    }
}