package me.shiki.livepusher

import android.graphics.Bitmap
import android.opengl.GLES20
import me.shiki.livepusher.egl.EGLRender
import me.shiki.livepusher.egl.ShaderUtil
import me.shiki.livepusher.ext.dpToPx
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

abstract class BaseCommRender : EGLRender {

    protected val vertexData = floatArrayOf(
        -1f, -1f,
        1f, -1f,
        -1f, 1f,
        1f, 1f,

        0f, 0f,
        0f, 0f,
        0f, 0f,
        0f, 0f
    )
    protected val vertexBuffer: FloatBuffer by lazy {
        ByteBuffer.allocateDirect(vertexData.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertexData)
    }

    protected val fragmentData = floatArrayOf(
        0f, 1f,
        1f, 1f,
        0f, 0f,
        1f, 0f
    )
    protected val fragmentBuffer: FloatBuffer by lazy {
        ByteBuffer.allocateDirect(fragmentData.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(fragmentData)
    }

    protected var program = 0
    protected var vPosition = 0
    protected var fPosition = 0

    protected var vboId = 0

    protected val vertexSource: String = """
        attribute vec4 v_Position;
        attribute vec2 f_Position;
        varying vec2 ft_Position;
        void main() {
            ft_Position = f_Position;
            gl_Position = v_Position;
        }
  """

    protected val fragmentSource: String = """
        precision mediump float;
        varying vec2 ft_Position;
        uniform sampler2D sTexture;
        void main() {
            gl_FragColor=texture2D(sTexture, ft_Position);
        }
  """

    var bitmap: Bitmap? = null
    protected var bitmapTextureId: Int? = null

    init {
        bitmap = ShaderUtil.createTextImage("Test:测试", 25.dpToPx(), "#ff0000")

        val r = bitmap!!.width.toFloat() / bitmap!!.height
        val w = r * 0.1f

        vertexData[8] = 0.8f - w
        vertexData[9] = -0.8f

        vertexData[10] = 0.8f
        vertexData[11] = -0.8f

        vertexData[12] = 0.8f - w
        vertexData[13] = -0.7f

        vertexData[14] = 0.8f
        vertexData[15] = -0.7f


        vertexBuffer.position(0)
        fragmentBuffer.position(0)
    }

    override fun onSurfaceCreated() {

        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        program = ShaderUtil.createProgram(vertexSource, fragmentSource)

        vPosition = GLES20.glGetAttribLocation(program, "v_Position")
        fPosition = GLES20.glGetAttribLocation(program, "f_Position")

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

        if (bitmap != null) {
            bitmapTextureId = ShaderUtil.loadBitmapTexture(bitmap!!)
        }
    }

    override fun onSurfaceChanged(width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    fun onDraw(textureId: Int) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glClearColor(1f, 1f, 1f, 1f)

        GLES20.glUseProgram(program)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId)

        //fbo
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
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

        //bitmap
        drawBitmap()

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
    }

    protected fun drawBitmap() {
        //bitmap
        bitmapTextureId?.let {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, it)
            GLES20.glEnableVertexAttribArray(vPosition)
            GLES20.glVertexAttribPointer(
                vPosition, 2, GLES20.GL_FLOAT, false, 8,
                32
            )

            GLES20.glEnableVertexAttribArray(fPosition)
            GLES20.glVertexAttribPointer(
                fPosition, 2, GLES20.GL_FLOAT, false, 8,
                vertexData.size * 4
            )
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        }
    }
}