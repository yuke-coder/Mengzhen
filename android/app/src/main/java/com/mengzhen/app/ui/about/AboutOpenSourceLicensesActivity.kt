package com.mengzhen.app.ui.about

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.google.android.material.appbar.MaterialToolbar
import com.mengzhen.app.R
import com.mengzhen.app.ui.theme.ThemeMode
import com.mengzhen.app.ui.theme.ThemeModeStore

/** Source-adapted from Bilibili 9.7.0 LicenseActivity. */
class AboutOpenSourceLicensesActivity : AppCompatActivity() {
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
        setContentView(R.layout.bili_about_activity_with_toolbar_source)
        bindToolbar()

        val padding = (16 * resources.displayMetrics.density).toInt()
        val content = findViewById<FrameLayout>(R.id.bili_about_document_content).apply {
            setPadding(padding, padding, padding, padding)
        }
        val text = resources.openRawResource(R.raw.mengzhen_open_source_licenses)
            .bufferedReader()
            .use { it.readText() }
        val textView = TextView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            setTextColor(ContextCompat.getColor(context, R.color.Ga9))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextIsSelectable(true)
            this.text = text
        }
        content.addView(
            ScrollView(this).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                addView(textView)
            },
        )
    }

    private fun bindToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.bili_about_document_toolbar)
        toolbar.title = getString(R.string.bili_about_open_source_licenses)
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
}
