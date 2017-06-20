package de.tubaf.mandelGL

import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.support.v7.app.AppCompatActivity
import android.transition.TransitionManager
import android.widget.Button
import android.widget.RadioGroup
import android.widget.SeekBar

class MainActivity : AppCompatActivity() {

    private var constraintLayout: ConstraintLayout? = null
    private val hideSettingsConstraintSet: ConstraintSet = ConstraintSet()
    private val showSettingsConstraintSet: ConstraintSet = ConstraintSet() // default
    private var settingsHidden = false
    private var hideUnhideButton: Button? = null

    private var iterationSlider: SeekBar? = null
    private var renderScaleSlider: SeekBar? = null

    private var themeChooser: RadioGroup? = null

    private var mandelSurfaceView: MandelGLSurfaceView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //init mandel gl view
        this.mandelSurfaceView = findViewById(R.id.mandelGLSurfaceView) as MandelGLSurfaceView?

        //setup sliders
        this.iterationSlider = findViewById(R.id.iterationSlider) as SeekBar?

        this.iterationSlider?.max = 200
        this.iterationSlider?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                mandelSurfaceView?.renderer?.iterations = progress
                mandelSurfaceView?.requestRender()
            }
        })

        this.renderScaleSlider = findViewById(R.id.renderScaleSlider) as SeekBar?
        this.renderScaleSlider?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                var newVal = p1
                if (newVal < 50) {
                    newVal = 50
                    renderScaleSlider?.progress = 50
                }
                mandelSurfaceView?.superSamplingFactor = newVal.toDouble() / 100.0
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }
        })

        //setup theme chooser
        this.themeChooser = findViewById(R.id.radioGroup) as RadioGroup?
        this.themeChooser?.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioTrippy -> this.mandelSurfaceView?.renderer?.hueTexture = HueTexture.psychue
                R.id.radioAsh -> this.mandelSurfaceView?.renderer?.hueTexture = HueTexture.ashhue
                R.id.radioFire -> this.mandelSurfaceView?.renderer?.hueTexture = HueTexture.firehue
                R.id.radioIce -> this.mandelSurfaceView?.renderer?.hueTexture = HueTexture.icehue
            }
            this.mandelSurfaceView?.requestRender()
        }

        //hide/show settings, setup constraint transitions
        this.hideUnhideButton = findViewById(R.id.hideUnhideButton) as Button

        this.constraintLayout = findViewById(R.id.constraintLayout) as ConstraintLayout?
        this.showSettingsConstraintSet.clone(this.constraintLayout)
        this.hideSettingsConstraintSet.clone(this.constraintLayout)
        this.hideSettingsConstraintSet.clear(R.id.settingsLayout, ConstraintSet.BOTTOM)
        this.hideSettingsConstraintSet.connect(R.id.settingsLayout, ConstraintSet.TOP, R.id.constraintLayout, ConstraintSet.BOTTOM, 0)

        this.hideUnhideButton?.setOnClickListener {
            TransitionManager.beginDelayedTransition(this.constraintLayout)
            if (settingsHidden) {
                //show settings
                showSettingsConstraintSet.applyTo(this.constraintLayout)
                hideUnhideButton?.text = getString(R.string.hideButtonText)
                this.settingsHidden = false
            } else {
                //hide settings
                hideSettingsConstraintSet.applyTo(this.constraintLayout)
                hideUnhideButton?.text = getString(R.string.settingsButtonText)
                this.settingsHidden = true
            }
        }
    }

    override fun onBackPressed() {
        if (!settingsHidden) {
            this.hideUnhideButton?.performClick()
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()

        TransitionManager.beginDelayedTransition(this.constraintLayout)
        if (settingsHidden) {
            this.hideUnhideButton?.text = getString(R.string.settingsButtonText)
            this.hideSettingsConstraintSet.applyTo(this.constraintLayout)
        } else {
            this.hideUnhideButton?.text = getString(R.string.hideButtonText)
            this.showSettingsConstraintSet.applyTo(this.constraintLayout)
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)

        //save rendering state
        outState?.putDouble("positionX", this.mandelSurfaceView?.renderer?.positionX ?: 0.0)
        outState?.putDouble("positionY", this.mandelSurfaceView?.renderer?.positionY ?: 0.0)
        outState?.putDouble("scale", this.mandelSurfaceView?.renderer?.scale ?: 75.0)
        outState?.putString("theme", this.mandelSurfaceView?.renderer?.hueTexture.toString())
        outState?.putDouble("superSamplingFactor", this.mandelSurfaceView?.superSamplingFactor ?: 2.0)
        outState?.putBoolean("settingsHidden", this.settingsHidden)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)

        //restore rendering state
        this.mandelSurfaceView?.renderer?.positionX = savedInstanceState?.getDouble("positionX") ?: 0.0
        this.mandelSurfaceView?.renderer?.positionY = savedInstanceState?.getDouble("positionY") ?: 0.0
        this.mandelSurfaceView?.renderer?.scale = savedInstanceState?.getDouble("scale") ?: 75.0
        this.mandelSurfaceView?.renderer?.hueTexture = HueTexture.valueOf(savedInstanceState?.getString("theme") ?: "firehue")
        this.mandelSurfaceView?.superSamplingFactor = savedInstanceState?.getDouble("superSamplingFactor") ?: 2.0
        this.settingsHidden = savedInstanceState?.getBoolean("settingsHidden") ?: false
    }
}
