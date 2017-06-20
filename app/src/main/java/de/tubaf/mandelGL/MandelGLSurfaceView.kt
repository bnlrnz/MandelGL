package de.tubaf.mandelGL

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.support.v4.view.GestureDetectorCompat
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder


/**
 * Created by lorenz on 16.06.17 for de.tubaf.lndw
 */

class MandelGLSurfaceView(context: Context?, attrs: AttributeSet) : GLSurfaceView(context, attrs), ScaleGestureDetector.OnScaleGestureListener, GestureDetector.OnGestureListener {
    private val DENSITY = getContext().resources.displayMetrics.density

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

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        this.renderer.updateFrame(w, h)
        holder.setFixedSize((w / DENSITY * this.superSamplingFactor).toInt(), (h / DENSITY * this.superSamplingFactor).toInt())
    }

    fun saveFrame(file: File) {

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

        val width = width
        val height = height
        val buf = ByteBuffer.allocateDirect(width * height * 4)
        buf.order(ByteOrder.LITTLE_ENDIAN)
        GLES20.glReadPixels(0, 0, width, height,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf)
        buf.rewind()

        var bos: BufferedOutputStream? = null
        try {
            bos = BufferedOutputStream(FileOutputStream(filename))
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bmp.copyPixelsFromBuffer(buf)
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, bos)
            bmp.recycle()
        } finally {
            if (bos != null) bos.close()
        }
    }

    //Handle touch events
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
    override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
        return true
    }

    override fun onScaleEnd(detector: ScaleGestureDetector?) {
    }

    override fun onScale(detector: ScaleGestureDetector?): Boolean {
        val unwrappedDetector: ScaleGestureDetector = detector ?: return false

        //Get the pinch center:
        val point = floatArrayOf(unwrappedDetector.focusX / this.DENSITY, unwrappedDetector.focusY / this.DENSITY)

        //println("Pinch Center: ${point[0]}, ${point[1]}")

        //Convert to Gaussian:
        val centerX = this.renderer.positionX + (point[0] - 0.5f * width / DENSITY) / this.renderer.scale
        val centerY = this.renderer.positionY - (point[1] - 0.5f * height / DENSITY) / this.renderer.scale

        //Execute the scale:
        this.renderer.scale = this.renderer.scale * unwrappedDetector.scaleFactor.toDouble()

        //Move the saved Gaussian back to the center point:
        this.renderer.positionX = centerX - (point[0] - 0.5f * width / DENSITY) / this.renderer.scale
        this.renderer.positionY = centerY + (point[1] - 0.5f * height / DENSITY) / this.renderer.scale

        requestRender()

        return true
    }

    //Not needed
    override fun onShowPress(e: MotionEvent?) {
    }

    override fun onSingleTapUp(e: MotionEvent?): Boolean {
        return false
    }

    override fun onDown(e: MotionEvent?): Boolean {
        return false
    }

    override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
        return false
    }

    override fun onLongPress(e: MotionEvent?) {
    }
}