package com.dartdl.app.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * Robust way to find the underlying Activity from any Context.
 * Useful in Jetpack Compose where context might be a ContextWrapper or TintContextWrapper.
 */
fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
