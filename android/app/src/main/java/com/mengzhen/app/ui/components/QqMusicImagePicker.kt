package com.mengzhen.app.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.mengzhen.app.ui.feedback.AppNotice
import com.tencent.picker.activity.PictureSelectorActivity
import com.tencent.picker.d as PicturePickerConfiguration
import com.tencent.picker.j as PicturePickerRegistry
import com.tencent.qqmusic.business.timeline.post.MediaSelector
import com.tencent.qqmusic.business.timeline.post.m0 as QqMusicImageLoader
import com.tencent.qqmusic.business.timeline.post.n0 as QqMusicPickerLogger
import java.io.File

/**
 * The one image picker used by Mengzhen.
 *
 * It hosts QQ Music's bundled PictureSelectorActivity and original image loader/resources,
 * including its album switcher, selection grid, full-screen preview and selected preview.
 */
@Composable
internal fun rememberQqMusicImagePicker(
    maxSelection: Int,
    onImagesSelected: (List<Uri>) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val currentCallback = rememberUpdatedState(onImagesSelected)
    val contract = remember(maxSelection) {
        QqMusicImagePickerContract(maxSelection.coerceIn(1, MAX_PICKER_SELECTION))
    }
    val pickerLauncher = rememberLauncherForActivityResult(contract) { selected ->
        if (selected.isNotEmpty()) currentCallback.value(selected)
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        if (context.hasImageReadAccess()) {
            pickerLauncher.launch(Unit)
        } else {
            AppNotice.warning(context, "需要照片访问权限才能选择图片")
        }
    }

    return remember(context, pickerLauncher, permissionLauncher) {
        {
            if (context.hasImageReadAccess()) {
                pickerLauncher.launch(Unit)
            } else {
                permissionLauncher.launch(context.requiredImagePermissions())
            }
        }
    }
}

private class QqMusicImagePickerContract(
    private val maxSelection: Int,
) : ActivityResultContract<Unit, List<Uri>>() {
    override fun createIntent(context: Context, input: Unit): Intent {
        initializeQqMusicImagePicker()
        return Intent(context, PictureSelectorActivity::class.java).apply {
            putExtra(PictureSelectorActivity.CHOOSE_IMAGE, true)
            putExtra(PictureSelectorActivity.COUNT, maxSelection)
            putExtra(PictureSelectorActivity.USE_NUMBER_PICK_ICON, maxSelection > 1)
            putExtra(PictureSelectorActivity.FINISH_BUTTON_SHOW_SELECTED_COUNT, maxSelection > 1)
            putExtra(PictureSelectorActivity.NO_PREVIEW, false)
            putExtra(PictureSelectorActivity.SHOW_PREVIEW_THUMB_STRIP, false)
            putExtra(PictureSelectorActivity.SHOW_TAKE_PHOTO_OR_VIDEO_BUTTON, false)
            putExtra(PictureSelectorActivity.FILTER_GIF, false)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): List<Uri> {
        if (resultCode != Activity.RESULT_OK) return emptyList()
        return intent
            ?.getStringArrayExtra(PictureSelectorActivity.IMAGES)
            .orEmpty()
            .asSequence()
            .filter(String::isNotBlank)
            .distinct()
            .map(::pickerPathToUri)
            .toList()
    }
}

private fun initializeQqMusicImagePicker() {
    val configuration = PicturePickerConfiguration.b()
        .k(QqMusicImageLoader())
        .l(QqMusicPickerLogger())
        .m(MediaSelector.q())
        .o(PICKER_ACCENT_COLOR)
        .h()
    PicturePickerRegistry.b().c(configuration)
}

private fun pickerPathToUri(path: String): Uri = when {
    path.startsWith("content://") || path.startsWith("file://") -> Uri.parse(path)
    else -> Uri.fromFile(File(path))
}

private fun Context.hasImageReadAccess(): Boolean = when {
    applicationInfo.targetSdkVersion <= Build.VERSION_CODES.S_V2 -> {
        hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
        hasPermission(Manifest.permission.READ_MEDIA_IMAGES) ||
            hasPermission(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
    }
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
        hasPermission(Manifest.permission.READ_MEDIA_IMAGES)
    }
    else -> hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
}

private fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

private fun Context.requiredImagePermissions(): Array<String> = when {
    applicationInfo.targetSdkVersion <= Build.VERSION_CODES.S_V2 -> arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
    )
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
    )
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
    )
    else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
}

private const val MAX_PICKER_SELECTION = 9
private const val PICKER_ACCENT_COLOR = 0xFF83E6C8.toInt()
