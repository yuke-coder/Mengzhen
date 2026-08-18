package com.mengzhen.app.ui.about

import android.content.Context
import android.content.res.Configuration
import android.media.AudioManager
import android.media.MediaCodecList
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mengzhen.app.R
import com.mengzhen.app.ui.theme.ThemeMode
import com.mengzhen.app.ui.theme.ThemeModeStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/** Source-adapted implementation of Bilibili 9.7.0 CpuInfoActivity. */
class AboutCpuInfoActivity : AppCompatActivity(), GLSurfaceView.Renderer {
    private var initialThemeMode = ThemeMode.SYSTEM
    private var glSurfaceView: GLSurfaceView? = null
    private var gpuRenderer: String? = null
    private var gpuVendor: String? = null
    private var decoderInfo: String = ""

    override fun attachBaseContext(newBase: Context) {
        initialThemeMode = ThemeModeStore.bootstrapMode(newBase)
        val configuration = Configuration(newBase.resources.configuration).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                Configuration.UI_MODE_NIGHT_YES
        }
        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bili_about_activity_cpu_info_source)
        runCatching {
            GLSurfaceView(this).also { surface ->
                glSurfaceView = surface
                surface.setRenderer(this)
                findViewById<FrameLayout>(R.id.bili_about_glsurface_container).addView(surface)
            }
        }
        lifecycleScope.launch {
            decoderInfo = withContext(Dispatchers.IO) { readHardwareDecoders() }
            renderInfo()
        }
        renderInfo()
    }

    override fun onResume() {
        super.onResume()
        glSurfaceView?.onResume()
    }

    override fun onPause() {
        glSurfaceView?.onPause()
        super.onPause()
    }

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        gpuRenderer = gl.glGetString(GL10.GL_RENDERER)
        gpuVendor = gl.glGetString(GL10.GL_VENDOR)
        runOnUiThread(::renderInfo)
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) = Unit

    override fun onDrawFrame(gl: GL10) = Unit

    private fun renderInfo() {
        val info = findViewById<TextView>(R.id.bili_about_cpu_info)
        info.text = buildString {
            append("====================\n= ")
            append(Build.MANUFACTURER.trim())
            append(' ')
            append(Build.MODEL)
            append("\n====================\n\n")
            append(Build.MANUFACTURER.trim())
            append(' ')
            append(Build.MODEL)
            append(" (")
            append(Build.DEVICE)
            append(")\n")
            append(Runtime.getRuntime().availableProcessors())
            append(" cores\n\n")

            append("===== ABI =====\n\n")
            Build.SUPPORTED_ABIS.forEachIndexed { index, abi ->
                append("CPU ABI")
                append(index)
                append(": ")
                append(abi)
                append('\n')
            }

            append("\n\n===== CPU =====\n\n")
            append(readCpuInfo())
            append("\n\n===== GPU =====\n\n")
            append("Render:  ")
            append(gpuRenderer ?: "N/A")
            append("\nVendor:  ")
            append(gpuVendor ?: "N/A")
            append('\n')

            append("\n===== Audio =====\n\n")
            append("LowLatency: ")
            append(
                if (packageManager.hasSystemFeature("android.hardware.audio.low_latency")) {
                    "Yes"
                } else {
                    "No"
                },
            )
            append('\n')
            val audio = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audio != null) {
                append("BufferSize: ")
                append(audio.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER) ?: "N/A")
                append(" frames (lower is better)\nSampleRate: ")
                append(audio.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE) ?: "N/A")
                append(" Hz\n")
            }
            if (decoderInfo.isNotBlank()) {
                append("\n===== HW Decoders =====\n\n")
                append(decoderInfo)
                append('\n')
            }
        }
    }

    private fun readCpuInfo(): String = runCatching {
        File("/proc/cpuinfo").readText()
    }.getOrDefault("N/A")

    private fun readHardwareDecoders(): String = runCatching {
        MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos
            .asSequence()
            .filterNot { it.isEncoder }
            .filter { codec ->
                !codec.name.startsWith("OMX.google.") &&
                    !codec.name.startsWith("c2.android.")
            }
            .joinToString("\n") { codec ->
                "${codec.name}: ${codec.supportedTypes.joinToString()}"
            }
    }.getOrDefault("")
}
