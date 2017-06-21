package de.tubaf.mandelGL

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.transition.TransitionManager
import android.view.WindowManager
import android.widget.Button
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import java.io.File
import java.io.File.separator
import java.io.IOException


class MainActivity : AppCompatActivity() {

    private var constraintLayout: ConstraintLayout? = null
    private val hideSettingsConstraintSet: ConstraintSet = ConstraintSet()
    private val showSettingsConstraintSet: ConstraintSet = ConstraintSet() // default
    private var settingsHidden = true
    private var hideUnhideButton: Button? = null

    private var infoDialogPresent = false
    private var aboutDialogPresent = false

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
                //limit the min value of the renderscale slider
                if (newVal < 50) {
                    newVal = 50
                    renderScaleSlider?.progress = 50
                }
                //pass the new value to the supersampling property
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

        //button interaction
        findViewById(R.id.aboutButton).setOnClickListener {
            this.aboutDialogPresent = true
            showAboutDialog()
        }

        findViewById(R.id.infoButton).setOnClickListener {
            this.infoDialogPresent = true
            showInfoDialog()
        }

        //share feature
        findViewById(R.id.shareButton).setOnClickListener {
            getCurrentImage()
        }
    }

    fun getCurrentImage() {
        this.mandelSurfaceView?.renderer?.glTasks?.add {
            val file = File(cacheDir?.absolutePath + separator + "temporary_file.png")
            try {
                file.createNewFile()
                this.mandelSurfaceView?.saveFrame(file)
            } catch (e: IOException) {
                e.printStackTrace()
            }

            val contentUri = FileProvider.getUriForFile(this, "de.tubaf.mandelGL.fileprovider", file)

            runOnUiThread {
                val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // temp permission for receiving app to read this file
                shareIntent.setDataAndType(contentUri, getContentResolver().getType(contentUri));
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                shareIntent.type = "image/jpeg"
                startActivity(Intent.createChooser(shareIntent, resources.getText(R.string.send_to)))
            }
        }

        //we need to trigger render to execute gl call
        this.mandelSurfaceView?.requestRender()
    }

    override fun onBackPressed() {
        //if settings are visible, back press will hide them
        //if not, app gets closed (default behaviour)
        if (!settingsHidden) {
            this.hideUnhideButton?.performClick()
        } else {
            super.onBackPressed()
        }
    }

    private fun showInfoDialog() {
        val dialog: Dialog = Dialog(this)
        dialog.setContentView(R.layout.info)
        val shaderText = dialog.findViewById(R.id.shaderText) as TextView?

        //get shader source
        val rawText = this.assets.open("fragmentshader.glsl")
        val shaderString = rawText.bufferedReader().use { it.readText() }
        shaderText?.text = shaderString

        dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        dialog.show()
        dialog.setOnDismissListener {
            this.infoDialogPresent = false
        }
    }

    private fun showAboutDialog() {
        val dialog: Dialog = Dialog(this)
        dialog.setContentView(R.layout.about)
        dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        dialog.show()
        dialog.setOnDismissListener {
            this.aboutDialogPresent = false
        }
    }

    override fun onResume() {
        super.onResume()

        //resume with or withour visible settings?
        TransitionManager.beginDelayedTransition(this.constraintLayout)
        if (settingsHidden) {
            this.hideUnhideButton?.text = getString(R.string.settingsButtonText)
            this.hideSettingsConstraintSet.applyTo(this.constraintLayout)
        } else {
            this.hideUnhideButton?.text = getString(R.string.hideButtonText)
            this.showSettingsConstraintSet.applyTo(this.constraintLayout)
        }

        //resume with present dialog?
        if (this.infoDialogPresent) {
            showInfoDialog()
        }

        if (this.aboutDialogPresent) {
            showAboutDialog()
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

        //save gui state
        outState?.putBoolean("settingsHidden", this.settingsHidden)
        outState?.putBoolean("infoDialogPresent", this.infoDialogPresent)
        outState?.putBoolean("aboutDialogPresent", this.aboutDialogPresent)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)

        //restore rendering state
        this.mandelSurfaceView?.renderer?.positionX = savedInstanceState?.getDouble("positionX") ?: 0.0
        this.mandelSurfaceView?.renderer?.positionY = savedInstanceState?.getDouble("positionY") ?: 0.0
        this.mandelSurfaceView?.renderer?.scale = savedInstanceState?.getDouble("scale") ?: 75.0
        this.mandelSurfaceView?.renderer?.hueTexture = HueTexture.valueOf(savedInstanceState?.getString("theme") ?: "firehue")
        this.mandelSurfaceView?.superSamplingFactor = savedInstanceState?.getDouble("superSamplingFactor") ?: 2.0

        //restore gui state
        this.settingsHidden = savedInstanceState?.getBoolean("settingsHidden") ?: false
        this.infoDialogPresent = savedInstanceState?.getBoolean("infoDialogPresent") ?: false
        this.aboutDialogPresent = savedInstanceState?.getBoolean("aboutDialogPresent") ?: false
    }
}
