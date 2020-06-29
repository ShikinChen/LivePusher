package me.shiki.livepusher.egl

import android.opengl.GLES20
import java.security.CodeSource
import javax.microedition.khronos.opengles.GL10

/**
 * Shader工具类
 */
class ShaderUtil {
    companion object {
        /**
         * 生成Shader
         */
        @JvmStatic
        fun loadShader(shaderType: Int, source: String): Int {
            var shader = GLES20.glCreateShader(shaderType)
            if (shader != 0) {
                GLES20.glShaderSource(shader, source)
                GLES20.glCompileShader(shader)

                val compile = IntArray(1)
                GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compile, 0)
                if (compile[0] != GLES20.GL_TRUE) {
                    GLES20.glDeleteShader(shader)
                    shader = 0
                }
            }
            return shader
        }

        /**
         * 创建Program
         */
        @JvmStatic
        fun createProgram(vertexSource: String, fragmentSource: String): Int {
            var program = 0
            val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource)
            val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)

            if (vertexShader != 0 && fragmentShader != 0) {
                program = GLES20.glCreateProgram()

                GLES20.glAttachShader(program, vertexShader)
                GLES20.glAttachShader(program, fragmentShader)

                GLES20.glLinkProgram(program)
            }

            return program
        }
    }
}