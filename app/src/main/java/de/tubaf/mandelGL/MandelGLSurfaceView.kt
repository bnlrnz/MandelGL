package de.tubaf.mandelGL

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.core.view.GestureDetectorCompat
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MandelGLSurfaceView(context: Context?, attrs: AttributeSet) : GLSurfaceView(context, attrs), ScaleGestureDetector.OnScaleGestureListener, GestureDetector.OnGestureListener {
    private val density = getContext().resources.displayMetrics.density

    var superSamplingFactor = 2.0
        set(value) {
            if (value in 0.5..3.0) {
                field = value
                onSizeChanged(width, height, width, height)
                requestRender()
            }
        }

    internal val renderer = MandelGLRenderer(context)

    private var scaleDetector: ScaleGestureDetector? = null
    private var gestureDetector: GestureDetectorCompat? = null

    init {
        //GLES 3 is supported by Android 4.3+
        setEGLContextClientVersion(3)

        setRenderer(this.renderer)

        //on demand rendering
        renderMode = RENDERMODE_WHEN_DIRTY

        //Create gesture recognizers for panning and pinching:
        this.gestureDetector = GestureDetectorCompat(getContext(), this)
        this.scaleDetector = ScaleGestureDetector(getContext(), this)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        this.renderer.updateFrame(w, h)
        holder.setFixedSize((w / density * this.superSamplingFactor).toInt(), (h / density * this.superSamplingFactor).toInt())
    }

    fun saveFrame(file: File, finishedSavingFrame: () -> Unit) {
        // glReadPixels fills in a "direct" ByteBuffer with what is essentially big-endian RGBA
        // data (i.e. a byte of red, followed by a byte of green...).  While the Bitmap
        // constructor that takes an int[] wants little-endian ARGB (blue/red swapped), the
        // Bitmap "copy pixels" method wants the same format GL provides.
        //
        // Ideally we'd have some way to re-use the ByteBuffer, especially if we're calling
        // here often.
        //
        // Making this even more interesting is the upside-down nature of GL, which means
        // our output will look upside down relative to what appears on screen if the
        // typical GL conventions are used.

        val filename = file.toString()

        val renderWidth = this.renderer.renderBufferWidth
        val renderHeight = this.renderer.renderBufferHeight
        val buf = ByteBuffer.allocateDirect(renderWidth * renderHeight * 4)
        buf.order(ByteOrder.LITTLE_ENDIAN)

        GLES30.glFinish()
        GLES30.glReadPixels(0, 0, renderWidth, renderHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf)
        MandelGLRenderer.checkError("glReadPixels", "Failed to read pixels")

        buf.rewind()

        var bos: BufferedOutputStream? = null
        try {
            bos = BufferedOutputStream(FileOutputStream(filename))
            var bmp = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888)
            bmp.copyPixelsFromBuffer(buf)

            //flip image
            val m = Matrix()
            m.preScale(1.0f, -1.0f)
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, false)

            bmp.compress(Bitmap.CompressFormat.PNG, 100, bos)
            bmp.recycle()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            if (bos != null) {
                bos.close()
                finishedSavingFrame.invoke()
            }
        }
    }

    //Handle touch events
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        var retVal = scaleDetector?.onTouchEvent(event)
        retVal = gestureDetector?.onTouchEvent(event) as Boolean || retVal as Boolean
        return retVal || super.onTouchEvent(event)
    }

    //Panning
    override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
        this.renderer.positionX += distanceX / this.renderer.scale
        this.renderer.positionY -= distanceY / this.renderer.scale

        requestRender()
        return true
    }

    //Pinch Zooming
    override fun onScaleBegin(detector: ScaleGestureDetector?) = true

    override fun onScaleEnd(detector: ScaleGestureDetector?) {}

    override fun onScale(detector: ScaleGestureDetector?): Boolean {
        val unwrappedDetector: ScaleGestureDetector = detector ?: return false

        //Get the pinch center:
        val point = floatArrayOf(unwrappedDetector.focusX / this.density, unwrappedDetector.focusY / this.density)

        //println("Pinch Center: ${point[0]}, ${point[1]}")

        //Convert to Gaussian:
        val centerX = this.renderer.positionX + (point[0] - 0.5f * width / density) / this.renderer.scale
        val centerY = this.renderer.positionY - (point[1] - 0.5f * height / density) / this.renderer.scale

        //Execute the scale:
        this.renderer.scale = this.renderer.scale * unwrappedDetector.scaleFactor.toDouble()

        //Move the saved Gaussian back to the center point:
        this.renderer.positionX = centerX - (point[0] - 0.5f * width / density) / this.renderer.scale
        this.renderer.positionY = centerY + (point[1] - 0.5f * height / density) / this.renderer.scale

        requestRender()

        return true
    }

    //Not needed
    override fun onShowPress(e: MotionEvent?) {}

    override fun onSingleTapUp(e: MotionEvent?) = false

    override fun onDown(e: MotionEvent?) = false

    override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float) = false

    override fun onLongPress(e: MotionEvent?) {}
}

