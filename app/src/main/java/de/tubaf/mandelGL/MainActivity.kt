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
        this.iterationSlider?.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
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
        this.themeChooser?.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId){
                R.id.radioTrippy -> this.mandelSurfaceView?.renderer?.hueTexture = HueTexture.psychue
                R.id.radioAsh -> this.mandelSurfaceView?.renderer?.hueTexture = HueTexture.ashhue
                R.id.radioFire -> this.mandelSurfaceView?.renderer?.hueTexture = HueTexture.firehue
                R.id.radioIce -> this.mandelSurfaceView?.renderer?.hueTexture = HueTexture.icehue
            }
            this.mandelSurfaceView?.requestRender()
        }

        //hide/show settings, setup constraint transitions
        val hideUnhideButton = findViewById(R.id.hideUnhideButton) as Button

        this.constraintLayout = findViewById(R.id.constraintLayout) as ConstraintLayout?
        this.showSettingsConstraintSet.clone(this.constraintLayout)
        this.hideSettingsConstraintSet.clone(this.constraintLayout)
        this.hideSettingsConstraintSet.clear(R.id.settingsLayout, ConstraintSet.TOP)
        this.hideSettingsConstraintSet.connect(R.id.settingsLayout, ConstraintSet.BOTTOM, R.id.constraintLayout, ConstraintSet.TOP, 0)

        hideUnhideButton.setOnClickListener {
            TransitionManager.beginDelayedTransition(this.constraintLayout)
            if (this.settingsHidden) {
                //show settings
                this.showSettingsConstraintSet.applyTo(this.constraintLayout)
                hideUnhideButton.text = "Hide"
                this.settingsHidden = false
            } else {
                //hide settings
                this.hideSettingsConstraintSet.applyTo(this.constraintLayout)
                hideUnhideButton.text = "Settings"
                this.settingsHidden = true
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)

        //save rendering state
        outState?.putDouble("positionX", this.mandelSurfaceView?.renderer?.positionX?:0.0)
        outState?.putDouble("positionY", this.mandelSurfaceView?.renderer?.positionY?:0.0)
        outState?.putDouble("scale", this.mandelSurfaceView?.renderer?.scale?:75.0)
        outState?.putString("theme", this.mandelSurfaceView?.renderer?.hueTexture.toString())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)

        //restore rendering state
        this.mandelSurfaceView?.renderer?.positionX = savedInstanceState?.getDouble("positionX")?:0.0
        this.mandelSurfaceView?.renderer?.positionY = savedInstanceState?.getDouble("positionY")?:0.0
        this.mandelSurfaceView?.renderer?.scale = savedInstanceState?.getDouble("scale")?:75.0
        this.mandelSurfaceView?.renderer?.hueTexture = HueTexture.valueOf(savedInstanceState?.getString("theme")?:"firehue")
    }
}
