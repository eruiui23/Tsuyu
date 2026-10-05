package com.example.tsuyu

import android.content.Context

object DispatchCoordinator {
    const val PREFS_NAME = "tsuyu_prefs"
    const val PREF_USE_FIREFOX = "pref_use_firefox"

    fun dispatch(context: Context, text: String) {
        // Always copy to clipboard (Primary Flow)
        ClipboardHelper.copyToClipboard(context, text)

        // Check if secondary flow (Firefox/Yomitan) is enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val useFirefox = prefs.getBoolean(PREF_USE_FIREFOX, false)

        if (useFirefox) {
            BrowserLauncher.openInFirefox(context, text)
        }
    }
}
