package me.shiki.livepusher.yuv

import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Log
import me.shiki.livepusher.egl.EGLRender
import me.shiki.livepusher.egl.ShaderUtil
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * YuvRender
 *
 * @author shiki
 * @date 2020/7/10
 *
 */
class YuvRender : EGLRender {

    private val vertexData = floatArrayOf(
        1f, 1f,
        -1f, 1f,
        1f, -1f,
        -1f, -1f
    )
    private val vertexBuffer: FloatBuffer by lazy {
        ByteBuffer.allocateDirect(vertexData.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertexData)
    }

    private val fragmentData = floatArrayOf(
        1f, 0f,
        0f, 0f,
        1f, 1f,
        0f, 1f
    )
    private val fragmentBuffer: FloatBuffer by lazy {
        ByteBuffer.allocateDirect(fragmentData.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(fragmentData)
    }

    private val matrix = FloatArray(16)

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
        precision mediump float;
        varying vec2 ft_Position;
        uniform sampler2D sampler_y;
        uniform sampler2D sampler_u;
        uniform sampler2D sampler_v;
        void main() {
            float y,u,v;
            y = texture2D(sampler_y,ft_Position).x;
            u = texture2D(sampler_u,ft_Position).x- 128./255.;
            v = texture2D(sampler_v,ft_Position).x- 128./255.;

            vec3 rgb;
            rgb.r = y + 1.403 * v;
            rgb.g = y - 0.344 * u - 0.714 * v;
            rgb.b = y + 1.770 * u;

            gl_FragColor = vec4(rgb,1);
        }
  """
    private var program = 0
    private var fPosition = 0
    private var vPosition = 0
    private var uMatrix = 0

    private var samplerY = 0
    private var samplerU = 0
    private var samplerV = 0

    private var vboId = 0
    private var fboId = 0

    private var width: Int = 0
    private var height: Int = 0

    private var y: Buffer? = null
    private var u: Buffer? = null
    private var v: Buffer? = null

    private var fboTextureId = 0

    private val yuvFboRender: YuvFboRender by lazy {
        YuvFboRender()
    }

    private val textureYuv by lazy {
        IntArray(3)
    }

    init {
        Matrix.setIdentityM(matrix, 0)
        vertexBuffer.position(0)
        fragmentBuffer.position(0)
    }

    override fun onSurfaceCreated() {
        program = ShaderUtil.createProgram(vertexSource, fragmentSource)

        fPosition = GLES20.glGetAttribLocation(program, "f_Position")
        vPosition = GLES20.glGetAttribLocation(program, "v_Position")
        uMatrix = GLES20.glGetUniformLocation(program, "u_Matrix")

        samplerY = GLES20.glGetUniformLocation(program, "sampler_y")
        samplerU = GLES20.glGetUniformLocation(program, "sampler_u")
        samplerV = GLES20.glGetUniformLocation(program, "sampler_v")


        GLES20.glGenTextures(3, textureYuv, 0)

        textureYuv.forEach {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, it)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        }

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

        yuvFboRender.onSurfaceCreated()
    }

    override fun onSurfaceChanged(width: Int, height: Int) {

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



        Matrix.rotateM(matrix, 0, 180f, 1f, 0f, 0f)

        GLES20.glViewport(0, 0, width, height)
        yuvFboRender.onSurfaceChanged(width, height)
    }

    override fun onDrawFrame() {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glClearColor(1f, 1f, 1f, 1f)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId)

        if (width > 0 && height > 0 && y != null && u != null && v != null) {
            GLES20.glUseProgram(program)

            GLES20.glUniformMatrix4fv(uMatrix, 1, false, matrix, 0)

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


            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureYuv[0])
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D,
                0,
                GLES20.GL_LUMINANCE,
                width,
                height,
                0,
                GLES20.GL_LUMINANCE,
                GLES20.GL_UNSIGNED_BYTE,
                y
            )
            GLES20.glUniform1i(samplerY, 0)

            GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureYuv[1])
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D,
                0,
                GLES20.GL_LUMINANCE,
                width / 2,
                height / 2,
                0,
                GLES20.GL_LUMINANCE,
                GLES20.GL_UNSIGNED_BYTE,
                u
            )
            GLES20.glUniform1i(samplerU, 1)

            GLES20.glActiveTexture(GLES20.GL_TEXTURE2)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureYuv[2])
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D,
                0,
                GLES20.GL_LUMINANCE,
                width / 2,
                height / 2,
                0,
                GLES20.GL_LUMINANCE,
                GLES20.GL_UNSIGNED_BYTE,
                v
            )
            GLES20.glUniform1i(samplerV, 2)

            y?.clear()
            u?.clear()
            v?.clear()

            y = null
            u = null
            v = null
        }

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)

        yuvFboRender.onDraw(fboTextureId)
    }

    fun setFrameData(width: Int, height: Int, y: ByteArray, u: ByteArray, v: ByteArray) {
        this.width = width
        this.height = height

        this.y = ByteBuffer.wrap(y)
        this.u = ByteBuffer.wrap(u)
        this.v = ByteBuffer.wrap(v)
    }
}