package com.mengzhen.app.ui.feedback.toast

import androidx.compose.ui.Alignment

/**
 * Toast viewer alignment options.
 */
public enum class ToastAlignment {
  Top,
  Bottom,
  ;

  public val isTop: Boolean get() = this == Top
  public val isBottom: Boolean get() = this == Bottom

  public fun toComposeAlignment(): Alignment = when (this) {
    Top -> Alignment.TopCenter
    Bottom -> Alignment.BottomCenter
  }
}
