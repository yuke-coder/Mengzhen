package com.tencent.qqmusic.activity

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mengzhen.app.R
import com.mengzhen.app.data.api.ApiClient
import com.tencent.qqmusic.ui.customview.imagecrop.CroppableImageView
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ImageCropActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var cropView: CroppableImageView
    private lateinit var confirm: TextView
    private var sourcePath = ""
    private var saving = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.a9b)

        sourcePath = intent.getStringExtra(KEY_PATH).orEmpty()
        cropView = findViewById<CroppableImageView>(R.id.dmn).apply {
            val size = minOf(resources.displayMetrics.widthPixels, resources.displayMetrics.heightPixels)
            setCropDiaMeter(size - (((size * 0.5f * 45) / 375).toInt() * 2))
        }
        findViewById<TextView>(R.id.lbd).setText(R.string.vn)
        findViewById<ImageView>(R.id.a54).setOnClickListener(this)
        confirm = findViewById<TextView>(R.id.jdo).also { it.setOnClickListener(this) }

        runCatching { decodeSource(sourcePath) }
            .onSuccess { bitmap ->
                if (bitmap == null) finishWithError(getString(R.string.vm)) else cropView.setImageBitmap(bitmap)
            }
            .onFailure { finishWithError(getString(R.string.vm)) }
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.a54 -> finish()
            R.id.jdo -> saveAvatar()
        }
    }

    private fun saveAvatar() {
        if (saving) return
        saving = true
        confirm.isEnabled = false
        confirm.setText(R.string.vr)
        lifecycleScope.launch(Dispatchers.IO) {
            var output: File? = null
            val result = runCatching {
                output = File(cacheDir, "avatartemp_${System.currentTimeMillis()}.jpg")
                compress(cropView.g(-1), output!!, SOURCE_MAX_BYTES)
                ApiClient.get(this@ImageCropActivity).uploadAvatar(output!!, "image/jpeg")
            }
            output?.delete()
            withContext(Dispatchers.Main) {
                result.onSuccess { response ->
                    if (response.optBoolean("success", false)) {
                        setResult(
                            Activity.RESULT_OK,
                            Intent().putExtra(KEY_PATH, response.optString("avatar_url")),
                        )
                        finish()
                    } else {
                        finishWithError(response.optString("error", getString(R.string.vp)))
                    }
                }.onFailure { finishWithError(getString(R.string.vp)) }
            }
        }
    }

    private fun finishWithError(message: String) {
        setResult(Activity.RESULT_CANCELED, Intent().putExtra(KEY_ERROR_MSG, message))
        finish()
    }

    override fun onDestroy() {
        File(sourcePath).takeIf {
            it.parentFile == cacheDir && it.name.startsWith("avatar_")
        }?.delete()
        super.onDestroy()
    }

    private fun decodeSource(path: String): Bitmap? {
        if (path.isBlank()) return null
        return ImageDecoder.decodeBitmap(ImageDecoder.createSource(File(path))) { decoder, info, _ ->
            val width = info.size.width
            val height = info.size.height
            val longest = max(width, height)
            if (longest > SOURCE_MAX_SIDE) {
                decoder.setTargetSize(
                    width * SOURCE_MAX_SIDE / longest,
                    height * SOURCE_MAX_SIDE / longest,
                )
            }
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    }

    private fun compress(bitmap: Bitmap, file: File, maxBytes: Int) {
        FileOutputStream(file).use { output ->
            ByteArrayOutputStream().use { buffer ->
                if (bitmap.compress(Bitmap.CompressFormat.JPEG, 100, buffer)) {
                    val bytes = buffer.toByteArray()
                    if (bytes.size > maxBytes) {
                        bitmap.compress(
                            Bitmap.CompressFormat.JPEG,
                            maxBytes * 100 / bytes.size,
                            output,
                        )
                    } else {
                        output.write(bytes)
                        output.flush()
                    }
                }
            }
        }
        bitmap.recycle()
    }

    companion object {
        const val KEY_PATH = "KEY_PATH"
        const val KEY_ERROR_MSG = "KEY_ERROR_MSG"

        private const val SOURCE_MAX_SIDE = 2048
        private const val SOURCE_MAX_BYTES = 3_000_000
    }
}
