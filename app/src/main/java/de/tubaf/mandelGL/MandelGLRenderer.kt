package de.tubaf.mandelGL

import android.app.Activity
import android.content.Context
import android.opengl.GLES20.*
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


/**
 * Created by lorenz on 16.06.17 for de.tubaf.lndw
 */

class MandelGLRenderer(context: Context?) : GLSurfaceView.Renderer {
    private var context: Context = Activity()

    init {
        assert(context != null)
        this.context = context!!
    }

    private var vertexBufferObject = IntArray(1)

    private var shaderProgramHandle: Int = 0

    private var gaussianPositionUniform: Int = 0
    private var gaussianHalfFrameUniform: Int = 0
    private var iterationsUniform: Int = 0

    private var hueTextures = HashMap<HueTexture, Int>(HueTexture.values().size)

    var positionX: Double = 0.0
        set(value) {
            field = maxOf(this.minPosition, minOf(value, this.maxPosition))
        }

    var positionY: Double = 0.0
        set(value) {
            field = maxOf(this.minPosition, minOf(value, this.maxPosition))
        }

    var frameWidth: Int = 0
    var frameHeight: Int = 0

    var renderBufferWidth: Int = 0
    var renderBufferHeight: Int = 0

    //Limit position and scale:
    private val minPosition: Double = -3.0
    private val maxPosition: Double = 3.0

    private val minScale: Double = 75.0
    private val maxScale: Double = 100000000.0

    var scale = minScale
        set(value) {
            field = maxOf(this.minScale, minOf(value, this.maxScale))
        }

    var iterations = 50
        set(value) {
            field = value
        }

    private val clearColor = floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f)

    var hueTexture: HueTexture = HueTexture.firehue
        set(value) {
            field = value

            glTasks.add {
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, this.hueTextures[value]!!)
            }
        }

    val glTasks: ArrayList<() -> Unit> = ArrayList()

    companion object {
        fun checkError(dbgDomain: String, dbgText: String): Unit {
            if (!BuildConfig.DEBUG) return

            val error = GLES30.glGetError()

            if (error != GLES30.GL_NO_ERROR) {
                Log.d(dbgDomain, dbgText)
            }
        }
    }

    fun updateFrame(width: Int, height: Int) {
        this.frameWidth = width
        this.frameHeight = height
    }

    override fun onDrawFrame(p0: GL10?) {
        val dbgDomain = "Rendering Frame"

        this.glTasks.forEach {
            it.invoke()
        }

        this.glTasks.clear()

        GLES30.glUniform2f(this.gaussianPositionUniform, this.positionX.toFloat(), this.positionY.toFloat())
        MandelGLRenderer.checkError(dbgDomain, "Failed to provide uniform (gaussianPosition)")

        GLES30.glUniform2f(this.gaussianHalfFrameUniform, (0.5 * this.frameWidth.toDouble() / this.scale).toFloat(), (0.5 * this.frameHeight.toDouble() / this.scale).toFloat())
        MandelGLRenderer.checkError(dbgDomain, "Failed to provide uniform (gaussianPosition)")

        GLES30.glUniform1ui(this.iterationsUniform, this.iterations)
        MandelGLRenderer.checkError(dbgDomain, "Failed to provide uniform (iterations)")

        //Clear the renderbuffer with the given clear color.
        //Second parameter means: drawbuffers[0] which is the renderbuffer.
        GLES30.glClearBufferfv(GLES30.GL_COLOR, 0, FloatBuffer.wrap(this.clearColor))
        MandelGLRenderer.checkError(dbgDomain, "Failed to clear renderbuffer")

        //Draw a full-screen-quad:
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        MandelGLRenderer.checkError(dbgDomain, "Failed to draw")

        //Log.d("Render at", "" + this.positionX + ", " + this.positionY + " with scale: " + this.scale)
    }

    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        this.renderBufferWidth = width
        this.renderBufferHeight = height

        GLES30.glViewport(0, 0, width, height)
        MandelGLRenderer.checkError("Setting Viewport", "Failed to specify the viewport")
    }

    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        initializeGLESFeatures()
        initializeVertexData()
        initializeShaders()
        initializeTextures()

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, this.hueTextures[this.hueTexture]!!)

        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
    }

    private fun initializeGLESFeatures() {
        val dbgDomain = "Initializing GLES features"

        //Disable alpha blending:
        GLES30.glDisable(GLES30.GL_BLEND)
        MandelGLRenderer.checkError(dbgDomain, "Failed to disable alpha blending")

        //Disable the depth test:
        GLES30.glDisable(GLES30.GL_DEPTH_TEST)
        MandelGLRenderer.checkError(dbgDomain, "Failed to disable the depth test")

        GLES30.glDepthMask(false)
        MandelGLRenderer.checkError(dbgDomain, "Failed to disable the depth mask")

        //Disable the scissor test:
        GLES30.glDisable(GLES30.GL_SCISSOR_TEST)
        MandelGLRenderer.checkError(dbgDomain, "Failed to disable the scissor test")

        //Disable the stencil test:
        GLES30.glDisable(GLES30.GL_STENCIL_TEST)
        MandelGLRenderer.checkError(dbgDomain, "Failed to disable the stencil test")

        //Disable dithering:
        GLES30.glDisable(GLES30.GL_DITHER)
        MandelGLRenderer.checkError(dbgDomain, "Failed to disable dithering")
    }

    private fun initializeVertexData() {
        val dbgDomain = "Initializing vertex data"

        //Generate a VBO:
        GLES30.glGenBuffers(1, this.vertexBufferObject, 0)
        MandelGLRenderer.checkError(dbgDomain, "Failed to generate VBO")

        //Bind it:
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, this.vertexBufferObject[0])
        MandelGLRenderer.checkError(dbgDomain, "Failed to bind VBO")

        //Create simple vertex data for the corners:
        val vertexData = floatArrayOf(-1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f)

        val vertexDataBuffer = ByteBuffer.allocateDirect(vertexData.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
        vertexDataBuffer.put(vertexData)
        vertexDataBuffer.position(0)

        //Upload it:
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, 8 * 4, vertexDataBuffer, GLES30.GL_STATIC_DRAW)
        MandelGLRenderer.checkError(dbgDomain, "Failed to buffer vertex data")

        //Enable the array:
        val positionAttribute = 0

        GLES30.glEnableVertexAttribArray(positionAttribute)
        MandelGLRenderer.checkError(dbgDomain, "Failed to enable position attribute")

        //Specify the vertex data:
        val stride = 4 * 2

        GLES30.glVertexAttribPointer(positionAttribute, 2, GLES30.GL_FLOAT, false, stride, 0)
        MandelGLRenderer.checkError(dbgDomain, "Failed to specify position attribute")
    }

    private fun initializeShaders() {
        val dbgDomain = "Initializing shaders"

        //Create the vertex shader:
        val vertexShaderHandle = createShader(GLES30.GL_VERTEX_SHADER, "vertexshader.glsl")

        //Create the fragment shader:
        val fragmentShaderHandle = createShader(GLES30.GL_FRAGMENT_SHADER, "fragmentshader.glsl")

        //Create the program:
        this.shaderProgramHandle = GLES30.glCreateProgram()
        MandelGLRenderer.checkError(dbgDomain, "Failed to generate shader program handle")

        //Attach the shaders:
        GLES30.glAttachShader(this.shaderProgramHandle, vertexShaderHandle)
        MandelGLRenderer.checkError(dbgDomain, "Failed to attach vertex shader")

        GLES30.glAttachShader(this.shaderProgramHandle, fragmentShaderHandle)
        MandelGLRenderer.checkError(dbgDomain, "Failed to attach fragment shader")

        //Link the program:
        GLES30.glLinkProgram(this.shaderProgramHandle)
        MandelGLRenderer.checkError(dbgDomain, "Failed to link shader program")

        //Check if we had success:
        val linkingSuccess = IntArray(1)

        GLES30.glGetProgramiv(this.shaderProgramHandle, GLES30.GL_LINK_STATUS, linkingSuccess, 0)
        MandelGLRenderer.checkError(dbgDomain, "Failed to retrieve shader program parameter")

        if (linkingSuccess[0] != GLES30.GL_TRUE) {
            //Retrieve the error message:
            val errorMessage = GLES30.glGetProgramInfoLog(this.shaderProgramHandle)
            MandelGLRenderer.checkError(dbgDomain, "Failed to retrieve shader program info log")

            //Print it and fail:
            Log.d(dbgDomain, errorMessage)
            assert(true)
        }

        //After we have linked the program, it's a good idea to detach the shaders from it:
        GLES30.glDetachShader(this.shaderProgramHandle, vertexShaderHandle)
        MandelGLRenderer.checkError(dbgDomain, "Failed to detach vertex shader")

        GLES30.glDetachShader(this.shaderProgramHandle, fragmentShaderHandle)
        MandelGLRenderer.checkError(dbgDomain, "Failed to detach fragment shader")

        //We don't need the shaders anymore, so we can delete them right here:
        GLES30.glDeleteShader(vertexShaderHandle)
        MandelGLRenderer.checkError(dbgDomain, "Failed to devale vertex shader")

        GLES30.glDeleteShader(fragmentShaderHandle)
        MandelGLRenderer.checkError(dbgDomain, "Failed to devale fragment shader")

        //Use our program from now on:
        GLES30.glUseProgram(this.shaderProgramHandle)
        MandelGLRenderer.checkError(dbgDomain, "Failed to enable shader program")

        //Retrieve the uniforms:
        this.gaussianPositionUniform = glGetUniformLocation(this.shaderProgramHandle, "gaussianPosition")
        MandelGLRenderer.checkError(dbgDomain, "Failed to retrieve uniform (gaussianPosition)")
        assert(this.gaussianPositionUniform >= 0)

        this.gaussianHalfFrameUniform = glGetUniformLocation(this.shaderProgramHandle, "gaussianHalfFrame")
        MandelGLRenderer.checkError(dbgDomain, "Failed to retrieve uniform (gaussianHalfFrame)")
        assert(this.gaussianHalfFrameUniform >= 0)

        this.iterationsUniform = glGetUniformLocation(this.shaderProgramHandle, "iterations")
        MandelGLRenderer.checkError(dbgDomain, "Failed to retrieve uniform (iterations)")
        assert(this.iterationsUniform >= 0)

        //Set the texture uniform:
        val hueTextureUniform = glGetUniformLocation(this.shaderProgramHandle, "hueTexture")
        MandelGLRenderer.checkError(dbgDomain, "Failed to retrieve uniform (hueTexture)")
        assert(hueTextureUniform >= 0)

        //Assign the value to this uniform (const):
        glUniform1i(hueTextureUniform, 0)
        MandelGLRenderer.checkError(dbgDomain, "Failed to assign to constant uniform (hueTexture)")

        //Release the shader compiler:
        glReleaseShaderCompiler()
        MandelGLRenderer.checkError(dbgDomain, "Failed to release the shader compiler")
    }


    private fun createShader(shaderType: Int, sourceFileName: String): Int {
        val dbgDomain = "Creating shader"

        //Create a shader of our type:
        val shaderHandle = GLES30.glCreateShader(shaderType)
        MandelGLRenderer.checkError(dbgDomain, "Failed to generate shader handle")

        //Get a null-terminated raw char pointer to the source:
        val rawText = this.context.assets.open(sourceFileName)
        val shaderString = rawText.bufferedReader().use { it.readText() }

        //Pass the shader source down to GLES:
        GLES30.glShaderSource(shaderHandle, shaderString)
        MandelGLRenderer.checkError(dbgDomain, "Failed to provide shader source code")

        //Compile the shader:
        GLES30.glCompileShader(shaderHandle)
        MandelGLRenderer.checkError(dbgDomain, "Failed to compile shader")

        //Check if we had success:
        val compilationSuccess = IntArray(1)

        GLES30.glGetShaderiv(shaderHandle, GLES30.GL_COMPILE_STATUS, compilationSuccess, 0)
        MandelGLRenderer.checkError(dbgDomain, "Failed to retrieve shader parameter")

        if (compilationSuccess[0] != GLES30.GL_TRUE) {
            //Retrieve the error message:
            val errorMessageRaw = GLES30.glGetShaderInfoLog(shaderHandle)
            MandelGLRenderer.checkError(dbgDomain, "Failed to retrieve shader info log")

            //Print it and fail:
            Log.d(dbgDomain, errorMessageRaw)
            assert(true)
        }

        //Return the shader handle:
        return shaderHandle
    }

    private fun initializeTextures() {
        val dbgDomain = "Initializing textures"

        //Activate the texture unit:
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        MandelGLRenderer.checkError(dbgDomain, "Failed to activate texture unit")

        //Create the textures:
        HueTexture.values().forEach {
            this.hueTextures.put(it, createHueTexture(it))
        }
    }

    private fun createHueTexture(hue: HueTexture): Int {
        val dbgDomain = "Creating texture " + hue.toString()

        //Read the bytes:
        val rawText = this.context.assets.open(hue.name + ".rgba")
        val pixelData = rawText.buffered().readBytes()

        //Calculate the pixels count:
        val pixelsCount = pixelData.count() / 4

        //TODO: Check PoT

        //Generate a texture handle:
        val textureHandle = IntArray(1)

        GLES30.glGenTextures(1, textureHandle, 0)
        MandelGLRenderer.checkError(dbgDomain, "Failed to generate texture handle")

        //Bind our texture:
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureHandle[0])
        MandelGLRenderer.checkError(dbgDomain, "Failed to bind texture")

        //Set wrapping mode:
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        MandelGLRenderer.checkError(dbgDomain, "Failed to set wrapping for s")

        glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        MandelGLRenderer.checkError(dbgDomain, "Failed to set wrapping for t")

        //Provide the bytes:
        GLES30.glTexStorage2D(GLES30.GL_TEXTURE_2D, 1, GLES30.GL_RGBA8, pixelsCount, 1)
        MandelGLRenderer.checkError(dbgDomain, "Failed to specify texture storage (2D)")

        GLES30.glTexSubImage2D(GLES30.GL_TEXTURE_2D, 0, 0, 0, pixelsCount, 1, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, ByteBuffer.wrap(pixelData))
        MandelGLRenderer.checkError(dbgDomain, "Failed to push texture data (2D)")

        //Set min filter:
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST)
        MandelGLRenderer.checkError(dbgDomain, "Failed to set texture minification filter")

        //Set mag filter:
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST)
        MandelGLRenderer.checkError(dbgDomain, "Failed to set texture magnification filter")

        //Return the texture handle:
        return textureHandle[0]
    }
}