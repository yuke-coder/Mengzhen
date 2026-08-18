package com.mengzhen.app.ui.about

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.mengzhen.app.R
import com.mengzhen.app.ui.theme.ThemeMode
import com.mengzhen.app.ui.theme.ThemeModeStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.util.concurrent.TimeUnit

/** Source-adapted from Bilibili 9.7.0 PingTestActivity. */
class AboutNetworkDiagnosticsActivity : AppCompatActivity() {
    private var initialThemeMode = ThemeMode.SYSTEM
    private lateinit var localIp: TextView
    private lateinit var log: TextView
    private lateinit var test: Button
    private lateinit var copy: Button
    private lateinit var progress: ProgressBar

    override fun attachBaseContext(newBase: Context) {
        initialThemeMode = ThemeModeStore.bootstrapMode(newBase)
        if (initialThemeMode == ThemeMode.SYSTEM) {
            super.attachBaseContext(newBase)
            return
        }
        val configuration = Configuration(newBase.resources.configuration).apply {
            val nightMode = if (initialThemeMode == ThemeMode.DARK) {
                Configuration.UI_MODE_NIGHT_YES
            } else {
                Configuration.UI_MODE_NIGHT_NO
            }
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or nightMode
        }
        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bili_about_activity_network_diagnostics_source)
        bindToolbar()

        localIp = findViewById(R.id.bili_about_network_local_ip)
        log = findViewById(R.id.bili_about_network_log)
        test = findViewById(R.id.bili_about_network_test)
        copy = findViewById(R.id.bili_about_network_copy)
        progress = findViewById(R.id.bili_about_network_progress)

        lifecycleScope.launch {
            val address = withContext(Dispatchers.IO) { findLocalAddress() }
            localIp.text = address ?: getString(R.string.bili_about_network_not_connected)
            test.isEnabled = address != null
        }
        test.setOnClickListener { runDiagnostics() }
        copy.setOnClickListener {
            val result = "${localIp.text}\n${log.text}"
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("network diagnostics", result))
            Toast.makeText(this, R.string.bili_about_network_copied, Toast.LENGTH_SHORT).show()
        }
    }

    private fun bindToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.bili_about_network_toolbar)
        toolbar.title = getString(R.string.bili_about_network_diagnostics)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.navigationIcon?.setTint(
            ContextCompat.getColor(this, R.color.theme_color_primary_tr_icon),
        )
        toolbar.setNavigationOnClickListener { finish() }
        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        window.statusBarColor = ContextCompat.getColor(
            this,
            R.color.theme_color_primary_tr_background,
        )
        window.navigationBarColor = ContextCompat.getColor(this, R.color.Ga1)
        window.isNavigationBarContrastEnforced = false
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
    }

    private fun runDiagnostics() {
        test.isEnabled = false
        copy.isEnabled = false
        progress.visibility = View.VISIBLE
        log.text = ""
        lifecycleScope.launch {
            val results = withContext(Dispatchers.IO) {
                DIAGNOSTIC_HOSTS.map(::diagnoseHost)
            }
            log.text = results.joinToString("\n")
            progress.visibility = View.INVISIBLE
            test.isEnabled = true
            copy.isEnabled = true
        }
    }

    private fun diagnoseHost(host: String): String {
        val lookupStarted = SystemClock.elapsedRealtime()
        val address = runCatching { InetAddress.getByName(host) }.getOrNull()
            ?: return "{$host: ${getString(R.string.bili_about_network_lookup_failed)}}"
        val lookupTime = SystemClock.elapsedRealtime() - lookupStarted

        val connectStarted = SystemClock.elapsedRealtime()
        val connectTime = runCatching {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(address, 443), SOCKET_TIMEOUT_MS)
            }
            SystemClock.elapsedRealtime() - connectStarted
        }.getOrNull()

        val requestStarted = SystemClock.elapsedRealtime()
        val httpsTime = runCatching {
            HTTP_CLIENT.newCall(Request.Builder().url("https://$host/").head().build())
                .execute()
                .use { }
            SystemClock.elapsedRealtime() - requestStarted
        }.getOrNull()

        return buildString {
            append('{')
            append(host)
            append('/')
            append(address.hostAddress)
            append(": lookupTime=")
            append(lookupTime)
            append(", connectTime=")
            append(connectTime?.let { "${it}ms" } ?: getString(R.string.bili_about_network_timeout))
            append(", https time=")
            append(httpsTime?.let { "${it}ms" } ?: getString(R.string.bili_about_network_timeout))
            append('}')
        }
    }

    private fun findLocalAddress(): String? = runCatching {
        NetworkInterface.getNetworkInterfaces().toList()
            .asSequence()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { it.inetAddresses.toList().asSequence() }
            .firstOrNull { !it.isLoopbackAddress && !it.isLinkLocalAddress }
            ?.hostAddress
    }.getOrNull()

    companion object {
        private const val SOCKET_TIMEOUT_MS = 6_000
        private val DIAGNOSTIC_HOSTS = listOf(
            "driftcue.com",
            "br-epic-clam-5a2fd709.supabase2.aidap-global.cn-beijing.volces.com",
            "www.baidu.com",
        )
        private val HTTP_CLIENT = OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .build()
    }
}
