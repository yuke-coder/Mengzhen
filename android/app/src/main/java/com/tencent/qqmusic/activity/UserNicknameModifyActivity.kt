package com.tencent.qqmusic.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mengzhen.app.R
import com.mengzhen.app.data.api.ApiClient
import com.mengzhen.app.data.model.parseProfile
import com.mengzhen.app.data.store.TaskStore
import com.tencent.qqmusic.fragment.profile.d
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserNicknameModifyActivity : AppCompatActivity(), View.OnClickListener, TextWatcher {
    private lateinit var editText: EditText
    private lateinit var deleteBtn: ImageView
    private lateinit var countText: TextView
    private lateinit var errorText: TextView
    private lateinit var saveBtn: TextView
    private var maxLength = SOURCE_NICKNAME_LIMIT
    private var saving = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ctq)

        findViewById<TextView>(R.id.lbd).text =
            intent.getStringExtra(KEY_TITLE) ?: getString(R.string.qq_profile_set_nickname)
        findViewById<ImageView>(R.id.a54).setOnClickListener(this)
        countText = findViewById(R.id.bb3)
        errorText = findViewById(R.id.c95)
        deleteBtn = findViewById<ImageView>(R.id.blu).also { it.setOnClickListener(this) }
        saveBtn = findViewById<TextView>(R.id.jdo).also { it.setOnClickListener(this) }
        maxLength = intent.getIntExtra(KEY_MAX_LENGTH, SOURCE_NICKNAME_LIMIT)
        editText = findViewById<EditText>(R.id.c1n).apply {
            hint = intent.getStringExtra(KEY_HINT) ?: getString(R.string.bum)
            isSingleLine = !intent.getBooleanExtra(KEY_MULTILINE, false)
            if (!isSingleLine) maxLines = 6
            addTextChangedListener(this@UserNicknameModifyActivity)
            setText(intent.getStringExtra(KEY_VALUE).orEmpty())
            setSelection(text.length)
            requestFocus()
        }
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        editText.post {
            (getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager)
                ?.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.a54 -> finish()
            R.id.blu -> editText.setText("")
            R.id.jdo -> save()
        }
    }

    override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) = Unit
    override fun afterTextChanged(text: Editable?) = Unit

    override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
        val length = if (text.isNullOrEmpty()) 0 else d.a.c(text)
        val nickname = intent.getStringExtra(KEY_FIELD).orEmpty() == FIELD_NICKNAME
        val validDigits = !nickname || (text?.count(Char::isDigit) ?: 0) <= SOURCE_DIGIT_LIMIT
        val valid = length in 1..maxLength && validDigits

        errorText.visibility = if (valid || length == 0) View.INVISIBLE else View.VISIBLE
        countText.visibility = if (valid) View.VISIBLE else View.INVISIBLE
        deleteBtn.visibility = if (length > 0) View.VISIBLE else View.INVISIBLE
        if (!valid && length > 0) {
            errorText.text = if (length > maxLength) {
                getString(R.string.qq_profile_text_too_long)
            } else {
                getString(R.string.qq_profile_nickname_too_many_digits)
            }
        }
        if (valid) countText.text = "$length / $maxLength"
        setSaveEnabled(valid && !saving)
    }

    private fun setSaveEnabled(enabled: Boolean) {
        saveBtn.isEnabled = enabled
        saveBtn.setBackgroundResource(
            if (enabled) R.drawable.btn_big_confirm else R.drawable.btn_big_confirm_unenabled,
        )
    }

    private fun save() {
        if (saving || !saveBtn.isEnabled) return
        saving = true
        setSaveEnabled(false)
        val field = intent.getStringExtra(KEY_FIELD).orEmpty()
        val value = editText.text.toString()
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching {
                val api = ApiClient.get(this@UserNicknameModifyActivity)
                when (field) {
                    FIELD_USERNAME -> api.updateProfile(username = value)
                    FIELD_LOCATION -> api.updateProfile(location = value)
                    FIELD_SIGNATURE -> api.updateProfile(signature = value)
                    FIELD_BIO -> api.updateProfile(bio = value)
                    else -> api.updateProfile(nickname = value)
                }
            }.onSuccess { response ->
                withContext(Dispatchers.Main) {
                    if (response.optBoolean("success", false)) {
                        parseProfile(response)?.let {
                            TaskStore.get(this@UserNicknameModifyActivity)
                                .saveUserSession("cookie_session", it)
                        }
                        setResult(
                            Activity.RESULT_OK,
                            Intent()
                                .putExtra(KEY_NICKNAME, value)
                                .putExtra(KEY_FIELD, field)
                                .putExtra(KEY_VALUE, value),
                        )
                        finish()
                    } else {
                        errorText.text = response.optString("error", getString(R.string.qq_profile_save_failed))
                        errorText.visibility = View.VISIBLE
                        countText.visibility = View.INVISIBLE
                        saving = false
                        setSaveEnabled(true)
                    }
                }
            }.onFailure {
                withContext(Dispatchers.Main) {
                    errorText.text = getString(R.string.qq_profile_save_failed)
                    errorText.visibility = View.VISIBLE
                    countText.visibility = View.INVISIBLE
                    saving = false
                    setSaveEnabled(true)
                }
            }
        }
    }

    companion object {
        const val KEY_NICKNAME = "KEY_NICKNAME"
        const val KEY_ERROR_CODE = "KEY_ERROR_CODE"
        const val KEY_ERROR_MSG = "KEY_ERROR_MSG"
        const val KEY_ERROR_MSG_URL = "KEY_ERROR_MSG_URL"
        const val KEY_FIELD = "KEY_FIELD"
        const val KEY_VALUE = "KEY_VALUE"
        const val KEY_TITLE = "KEY_TITLE"
        const val KEY_HINT = "KEY_HINT"
        const val KEY_MAX_LENGTH = "KEY_MAX_LENGTH"
        const val KEY_MULTILINE = "KEY_MULTILINE"

        const val FIELD_NICKNAME = "nickname"
        const val FIELD_USERNAME = "username"
        const val FIELD_LOCATION = "location"
        const val FIELD_SIGNATURE = "signature"
        const val FIELD_BIO = "bio"

        private const val SOURCE_NICKNAME_LIMIT = 15
        private const val SOURCE_DIGIT_LIMIT = 6
    }
}
