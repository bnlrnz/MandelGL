package de.tubaf.mandelGL

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.text.Layout
import android.text.method.LinkMovementMethod
import android.transition.TransitionManager
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.WindowManager
import android.widget.*
import java.io.File
import java.io.File.separator
import java.io.IOException


class MainActivity : AppCompatActivity() {

    private var constraintLayout: ConstraintLayout? = null
    private val hideSettingsConstraintSet: ConstraintSet = ConstraintSet()
    private val showSettingsConstraintSet: ConstraintSet = ConstraintSet() // default
    private var settingsHidden = true
    private var hideUnhideButton: Button? = null

    private var infoDialog: Dialog? = null
    private var aboutDialog: Dialog? = null

    private var infoDialogPresent = false
    private var aboutDialogPresent = false

    private var iterationSlider: SeekBar? = null
    private var renderScaleSlider: SeekBar? = null

    private var themeChooser: RadioGroup? = null

    private var progressView: LinearLayout? = null

    private var mandelSurfaceView: MandelGLSurfaceView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //init mandel gl view
        this.mandelSurfaceView = findViewById(R.id.mandelGLSurfaceView)

        //setup sliders
        this.iterationSlider = findViewById(R.id.iterationSlider)

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

        this.renderScaleSlider = findViewById(R.id.renderScaleSlider)
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
        this.themeChooser = findViewById(R.id.radioGroup)
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
        this.hideUnhideButton = findViewById(R.id.hideUnhideButton)

        this.constraintLayout = findViewById(R.id.constraintLayout)
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
        findViewById<Button>(R.id.aboutButton).setOnClickListener {
            this.aboutDialogPresent = true
            showAboutDialog()
        }

        findViewById<Button>(R.id.infoButton).setOnClickListener {
            this.infoDialogPresent = true
            showInfoDialog()
        }

        //share feature
        findViewById<Button>(R.id.shareButton).setOnClickListener {
            getCurrentImage()
        }

        //make sure rendering progress view is gone
        this.progressView = findViewById(R.id.progressLayout)
    }

    fun getCurrentImage() {

        //show progress spinner
        this.progressView?.visibility = VISIBLE
        this.progressView?.bringToFront()

        this.mandelSurfaceView?.renderer?.glTasks?.add {
            val file = File(cacheDir?.absolutePath + separator + "temporary_file.png")

            if (file.exists()) {
                file.delete()
            }

            file.createNewFile()

            try {
                this.mandelSurfaceView?.saveFrame(file, {
                    runOnUiThread {

                        //hide progress spinner
                        this.progressView?.visibility = GONE

                        val contentUri = FileProvider.getUriForFile(this, "de.tubaf.mandelGL.fileprovider", file)

                        val shareIntent = Intent()
                        shareIntent.action = Intent.ACTION_SEND
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // temp permission for receiving app to read this file
                        shareIntent.setDataAndType(contentUri, contentResolver.getType(contentUri))
                        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
                        shareIntent.type = "image/png"
                        startActivity(Intent.createChooser(shareIntent, resources.getText(R.string.send_to)))
                    }
                })
            } catch (e: IOException) {
                e.printStackTrace()

                //hide progress spinner
                this.progressView?.visibility = GONE
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
        if (this.infoDialog != null) {
            if (this.infoDialog?.isShowing as Boolean) {
                this.infoDialog?.dismiss()
            }
        }

        this.infoDialog = Dialog(this)
        this.infoDialog?.setContentView(R.layout.info)
        val shaderText = this.infoDialog?.findViewById<TextView?>(R.id.shaderText)

        //get shader source
        val rawText = this.assets.open("fragmentshader.glsl")
        val shaderString = rawText.bufferedReader().use { it.readText() }
        shaderText?.text = shaderString

        //link clicked
        val ccLink = this.infoDialog?.findViewById<TextView?>(R.id.cclink)
        ccLink?.isClickable = true
        ccLink?.movementMethod = LinkMovementMethod.getInstance()

        val sourceLink = this.infoDialog?.findViewById<TextView?>(R.id.sourceLink)
        sourceLink?.isClickable = true
        sourceLink?.movementMethod = LinkMovementMethod.getInstance()

        this.infoDialog?.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        this.infoDialog?.show()
        this.infoDialog?.setOnDismissListener {
            this.infoDialogPresent = false
        }
    }

    private fun showAboutDialog() {
        if (this.aboutDialog != null) {
            if (this.aboutDialog?.isShowing as Boolean) {
                this.aboutDialog?.dismiss()
            }
        }

        this.aboutDialog = Dialog(this)
        aboutDialog?.setContentView(R.layout.about)
        aboutDialog?.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        aboutDialog?.show()
        aboutDialog?.setOnDismissListener {
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

        // this is needed to show these views on top of the glsurfaceview on devices with hardware buttons (back home etc)
        // but why? :O
        this.hideUnhideButton?.bringToFront()
        findViewById<ConstraintLayout>(R.id.settingsLayout).bringToFront()
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
