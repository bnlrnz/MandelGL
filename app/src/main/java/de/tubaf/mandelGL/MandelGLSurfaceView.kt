package de.tubaf.mandelGL

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES30
import android.opengl.GLException
import android.opengl.GLSurfaceView
import android.support.v4.view.GestureDetectorCompat
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import java.nio.IntBuffer
import javax.microedition.khronos.opengles.GL10

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

    internal fun createBitmapFromGLSurface(x: Int, y: Int, w: Int, h: Int): Bitmap? {
        val bitmapBuffer = IntArray(w * h)
        val bitmapSource = IntArray(w * h)
        val intBuffer = IntBuffer.wrap(bitmapBuffer)
        intBuffer.position(0)

        try {
            GLES30.glReadPixels(x, y, w, h, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, intBuffer)
            var offset1: Int
            var offset2: Int
            for (i in 0..h - 1) {
                offset1 = i * w
                offset2 = (h - i - 1) * w
                for (j in 0..w - 1) {
                    val texturePixel = bitmapBuffer[offset1 + j]
                    val blue = texturePixel shr 16 and 0xff
                    val red = texturePixel shl 16 and 0x00ff0000
                    val pixel = texturePixel and 0xff00ff00.toInt() or red or blue
                    bitmapSource[offset2 + j] = pixel
                }
            }
        } catch (e: GLException) {
            return null
        }

        return Bitmap.createBitmap(bitmapSource, w, h, Bitmap.Config.ARGB_8888)
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