package com.mengzhen.app.ui.about

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Paint
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.text.format.Formatter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.mengzhen.app.BuildConfig
import com.mengzhen.app.R
import com.mengzhen.app.data.store.AppSettingsStore
import com.mengzhen.app.ui.theme.ThemeMode
import com.mengzhen.app.ui.theme.ThemeModeStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/** Native host migrated from Bilibili 9.7.0 BiliPreferencesActivity. */
class AboutMengZhenActivity : AppCompatActivity() {
    private var initialThemeMode = ThemeMode.SYSTEM

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
        setContentView(R.layout.bili_about_activity_preference_source)

        val toolbar = findViewById<MaterialToolbar>(R.id.bili_about_nav_top_bar)
        toolbar.title = getString(R.string.bili_about_title)
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

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.bili_about_content_layout, AboutMengZhenFragment())
                .commitNow()
        }
    }

    companion object {
        fun createIntent(context: Context): Intent =
            Intent(context, AboutMengZhenActivity::class.java)
    }
}

/** Logic migrated from Bilibili 9.7.0 HelpFragment with Mengzhen service targets. */
class AboutMengZhenFragment : PreferenceFragmentCompat() {
    private var checkingUpdate = false

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.bili_about_help_preferences_source, rootKey)

        findPreference<Preference>(getString(R.string.bili_about_pref_key_check_update))?.apply {
            summary = getString(
                R.string.bili_about_current_version,
                BuildConfig.VERSION_NAME,
            ) + "    (release-b${BuildConfig.VERSION_CODE})"
            setOnPreferenceClickListener {
                checkUpdate()
                true
            }
        }
        findPreference<Preference>(getString(R.string.bili_about_pref_key_cpu_info))
            ?.setOnPreferenceClickListener {
                startActivity(Intent(requireContext(), AboutCpuInfoActivity::class.java))
                true
            }
        findPreference<Preference>(getString(R.string.bili_about_pref_key_diagnostics))
            ?.setOnPreferenceClickListener {
                startActivity(Intent(requireContext(), AboutNetworkDiagnosticsActivity::class.java))
                true
            }
        findPreference<Preference>(getString(R.string.bili_about_pref_key_reset))
            ?.setOnPreferenceClickListener {
                showResetPrompt()
                true
            }
        findPreference<Preference>(getString(R.string.bili_about_pref_key_license))
            ?.setOnPreferenceClickListener {
                startActivity(Intent(requireContext(), AboutOpenSourceLicensesActivity::class.java))
                true
            }
        findPreference<Preference>(getString(R.string.bili_about_pref_key_join_us))
            ?.setOnPreferenceClickListener {
                openWeb(Uri.parse(JOIN_US_URL))
                true
            }

        // Bilibili hides this row when remote filing text or URL is absent.
        findPreference<Preference>(getString(R.string.bili_about_pref_key_record))?.isVisible = false
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val topPadding = resources.getDimensionPixelSize(
            R.dimen.bili_about_preference_top_padding,
        )
        listView.apply {
            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.Ga1))
            setPadding(0, topPadding, 0, topPadding)
            clipToPadding = false
            addItemDecoration(BiliAboutDividerDecoration(requireContext()))
        }
    }

    private fun checkUpdate() {
        if (checkingUpdate) return
        if (!requireContext().hasInternetNetwork()) {
            showSourceToast(R.string.bili_about_update_no_network)
            return
        }
        checkingUpdate = true
        viewLifecycleOwner.lifecycleScope.launch {
            val update = withContext(Dispatchers.IO) {
                runCatching { UpdateManifest.fetch(requireContext()) }.getOrNull()
            }
            checkingUpdate = false
            if (update == null) {
                showSourceToast(R.string.bili_about_update_check_failed)
            } else if (update.versionCode <= BuildConfig.VERSION_CODE) {
                showSourceToast(R.string.bili_about_update_no_new)
            } else {
                showUpdatePrompt(update)
            }
        }
    }

    private fun showUpdatePrompt(update: UpdateManifest) {
        val message = buildString {
            append(getString(R.string.bili_about_update_version))
            append(update.versionName)
            if (update.releaseNotes.isNotBlank()) {
                append("\n\n")
                append(getString(R.string.bili_about_update_content_title))
                append('\n')
                append(update.releaseNotes)
            }
            if (update.packageSize > 0L) {
                append("\n\n")
                append(getString(R.string.bili_about_update_package_size))
                append(Formatter.formatShortFileSize(requireContext(), update.packageSize))
            }
        }
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.bili_about_update_available)
            .setMessage(message)
            .setNegativeButton(R.string.bili_about_update_not_now, null)
            .setPositiveButton(R.string.bili_about_update_confirm) { _, _ ->
                openWeb(update.downloadUri)
            }
            .show()
    }

    private fun showResetPrompt() {
        AlertDialog.Builder(requireContext())
            .setMessage(R.string.bili_about_reset_message)
            .setPositiveButton(R.string.bili_about_yes) { _, _ ->
                AppSettingsStore.get(requireContext()).resetToDefaults()
                requireActivity().finish()
            }
            .setNegativeButton(R.string.bili_about_no, null)
            .show()
    }

    private fun openWeb(uri: Uri) {
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }.onFailure {
            showSourceToast(R.string.bili_about_update_cannot_find_web)
        }
    }

    private fun showSourceToast(messageRes: Int) {
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val JOIN_US_URL = "https://driftcue.com"
    }
}

private class BiliAboutDividerDecoration(context: Context) : RecyclerView.ItemDecoration() {
    private val paint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.Ga2)
    }
    private val height = context.resources
        .getDimension(R.dimen.bili_about_preference_divider_height)
        .coerceAtLeast(1f)
    private val left = context.resources
        .getDimension(R.dimen.bili_about_preference_divider_left_offset)

    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val adapter = parent.adapter ?: return
        for (index in 0 until parent.childCount) {
            val child = parent.getChildAt(index)
            val position = parent.getChildAdapterPosition(child)
            if (position == RecyclerView.NO_POSITION || position >= adapter.itemCount - 1) continue
            val top = child.bottom + child.translationY
            canvas.drawRect(left, top, parent.width.toFloat(), top + height, paint)
        }
    }
}

private data class UpdateManifest(
    val versionCode: Int,
    val versionName: String,
    val releaseNotes: String,
    val packageSize: Long,
    val downloadUri: Uri,
) {
    companion object {
        private const val URL = "https://driftcue.com/app-update.json"
        private const val APK_URL =
            "https://br-epic-clam-5a2fd709.supabase2.aidap-global.cn-beijing.volces.com/" +
                "storage/v1/object/public/apk/mengzhen-latest.apk"
        private val client = OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .build()

        fun fetch(context: Context): UpdateManifest =
            runCatching { fetchManifest() }.getOrElse { fetchFromPublishedApk(context) }

        private fun fetchManifest(): UpdateManifest {
            val request = Request.Builder()
                .url(URL)
                .cacheControl(CacheControl.FORCE_NETWORK)
                .header("Accept", "application/json")
                .build()
            client.newCall(request).execute().use { response ->
                check(response.isSuccessful)
                val json = JSONObject(response.body.string())
                check(json.getString("packageName") == BuildConfig.APPLICATION_ID)
                val versionCode = json.getInt("versionCode")
                val versionName = json.getString("versionName").trim()
                val downloadUri = Uri.parse(json.getString("downloadUrl"))
                check(versionCode > 0 && versionName.isNotEmpty())
                check(downloadUri.scheme == "https" && !downloadUri.host.isNullOrBlank())
                return UpdateManifest(
                    versionCode = versionCode,
                    versionName = versionName,
                    releaseNotes = json.optString("releaseNotes").trim(),
                    packageSize = json.optLong("packageSize", 0L).coerceAtLeast(0L),
                    downloadUri = downloadUri,
                )
            }
        }

        /**
         * Bootstrap path while older web deployments do not yet expose app-update.json.
         * It inspects the real published APK, so the result remains authoritative.
         */
        private fun fetchFromPublishedApk(context: Context): UpdateManifest {
            val temporaryApk = File.createTempFile("mengzhen-update-check-", ".apk", context.cacheDir)
            try {
                val request = Request.Builder()
                    .url(APK_URL)
                    .cacheControl(CacheControl.FORCE_NETWORK)
                    .build()
                val packageSize = client.newCall(request).execute().use { response ->
                    check(response.isSuccessful)
                    val body = response.body
                    temporaryApk.outputStream().use { output ->
                        body.byteStream().use { input -> input.copyTo(output) }
                    }
                    body.contentLength().coerceAtLeast(temporaryApk.length())
                }
                val packageInfo = checkNotNull(
                    context.packageManager.getPackageArchiveInfo(temporaryApk.absolutePath, 0),
                )
                check(packageInfo.packageName == BuildConfig.APPLICATION_ID)
                return UpdateManifest(
                    versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
                        .coerceAtMost(Int.MAX_VALUE.toLong())
                        .toInt(),
                    versionName = checkNotNull(packageInfo.versionName),
                    releaseNotes = "",
                    packageSize = packageSize,
                    downloadUri = Uri.parse(APK_URL),
                )
            } finally {
                temporaryApk.delete()
            }
        }
    }
}

private fun Context.hasInternetNetwork(): Boolean {
    val manager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return false
    val network = manager.activeNetwork ?: return false
    val capabilities = manager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
