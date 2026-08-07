package com.mengzhen.app.ui.feedback.toast

/**
 * A category for grouping and filtering toasts.
 *
 * Categories allow you to organize toasts and display them in different
 * [ToastHost] composables based on their type.
 */
public sealed class ToastCategory(public val name: String) {
  public data object General : ToastCategory("general")
  public data object Success : ToastCategory("success")
  public data object Warning : ToastCategory("warning")
  public data object Error : ToastCategory("error")
  public data class Custom(val customName: String) : ToastCategory(customName)
}
