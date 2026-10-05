package com.example.tsuyu

import android.content.Context

object DispatchCoordinator {
    const val PREFS_NAME = "tsuyu_prefs"
    const val PREF_USE_FIREFOX = "pref_use_firefox"

    fun dispatch(context: Context, text: String) {
        // Task 4.3: Do nothing if extracted text is blank
        if (text.isBlank()) return

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val useFirefox = prefs.getBoolean(PREF_USE_FIREFOX, false)

        if (useFirefox) {
            BrowserLauncher.openInFirefox(context, text)
        } else {
            ClipboardHelper.copyToClipboard(context, text)
        }
    }
}
