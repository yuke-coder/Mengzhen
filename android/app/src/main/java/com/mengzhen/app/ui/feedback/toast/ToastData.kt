package com.mengzhen.app.ui.feedback.toast

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Interface to represent data for a toast.
 * Similar to SnackbarData in Jetpack Compose.
 */
public interface ToastData {
  public val id: String
  public val message: String
  public val title: String?
  public val icon: Any?
  public val category: ToastCategory
  public val height: Dp
  public fun dismiss()
}

/**
 * Internal implementation of ToastData.
 */
internal class ToastDataImpl(
  override val id: String,
  override val message: String,
  override val title: String?,
  override val icon: Any?,
  override val category: ToastCategory,
  override val height: Dp = 64.dp,
  private val onDismiss: () -> Unit,
) : ToastData {
  override fun dismiss() = onDismiss()
}
