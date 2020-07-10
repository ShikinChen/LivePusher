package me.shiki.livepusher.imgvideo

import android.content.Context
import android.opengl.GLES20
import android.util.Log
import me.shiki.livepusher.egl.EGLRender
import me.shiki.livepusher.egl.ShaderUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * ImgVideoRender
 *
 * @author shiki
 * @date 2020/7/9
 *
 */
class ImgVideoRender(val context: Context) : EGLRender {

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
        0f, 0f,
        1f, 0f,
        0f, 1f,
        1f, 1f
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
    private var fboTextureId = 0

    private var vboId = 0
    private var fboId = 0

    var imgSrc = 0

    private var imgTextureId = 0

    var onRenderCreateListener: ((textureId: Int) -> Unit)? = null

    private val render: ImgFboRender by lazy {
        ImgFboRender()
    }

    init {
        vertexBuffer.position(0)
        fragmentBuffer.position(0)
    }

    override fun onSurfaceCreated() {
        render.onSurfaceCreated()
        program = ShaderUtil.createProgram(vertexSource, fragmentSource)
        vPosition = GLES20.glGetAttribLocation(program, "v_Position")
        fPosition = GLES20.glGetAttribLocation(program, "f_Position")
    }

    override fun onSurfaceChanged(width: Int, height: Int) {
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
            width,
            height,
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

        onRenderCreateListener?.invoke(fboTextureId)

        GLES20.glViewport(0, 0, width, height)

        render.onSurfaceChanged(width, height)
    }

    override fun onDrawFrame() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glClearColor(1f, 1f, 1f, 1f)
        if (imgSrc > 0) {
            imgTextureId = ShaderUtil.loadTexrute(imgSrc, context)

            GLES20.glUseProgram(program)

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, imgTextureId)


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

            val ids = intArrayOf(imgTextureId)
            GLES20.glDeleteTextures(1, ids, 0)
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        render.onDraw(fboTextureId)
    }
}