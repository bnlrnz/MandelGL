package de.tubaf.mandelGL

import android.content.Context
import android.opengl.GLSurfaceView
import android.support.v4.view.GestureDetectorCompat
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector

/**
* Created by lorenz on 16.06.17 for de.tubaf.lndw
*/

class MandelGLSurfaceView(context: Context?, attrs: AttributeSet) : GLSurfaceView(context, attrs), ScaleGestureDetector.OnScaleGestureListener, GestureDetector.OnGestureListener {
    private val DENSITY = getContext().resources.displayMetrics.density

    var superSamplingFactor = 2.0
        set(value) {
            if (value in 0.5..3.0) {
                field = superSamplingFactor

            }
        }

    internal val renderer = MandelGLRenderer(context)

    private var scaleDetector: ScaleGestureDetector? = null
    private var gestureDetector: GestureDetectorCompat? = null

    init {
        //GLES 3
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

        val contentWidth = (w / DENSITY).toInt()
        val contentHeight = (h / DENSITY).toInt()

        holder.setFixedSize((contentWidth * this.superSamplingFactor).toInt(), (contentHeight * this.superSamplingFactor).toInt())
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
        val unwrappedDetector: ScaleGestureDetector = detector ?: return false;

        //Get the pinch center:
        val point = floatArrayOf(unwrappedDetector.focusX / this.DENSITY, unwrappedDetector.focusY / this.DENSITY)

        //println("Pinch Center: ${point[0]}, ${point[1]}")

        //Convert to Gaussian:
        val centerX = this.renderer.positionX + (point[0] - 0.5f * this.renderer.frameWidth) / this.renderer.scale
        val centerY = this.renderer.positionY - (point[1] - 0.5f * this.renderer.frameHeight) / this.renderer.scale

        //Execute the scale:
        this.renderer.scale = this.renderer.scale * unwrappedDetector.scaleFactor.toDouble()

        //Move the saved Gaussian back to the center point:
        this.renderer.positionX = centerX - (point[0] - 0.5f * this.renderer.frameWidth) / this.renderer.scale
        this.renderer.positionY = centerY + (point[1] - 0.5f * this.renderer.frameHeight) / this.renderer.scale

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