package com.mengzhen.app.ui.feedback.toast

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Styling configuration for toasts
 */
public data class ToastStyle(
  val backgroundColor: Color = Color(0xFF2D2D2D),
  val textColor: Color = Color.White,
  val shape: Shape = RoundedCornerShape(12.dp),
  val elevation: Dp = 8.dp,
  val tonalElevation: Dp = 4.dp,
) {
  public companion object {
    public val Default: ToastStyle = ToastStyle()
  }
}

/**
 * Category-specific styling for toasts
 */
public data class ToastCategoryStyle(
  val backgroundColor: Color,
  val textColor: Color = Color.White,
  val icon: Any? = null,
)

/**
 * Complete toast theme configuration
 */
public data class ToastTheme(
  val default: ToastStyle = ToastStyle.Default,
  val success: ToastCategoryStyle? = null,
  val error: ToastCategoryStyle? = null,
  val warning: ToastCategoryStyle? = null,
  val info: ToastCategoryStyle? = null,
) {
  public companion object {
    public val Default: ToastTheme = ToastTheme()
  }
}
