package de.tubaf.mandelGL

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.RadioGroup
import android.widget.SeekBar

class MainActivity : AppCompatActivity() {

    private var iterationSlider: SeekBar? = null
    private var themeChooser: RadioGroup? = null

    private var mandelSurfaceView: MandelGLSurfaceView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        this.mandelSurfaceView = findViewById(R.id.mandelGLSurfaceView) as MandelGLSurfaceView?

        //setup sliders
        this.iterationSlider = findViewById(R.id.seekBar) as SeekBar?

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
